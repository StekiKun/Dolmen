package automaton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.annotation.Nullable;

import automaton.DFA.MemAction;
import automaton.DFA.MemMap;
import common.Maps;
import common.Sets;
import tagged.TRegular.TagInfo;

/**
 * An instance of this class can be used to determinize
 * a {@link NFA non-deterministic finite automaton} and
 * construct an equivalent {@link DFA deterministic
 * finite automaton}.
 * 
 * @author Stéphane Lescuyer
 */
@SuppressWarnings("unused")
public class Determinize {

	/** Used to index DFA states */
	private static final class StateNum {
		private final DFA.State state;
		private final int num;
		
		StateNum(DFA.State state, int num) {
			this.state = state;
			this.num = num;
		}
	}
	
	/** 
	 * The map from DFA state keys to a state number
	 */
	private Map<DFA.Key, Integer> stateMap;
	private Stack<StateNum> todo;	// TODO: Object?
	
	/** The next unused state number */
	private int nextStateNum;
	/** The next unused memory cell index */
	private int nextMemCell;
	/** Whether there are some temporaries pending */
	private boolean tempPending;
	
	/** A map associating memory cells used per tag */
	private Map<TagInfo, Set<Integer>> tagCells;
	/** The table of already built DFA states */
	private ArrayList<DFA.State> stateTable;
	
	private Determinize() {
		this.stateMap = new HashMap<>();
		this.todo = new Stack<>();
		this.nextStateNum = 0;
		this.nextMemCell = 0;
		this.tempPending = false;
		this.tagCells = new HashMap<>();
		this.stateTable = new ArrayList<>();
	}

	private void reset() {
		todo.clear();
		nextStateNum = 0;
		stateTable.trimToSize();
	}
	
	private void resetPartial(int ntags) {
		nextMemCell = ntags;
		tagCells.clear();
		tempPending = false;
		stateMap = new HashMap<>();
	}
	
	/** 
	 * @return a free memory cell, suitable for
	 * 	use as a temporary in a memory actions sequence
	 */
	private int allocTemp() {
		tempPending = true;
		return nextMemCell;
	}
	
	/**
	 * @return a memory cell for the specified {@code tag}
	 * but oustide of the ones given in {@code used}. If
	 * possible, this will reuse one of the cells already allocated
	 * for {@code tag}, but otherwise will allocate a new one.
	 * 
	 * @param used
	 * @param tag
	 */
	private int allocCell(Set<Integer> used, TagInfo tag) {
		Set<Integer> available =
			tagCells.getOrDefault(tag, Sets.empty());
		Set<Integer> free = Sets.diff(available, used);
		if (!free.isEmpty())
			return free.iterator().next();
		tempPending = false;
		int res = nextMemCell++;
		// TODO add bound to number of memory cells?
		available.add(res);
		return res;
	}
	
	
	/** Already allocated addresses have non-negative indices */
	private static boolean isOld(int addr) {
		return addr >= 0;
	}
	/** Fresh addresses, which must still be allocated, have negative indices */
	private static boolean isNew(int addr) {
		return addr < 0;
	}
	
	/**
	 * Extends {@code acc} with all non-fresh addresses specifies
	 * in the location map {@code m}
	 * 
	 * @param m
	 * @param acc
	 */
	private static <K> 
	void oldInMap(Map<K, Integer> m, Set<Integer> acc) {
		for (int addr : m.values())
			if (isOld(addr)) acc.add(addr);
	}
	
