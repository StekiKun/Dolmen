package test.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Nullable;

import common.Generator;
import common.SCC;
import common.SCC.Graph;
import test.TestRegistry;
import test.TestUnit;

/**
 * Testing unit which generates graphs whose nodes are simple
 * integers, computes their strongly-connected components
 * using {@link SCC}, and checks the results of the 
 * computed results.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestSCC
	implements TestUnit<Graph<Integer>, SCC<Integer>> {

	private final int size;
	private final float edgeChance;
	
	/**
	 * Creates a testing unit which uses graphs with {@code size} 
	 * nodes and where each pair of nodes has a probability 
	 * {@code edgeChance} of being connected
	 * 
	 * @param size
	 * @param edgeChance
	 */
	public TestSCC(int size, float edgeChance) {
		this.size = size;
		this.edgeChance = edgeChance;
	}
	
	@Override
	public String name() {
		return String.format(
			"Testing the computation of strongly-connected components (size=%d, edgeChance=%.2f)",
			size, edgeChance);
	}

	@Override
	public Generator<Graph<Integer>> generator() {
		final Random rng = new Random();
		
		return new Generator<Graph<Integer>>() {
			@Override
			public String name() {
				return "Graph generator";
			}

			@Override
			public Graph<Integer> generate() {
				// The graph's edges will be represented by a bitset
				// associated to every node 0 to size - 1
				final BitSet succs[] = new BitSet[size];
				for (int i = 0; i < size; ++i) {
					BitSet succi = new BitSet(size);
					for (int j = 0; j < size; ++j) {
						if (rng.nextFloat() < edgeChance)
							succi.set(j);
					}
					succs[i] = succi;
				}
				final String display = Arrays.toString(succs);
				
				// Return the graph represented by the successors array
				return new Graph<Integer>() {
					@Override
					public int size() {
						return size;
					}

					@Override
					public int index(Integer n) {
						return n;
					}

					@Override
					public void successors(Integer n, Consumer<Integer> f) {
						int idx = index(n);
						if (idx < 0 || idx >= size)
							throw new NoSuchElementException();
						BitSet bs = succs[idx];
						for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
							f.accept(i);
						}
					}

					@Override
					public void iter(Consumer<Integer> f) {
						for (int i = 0; i < size; ++i)
							f.accept(i);
					}
					
					@Override
					public String toString() {
						return display;
					}
				};
			}
		};
	}

	@Override
	public SCC<Integer> apply(Graph<Integer> input) {
		return SCC.of(input);
	}

	@Override
	public @Nullable String check(Graph<Integer> input, SCC<Integer> output) {
		// We will visit every strongly-connected component in turn, checking
		// that:
		//  - all the nodes in an SCC have the same representative, itself
		//	  in that SCC
		//  - all nodes belong to some SCC (the total size of SCCs is the size
		//    of the graph)
		//  - every node in an SCC is connected to every other node in the SCC
		BitSet visited = new BitSet(size);
		List<List<Integer>> sccs = new ArrayList<>(output.count());
		output.iter(sccs::add);
		for (List<Integer> scc : sccs) {
			if (scc.isEmpty()) return "Generated an empty SCC";
			
			int repr = output.representative(scc.get(0));
			boolean found = false;
			for (int k : scc) {
				found |= k == repr;
				visited.set(k);
				int rk = output.representative(k);
				if (repr != rk)
					return "Nodes " + scc.get(0) + " and " + k + 
						" belong to the same SCC " + scc + " but do not " +
						" have the same representatives: " + repr + " != " + rk;
				List<Integer> rscc = output.scc(k);
				if (rscc != scc)
					return "Node " + k + " belongs to SCC " + scc + " but is " +
						"associated to SCC " + rscc;
				// Check connectedness
				BitSet cc = connectedComponent(input, k);
				for (int j : scc) {
					if (!cc.get(j))
						return "Nodes " + k + " and " + j + " are in the same SCC " +
							scc + " but " + j + " is not reachable from " + k;
				}
			}
			
			if (!found)
				return "SCC " + scc + " has representative " + repr + 
					" which does not belong to the SCC";
		}
		
		// Check that the SCCs form a partition
		if (visited.cardinality() != size) {
			StringBuilder buf = new StringBuilder();
			buf.append("The computed SCCs do not cover all nodes: ");
			for (int i = visited.nextClearBit(0); i >= 0; i = visited.nextClearBit(i+1))
				buf.append(i).append(" ");
			return buf.toString();
		}
		
		// Everything ok
		return null;
	}
	
	private BitSet connectedComponent(Graph<Integer> graph, int n) {
		BitSet visited = new BitSet(graph.size());
		BitSet todo = new BitSet(graph.size());
		todo.set(n);
		while (!todo.isEmpty()) {
			int next = todo.nextSetBit(0);
			todo.clear(next);
			if (visited.get(next)) continue;
			visited.set(next);
			graph.successors(next, succ -> {
				if (!visited.get(succ)) todo.set(succ);
			});
		}
		return visited;
	}

	/**
	 * Entry point for performing {@link TestSCC} tests
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TestRegistry registry =
				TestRegistry.create()
					.add(new TestSCC(5, 0.5f), 1000)
					.add(new TestSCC(10, 0.5f), 50000)
					.add(new TestSCC(20, .25f), 50000)
					.done();
		registry.run(Mode.BATCH);
	}
}
