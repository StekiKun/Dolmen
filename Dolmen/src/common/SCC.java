package common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes the strongly-connected components of some graph
 * described by an implementation of {@link Graph}. The components
 * are computed via {@link #of(Graph)} using Tarjan's 1972 algorithm:
 * <pre>
 *   Tarjan, R. E. (1972), "Depth-first search and linear graph algorithms", 
 *   SIAM Journal on Computing, 1 (2): 146–160
 * </pre>
 * Components can then be investigated {@linkplain #scc(Object) individually},
 * {@linkplain #iter all at once}, or simply by using the {@link #representative(Object)}
 * function which maps every node in the graph to a unique node representing
 * its strongly-connected component.
 * 
 * @author Stéphane Lescuyer
 *
 * @param <Node> the type of nodes in the graph
 */
public final class SCC<Node> {

	/**
	 * Interface describing a directed graph whose vertices can
	 * be indexed by integers.
	 * <p>
	 * It is used to describe the input to the {@link SCC} class
	 * which computes the strongly-connected components of the graph.
	 * The {@link #index(Object)} method should be fast.
	 * 
	 * @author Stéphane Lescuyer
	 *
	 * @param <Node> the type of nodes in the graph
	 */
	public static interface Graph<Node> {
		/**
		 * @return the number of nodes in the graph
		 */
		int size();
		
		/**
		 * Each node must have a unique index, i.e. for
		 * every index {@code 0 <= i < size()}, there must
		 * be some node {@code n} such that {@code index(n) = i}.
		 * 
		 * @param n
		 * @return the index of node {@code n}, which must be
		 * 	between 0 and {@code size() - 1}
		 * @throws NoSuchElementException if the node is not in the graph
		 */
		int index(Node n);
		
		/**
		 * Applies the function {@code f} to all the successors
		 * of the node {@code n}, in no particular order
		 * 
		 * @param n
		 * @param f
		 * @throws NoSuchElementException if the node is not in the graph
		 */
		void successors(Node n, Consumer<Node> f);
		
		/**
		 * Applies the function {@code f} to all the nodes in
		 * the graph, in no particular order
		 * 
		 * @param f
		 */
		void iter(Consumer<Node> f);
	}
	
	/**
	 * Container class representing the state associated 
	 * with each node in the graph during the SCC computation
	 * 
	 * @author Stéphane Lescuyer
	 *
	 * @param <Node> the type of nodes whose status is described
	 */
	private static final class Status<Node> {
		/** 
		 * Whether the node is already in the algorithm's stack or not 
		 */
		boolean inStack;

		/**
		 * The node's order number, i.e. the order in which it has
		 * been discovered. Order numbers start at 0 and grow upwards.
		 * A negative order number means the node has not been visited yet.
		 */
		int order;

		/**
		 * The lowest order number associated to a node detected
		 * within this node's SCC
		 */
		int low;

		/**
		 * The representative of this node's SCC, or the node itself
		 * if its SCC has not been discovered yet
		 */
		Node repr;

		/**
		 * The list of nodes in this node's SCC, if this node is
		 * the representative of a discovered SCC, or {@code null}
		 * otherwise
		 */
		@Nullable List<Node> scc;
		
		Status(Node n) {
			this.inStack = false;
			this.order = this.low = -1;
			this.repr = n;
			this.scc = null;
		}
	}
	
	// The graph whose SCCs are being computed
	private final Graph<Node> graph;
	// The table of all node statuses, in order of node index
	private final Status<Node>[] statuses;
	
	// The next available order number, must be nonnegative
	private int counter;
	// The stack of nodes visited but not yet sorted out
	private final Stack<Status<Node>> stack;
	
	// The list of representatives of discovered SCCs
	private final List<Node> representatives;
	
	@SuppressWarnings("unchecked")
	private SCC(Graph<Node> graph) {
		this.graph = graph;		
		this.counter = 0;
		this.stack = new Stack<>();
		this.statuses = new Status[graph.size()];
		graph.iter(node -> 
			statuses[graph.index(node)] = new Status<>(node)
		);
		this.representatives = new ArrayList<>(graph.size());
	}
	
	/**
	 * Computes and returns an instance of {@link SCC} which
	 * describes the strongly connected components of the
	 * given {@code graph}.
	 * 
	 * @see #count()
	 * @see #scc(Object)
	 * @see #iter(Consumer)
	 * 
	 * @param graph
	 * @return the strongly connected components of {@code graph}
	 */
	public static <Node> SCC<Node> of(Graph<Node> graph) {
		SCC<Node> scc = new SCC<>(graph);
		scc.compute();
		return scc;
	}
	
	/**
	 * Start visiting the node associated to {@code st}.
	 * This allocates an order number for this node
	 * and adds it to the {@link #stack}.
	 * 
	 * @param st
	 */
	private void start(Status<Node> st) {
		st.inStack = true;
		st.order = st.low = counter;
		++counter;
		stack.add(st);		
	}

	private void compute() {
		graph.iter(this::visit);
	}
	
	private void visit(Node node) {
		Status<Node> status = statuses[graph.index(node)];
		// If the node has already been discovered, nothing to do
		if (status.order >= 0) return;
		// Otherwise, we mark it and start a depth-first traversal 
		// of its connected component
		start(status);
		dfs(node);
	}
	
	private void dfs(Node node) {
		final Status<Node> status = statuses[graph.index(node)];
		
		graph.successors(node, succ -> {
			Status<Node> stsucc = statuses[graph.index(succ)];
			if (stsucc.order < 0) {
				// This successor has not been visited yet. Mark
				// it and proceed onwards, depth-first style.
				start(stsucc);
				dfs(succ);  // <- we will not deal with gigantic
							//  graphs here, it's very unlikely that
							//  a chain in grammar dependencies would
							//  be as long as to lead to a stack overflow
				status.low = Math.min(status.low, stsucc.low);
			}
			else {
				// This successor has already been visited.
				// If it is still on the stack, its SCC has not been 
				// discovered so it must be the same as {@code node}'s.
				// It may become the new lowest order number in the SCC.
				if (stsucc.inStack && stsucc.low < status.low)
					status.low = Math.min(status.low, stsucc.order);
			}
		});
		
		if (status.order == status.low) {
			List<Node> scc = new LinkedList<>();
			// The whole SCC of {@code node} is now on the stack,
			// and node was the first element that was visited, so
			// the SCC is formed by all the nodes we can pop
			// from the stack until we reach (and including)
			// {@code node} itself.
			Status<Node> popped;
			do {
				popped = stack.pop();
				popped.inStack = false;
				scc.add(popped.repr);
				popped.repr = node;
			} while (popped != status);
			status.scc = scc;
			representatives.add(node);
		}		
	}
	
	/**
	 * @return the number of different strongly-connected components
	 */
	public int count() {
		return representatives.size();
	}
	
	/**
	 * <i>The node must belong to the graph for which this {@link SCC}
	 * 	was computed.</i>
	 * 
	 * @param n
	 * @return the strongly-connected component of the node {@code n},
	 * 	where nodes are presented in no particular order
	 */
	public List<Node> scc(Node n) {
		Node repr = statuses[graph.index(n)].repr;
		@Nullable List<Node> scc = statuses[graph.index(repr)].scc;
		if (scc == null)
			throw new IllegalStateException("SCC for representative node " + repr + " not set");
		return scc;
	}
	
	/**
	 * <i>The node must belong to the graph for which this {@link SCC}
	 * 	was computed.</i>
	 *
	 * Representatives are such that two nodes {@code n} and {@code m}
	 * are in the same strongly-connected component if and only if
	 * {@code representative(n) == representative(m)}. 
	 * 
	 * @param n
	 * @return the <i>representative</i> of {@code n}'s 
	 * 	strongly-connected component
	 */
	public Node representative(Node n) {
		return statuses[graph.index(n)].repr;
	}
	
	/**
	 * Iterates the given function {@code f} on all the
	 * strongly-connected components, in reverse topological order
	 * (i.e. a component is always visited before any of its
	 *  successors). 
	 * The relative order of nodes in each component is unspecified. 
	 * 
	 * @param f
	 */
	public void iter(Consumer<List<Node>> f) {
		for (Node repr : representatives) {
			Status<Node> st = statuses[graph.index(repr)];
			if (st.repr != repr)
				throw new IllegalStateException("Representative node is not a representative");
			@Nullable List<Node> scc = st.scc;
			if (scc == null)
				throw new IllegalStateException("SCC for representative node " + repr + " not set");
			f.accept(scc);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Strongly-connected components: (").append(count()).append(")\n");
		iter(scc -> {
			buf.append(" - ").append(scc).append("\n");
		});
		return buf.toString();
	}
}