	/**
	 * Go through a location map with potentially fresh addresses
	 * and allocates them, recording the memory actions corresponding
	 * to these new addresses (namely {@link MemAction#set(int) SET(n)}
	 * for all newly allocated addresses {@code n}).
	 * 
	 * @param used	memory cells which cannot be used in allocation
	 * @param m		
	 * @param mvs	a set of memory actions to extend
	 * @return the new location map with no more fresh addresses,
	 * 	and extends {@code mvs} with the corresponding memory actions
	 */
	private Map<TagInfo, Integer> allocMap(
		Set<Integer> used, Map<TagInfo, Integer> m, Set<Integer> mvs) {
		// TODO: is it OK to modify mvs in place?
		// TODO: can I do the same with m instead of rebuilding the tagmap?
		Map<TagInfo, Integer> allocated = new HashMap<>(m.size());
		for (Map.Entry<TagInfo, Integer> entry : m.entrySet()) {
			final TagInfo tag = entry.getKey();
			int addr = entry.getValue();
			// If the address is new, allocate it, and extend movs with it
			if (isNew(addr)) {
				addr = allocCell(used, tag);
				mvs.add(addr);
			}
			allocated.put(tag, addr);
		}
		return allocated;
	}
	
	/**
	 * Takes the description of a DFA state {@code s} whose location maps
	 * (both for the finisher and the regular states) may contain fresh
	 * unallocated addresses and ensure these are allocated to suitable
	 * (in the sense of not yet used in these location maps) memory
	 * cells.
	 * 
	 * @param s
	 * @param memActions
	 * @return the finalized DFA state with all fresh addresses allocated,
	 * 	and extended {@code memActions} with the corresponding memory actions
	 */
	private DFA.State createNewState(DFA.State s, 
			ArrayList<MemAction> memActions) {
		Map<TagInfo, Integer> fLocs = s.getFinalLocs();
		// Compute used memory cells, by both final locs and other locs
		Set<Integer> used = new HashSet<>();
		oldInMap(fLocs, used);
		for (MemMap mmap : s.others.values())
			oldInMap(mmap.locs, used);
		
		// Allocate new addresses in s, collecting moves on the way
		Set<Integer> moves = new HashSet<>();
		Map<TagInfo, Integer> newFLocs = allocMap(used, fLocs, moves);
		Map<Integer, MemMap> newOthers = new HashMap<>();
		s.others.forEach((k, mmap) -> {
			newOthers.put(k,
				new MemMap(mmap.id, allocMap(used, mmap.locs, moves)));
		});
		
		// Create updated state
		final DFA.State news;
		if (s.isFinal())
			news = new DFA.State(s.finalAction,
						new MemMap(0, newFLocs), newOthers);
		else
			news = new DFA.State(newOthers);
		
		// Turn all the moves into SET(n) memory actions
		for (int dst : moves)
			memActions.add(MemAction.set(dst));
		return news;
	}
	
	/**
	 * Used to generate fresh (i.e. negative) memory cells 
	 * for tags during the construction of a DFA state
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class AddressGen {
		private int count = -1;
		private Map<TagInfo, Integer> env = new HashMap<>();
		
		/**
		 * @param tag
		 * @return a fresh memory cell associated to the 
		 * 	given tag
		 */
		int allocNewAddr(TagInfo tag) {
			@Nullable Integer a = Maps.get(env, tag);
			if (a != null) return a;
			env.put(tag, count);
			return count--;
		}
	}
	
	/**
	 * @param tags
	 * @param gen
	 * @return a new location map that provides fresh
	 * 	memory addresses for every tag in {@code tags}.
	 * 	It uses {@code gen} as the address generator, so
	 * 	that location maps created with the same generator
	 * 	instance are guaranteed to not use the same fresh
	 * 	address for different tags. 
	 */
	private Map<TagInfo, Integer>
		createMemMap(Set<TagInfo> tags, AddressGen gen) {
		Map<TagInfo, Integer> locs = Maps.create();
		for (TagInfo tag : tags)
			locs.put(tag, gen.allocNewAddr(tag));
		return locs;
	}
	
	/**
	 * @param possible
	 * @return the initial state for the DFA corresponding to
	 * 	an NFA whose possible initial states (including ε-transitions)
	 *  is {@code possible}
	 * @see NFA#firstPos(tagged.TRegular)
	 */
	private DFA.State createInitState(Set<NFA.Transition> possible) {
		final AddressGen gen = new AddressGen();
		int finalAction = DFA.NO_ACTION;
		@Nullable MemMap finisher = null;
		Map<Integer, MemMap> others = Maps.create();
		for (NFA.Transition trans : possible) {
			final int n = trans.event.n;
			Map<TagInfo, Integer> locs = createMemMap(trans.tags, gen);
			switch (trans.event.kind) {
			case ON_CHARS: {
				if (others.containsKey(n))
					throw new IllegalStateException();
				others.put(n, new MemMap(0, locs));
				break;
			}
			case TO_ACTION: {
				// Only update the final action
				// if this one has higher priority
				if (n < finalAction) {
					finalAction = n;
					finisher = new MemMap(0, locs);
				}
				break;
			}
			}
		}
		if (finisher == null)
			return new DFA.State(others);
		return new DFA.State(finalAction, finisher, others);
	}

	/**
	 * Partitions the range of the memory actions {@code memActions}
	 * between {@code from} and {@code to} (inclusive), in-place,
	 * so that all memory actions which do not read one of the
	 * {@code modified} memory cells come first, and the others
	 * come last.
	 * 
	 * @param from
	 * @param memActions
	 * @param to
	 * @param modified
	 * @return the index of the first memory action that depends
	 * 	one of the modified cells, or {@code to + 1} if there are
	 *  none
	 */
	private int partitionMoves(
		int from, ArrayList<MemAction> memActions, int to,
		Set<Integer> modified) {
		int cfrom = from;
		int cto = to;
		// cfrom --> ... <-- cto
		while (cfrom < cto) {
			// traverse actions which do not read the modified
			// cfrom --> ...
			while (cfrom < cto &&
				!modified.contains(memActions.get(cfrom)))
				++cfrom;
			// traverse actions which do read the modified
			// ... <-- cto
			while (cfrom < cto &&
				modified.contains(memActions.get(cto)))
				--cto;
			// if we're not done, let's swap the bad guys
			if (cfrom >= cto) break;
			MemAction mfrom = memActions.get(cfrom);
			MemAction mto = memActions.get(cto);
			memActions.set(cfrom, mto);
			memActions.set(cto, mfrom);
			++cfrom; --cto;
		}
		return cfrom;
	}
	
	/**
	 * Auxiliary function used by {@link #sortMoves(ArrayList)}
	 * to sort the range of elements from {@code from} to {@code to}
	 * (inclusive)
	 * 
	 * @param from
	 * @param memActions
	 * @param to
	 */
	private void sortMovesAux(
		int from, ArrayList<MemAction> memActions, int to) {
		// Compute all memory cells modified by the actions in the slice
		Set<Integer> modified = Sets.create();
		for (int i = from; i <= to; ++i)
			modified.add(memActions.get(i).getDest());
		// Actions which use one of these modified cells as a source
		// must be performed before the others
		int pivot = partitionMoves(from, memActions, to, modified);
		if (pivot == from) {
			// We haven't made progress, we need to add a temporary
			// (does this happen ?? probably in pathological cases..
			//  would it be easier to just not merge states in this case?)
			MemAction.Copy copy = (MemAction.Copy) memActions.get(pivot);
			int tmp = allocTemp();
			// We save copy.dst value in tmp, and change every
			// furhter occurrence to copy.dst to tmp
			MemAction.Copy sav = MemAction.copy(tmp, copy.dst);
			for (int i = from; i <= to; ++i) {
				MemAction mi = memActions.get(i);
				if (mi.getSrc() == copy.dst)
					memActions.set(i, MemAction.copy(tmp, mi.getDest()));
			}
			// Insert sav before the slice, and sort the slice recursively
			memActions.add(pivot, sav);
		}
		else {
			// Sort the remainder of the slice recursively
			sortMovesAux(pivot, memActions, to);
		}
	}
	
	/**
	 * Sorts the various move actions in the given array (in place)
	 * in a "topological" way, so that actions which read some memory
	 * cell are applied <b>before</b> actions which write the same
	 * memory cell. Temporaries may be added in order to break
	 * dependency cycles.
	 *
	 * @param memActions
	 */
	void sortMoves(ArrayList<MemAction> memActions) {
		sortMovesAux(0, memActions, memActions.size() - 1);
	}
	
}