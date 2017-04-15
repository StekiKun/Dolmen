package automaton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import automaton.DFA.GotoAction;
import automaton.DFA.MemAction;
import automaton.DFA.MemMap;
import automaton.DFA.Remember;
import automaton.DFA.Shift;
import automaton.DFA.TEquiv;
import automaton.DFA.TagAction;
import automaton.DFA.TransActions;
import common.CSet;
import common.Maps;
import common.Sets;
import syntax.Lexer;
import tagged.Encoder;
import tagged.Optimiser.IdentInfo;
import tagged.Optimiser.TagAddr;
import tagged.TLexer;
import tagged.TLexerEntry;
import tagged.TLexerEntry.Finisher;
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
	/**
	 * The stack of states whose outgoing transitions
	 * must still be built
	 */
	private Stack<StateNum> todo;
	
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
				new MemMap(mmap.priority, 
					allocMap(used, mmap.locs, moves)));
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

	private void moveTo(Set<TEquiv> memKey,
		DFA.State src, DFA.State tgt, ArrayList<MemAction> moves) {
		for (TEquiv teq : memKey) {
			final TagInfo tag = teq.tag;
			teq.equiv.forEach(s -> {
				assert (!s.isEmpty());
				NFA.Event t = s.iterator().next();
				int asrc = src.getLocsFor(t).get(tag);
				int atgt = tgt.getLocsFor(t).get(tag);
				if (asrc != atgt) {
					if (isNew(asrc))
						moves.add(MemAction.set(atgt));
					else
						moves.add(MemAction.copy(asrc, atgt));
				}
			});
		}
		sortMoves(moves);
		return;
	}
	
	private int getState(DFA.State st, ArrayList<MemAction> moves) {
		final DFA.Key key = DFA.getKey(st);
		@Nullable Integer num = Maps.get(stateMap, key);
		if (num != null) {
			moveTo(key.mem, st, stateTable.get(num), moves);
			return num;
		} else {
			num = nextStateNum++;
			DFA.State newst = createNewState(st, moves);
			stateTable.add(newst);
			stateMap.put(key, num);
			todo.push(new StateNum(newst, num));
			return num;
		}
	}
	
	private static final class Indexed<T> {
		final int index;
		final T elt;
		
		Indexed(int index, T elt) {
			this.index = index;
			this.elt = elt;
		}
	}
	
	private <T> void mapOnAllStates(
		Function<DFA.State, T> f, List<Indexed<T>> acc) {
		while (!todo.isEmpty()) {
			StateNum sn = todo.pop();
			T r = f.apply(sn.state);
			acc.add(new Indexed<>(sn.num, r));
		}
	}
	
	private TransActions gotoState(DFA.State st) {
		if (st.isEmpty()) return TransActions.BACKTRACK;
		ArrayList<MemAction> moves = new ArrayList<>(2);
		int num = getState(st, moves);
		return new TransActions(GotoAction.Goto(num), moves);
	}
	
	private Map<TagInfo, Integer> addTagsToMap(AddressGen gen, 
		Set<TagInfo> tags, Map<TagInfo, Integer> locs) {
		if (tags.isEmpty()) return locs; // share if possible
		Map<TagInfo, Integer> newLocs = new HashMap<>(locs);
		for (TagInfo tag : tags) {
			newLocs.remove(tag);
			newLocs.put(tag, gen.allocNewAddr(tag));
		}
		return newLocs;
	}
	
	private DFA.State applyTransition(AddressGen gen,
		DFA.State st, int priority, Map<TagInfo, Integer> locs,
		NFA.Transition trans) {
		
		final Set<TagInfo> tags = trans.tags;
		final int n = trans.event.n;
		switch (trans.event.kind) {
		case ON_CHARS: {
			@Nullable MemMap other = Maps.get(st.others, n);
			// If already some state for this char of higher priority,
			// don't change anything. Otherwise, we add the new
			// mapping (or remplace the old one)
			if (other != null && priority >= other.priority)
				return st;
			return st.withLocs(n,
						new MemMap(priority,
							addTagsToMap(gen, tags, locs)));
		}
		case TO_ACTION: {
			int on = st.finalAction;
			@Nullable MemMap finisher = st.finisher;
			// If this final state has higher priority than the
			// one before, if any, update it
			if (finisher == null || 
				(n < on || (n == on && priority < finisher.priority))) {
				Map<TagInfo, Integer> newLocs =
					addTagsToMap(gen, tags, locs);
				MemMap newFinisher = new MemMap(priority, newLocs);
				return new DFA.State(n, newFinisher, st.others);
			}
			else return st;
		}
		}
		throw new IllegalStateException();
	}
	
	private DFA.State applyTransitions(
		AddressGen gen, DFA.State st, int priority,
		Map<TagInfo, Integer> locs, Set<NFA.Transition> transs) {
		DFA.State res = st;
		for (NFA.Transition tr : transs)
			res = applyTransition(gen, res, priority, locs, tr);
		return res;
	}
	
	private static final class CSetState {
		final CSet chars;
		final DFA.State state;
		
		CSetState(CSet chars, DFA.State state) {
			this.chars = chars;
			this.state = state;
		}
	}
	
	private void refineCharPartition(
		AddressGen gen, Set<NFA.Transition> follow,
		int pos, Map<TagInfo, Integer> locs, CSet chars,
		List<CSetState> partition, int from) {
		
		// If nothing more to do, it's fine
		// (it can happen if empty charset to start with)
		if (from == partition.size()) return;
		
		// Pick the first partition in the remaining part
		// of the character set, from char set s1 to state
		// st1
		final CSetState p = partition.get(from);
		final CSet s1 = p.chars;
		final DFA.State st1 = p.state;
		
		// Find the intersection with the current charset
		final CSet here = CSet.inter(chars, s1);
		if (here.isEmpty()) {
			// If empty, we can refine the remainder
			refineCharPartition(gen,
				follow, pos, locs, chars, partition, from + 1);
		}
		// Chars from chars which will not be accounted for
		// by splitting s1. If there are any, we need to
		// refine them in the remainder of the partition
		final CSet rest = CSet.diff(chars, here);
		if (!rest.isEmpty()) {
			refineCharPartition(gen,
				follow, pos, locs, rest, partition, from + 1);
			// NB: this only changes the back of the
			// list, so we can continue local insertions
		}
		
		// Compute the state associated to this refined character
		// set, and add it to the partition instead of the old one
		DFA.State newSt =
			applyTransitions(gen, st1, pos, locs, follow);
		partition.set(from, new CSetState(here, newSt));
		
		// If not all characters from s1 are accounted by chars,
		// we need to keep them in the partition
		final CSet stay = CSet.diff(s1, here);
		if (!stay.isEmpty())
			partition.add(from, new CSetState(stay, st1));
	}
	
	private List<CSetState> computeShiftTable(
		AddressGen gen, List<CSet> charsets, 
		@NonNull Set<NFA.Transition>[] follows, Map<Integer, MemMap> st) {
		
		final List<CSetState> partition = new ArrayList<>(4);
		// Start with a trivial partition: all chars to nowhere
		partition.add(new CSetState(CSet.ALL, DFA.State.EMPTY));
		// and refine it for every possible outgoing charset
		st.forEach((pos, mmap) -> {
			@NonNull Set<NFA.Transition> follow = follows[pos];
			refineCharPartition(gen, follow, pos, 
				mmap.locs, charsets.get(pos), partition, 0);
		});
		return partition;
	}
	
	private Map<CSet, TransActions> reachable(
		List<CSet> charsets, @NonNull Set<NFA.Transition>[] follows, 
		Map<Integer, MemMap> st) {
		final AddressGen gen = new AddressGen();
		// Build the association list from char set to new states
		List<CSetState> charMap = computeShiftTable(gen, charsets, follows, st);
		// Change it into a mapping from char set to goto actions
		// (in particular this replaces states by their numbers, so
		//  it takes care of canonizing states, or creating new ones
		//  on the todo stack)
		Map<CSet, TransActions> res = new HashMap<>(charMap.size());
		charMap.forEach(css -> {
			res.put(css.chars, gotoState(css.state));
		});
		return res;
	}
	
	private int getTagMem(int action, 
		@NonNull Map<TagInfo, Integer>[] env, TagInfo t) {
		Map<TagInfo, Integer> locs = env[action];
		@Nullable Integer res = Maps.get(locs, t);
		if (res == null) throw new IllegalStateException();
		return res;
	}
	
	private List<TagAction> doTagActions(int action,
		@NonNull Map<TagInfo, Integer>[] env, Map<TagInfo, Integer> locs) {
		List<TagAction> actions = new ArrayList<>(locs.size());
		// First compute the set of used memory cells, and the associated
		// tag actions
		Set<Integer> used = Sets.create();
		locs.forEach((t, m) -> {
			int a = getTagMem(action, env, t);
			used.add(a);
			actions.add(TagAction.SetTag(a, m));
		});
		// Now go through the final environment associated to the
		// action and erase all those that are unused starting tags
		env[action].forEach((tag, m) -> {
			if (tag.start && !used.contains(m)) {
				used.add(m);
				actions.add(TagAction.EraseTag(m));
			}
		});
		// XXX Shall I reverse the actions list? Is order important?
		return actions;
	}
	
	private DFA.Cell translateState(boolean shortest,
		@NonNull Map<TagInfo, Integer>[] tags, List<CSet> charsets, 
		@NonNull Set<NFA.Transition>[] follows, DFA.State st) {
		final int n = st.finalAction;
		final Map<TagInfo, Integer> m = st.getFinalLocs();
		// If there are no successors after [st], it must be final
		// and we can just perform the associated semantic action
		if (st.others.isEmpty()) {
			if (!st.isFinal()) throw new IllegalStateException();
			return new DFA.Perform(n, doTagActions(n, tags, m));
		}
		// If we are interested in shortest match instead of 
		// longest match, then we can stop as soon as we reach
		// a final state, and otherwise we can continue without
		// taking care to remember the last encountered final state
		if (shortest) {
			if (st.isFinal())
				return new DFA.Perform(n, doTagActions(n, tags, m));
			else
				return new DFA.Shift(Remember.NOTHING,
							reachable(charsets, follows, st.others));
		}
		// If we are interested in longest match, we never stop
		// as long as we can shift, but we make sure to remember
		// the last encountered final state
		Remember remember = 
			!st.isFinal() ? Remember.NOTHING :
				new Remember(n, doTagActions(n, tags, m));
		return new DFA.Shift(remember, reachable(charsets, follows, st.others));
	}
	
	private static void addTagEntries(int action,
		String id, IdentInfo info, Map<TagInfo, Integer> locs) {
		TagAddr start = info.start;
		if (start.base >= 0 && start.offset == 0) {
			locs.put(new TagInfo(id, true, action), start.base);
		}
		@Nullable TagAddr end = info.end;
		if (end != null && end.base >= 0 && end.offset == 0) {
			locs.put(new TagInfo(id, false, action), end.base);
		}
	}
	
	private static @NonNull Map<TagInfo, Integer>[]
		extractTags(List<Finisher> finishers) {
		@SuppressWarnings("unchecked")
		@NonNull Map<TagInfo, Integer>[] res = 
			new @NonNull Map[finishers.size()];
		// Gather all actual tags used as bases in finishers
		for (Finisher finisher : finishers) {
			final int act = finisher.action;
			if (res[act] != null) throw new IllegalStateException();
			Map<TagInfo, Integer> locs = Maps.create();
			finisher.tags.forEach((name, info) -> {
				addTagEntries(act, name, info, locs);
			});
		}
		// Check that all spots are accounted for
		for (int i = 0; i < res.length; ++i)
			if (res[i] == null) throw new IllegalStateException();
		return res;
	}
	

	@SuppressWarnings("javadoc")
	public static Automata lexer(Lexer lexer) {
		// First get a tagged optimized version of the lexer entries
		final TLexer tlexer = Encoder.encodeLexer(lexer);
		// Compute the follow sets for the whole entries
		Set<NFA.Transition>[] follows =
			NFA.followPos(tlexer.charsets.size(), tlexer.entries);
		
		// Create a fresh determinization context
		Determinize det = new Determinize();
		List<Indexed<DFA.Cell>> indexedCells = new ArrayList<>(); 
		List<Automata.@NonNull Entry> automataEntries =
			new ArrayList<>(tlexer.entries.size());
		
		// For every rule, compute the corresponding initializers
		// and finishers, and create all the corresponding cells
		for (final TLexerEntry tentry : tlexer.entries) {
			// Extract all tags from this entry's actions
			final @NonNull Map<TagInfo, Integer>[] tags = extractTags(tentry.actions);
			det.resetPartial(tentry.memTags);
			
			// Compute the initial state by looking at the 
			// set of first possible transitions
			final Set<NFA.Transition> possible = NFA.firstPos(tentry.regexp);
			DFA.State initState = det.createInitState(possible);
			final ArrayList<MemAction> initActions = new ArrayList<>(2);
			final int initNum = det.getState(initState, initActions);
			
			// Perform the closure of all states reachable from
			// this initial state. This fills the indexedCells array
			// by side effect
			det.mapOnAllStates(st ->
				det.translateState(tentry.shortest, tags, 
								   tlexer.charsets, follows, st), 
				indexedCells);
			
			// And finally register the automaton entry corresponding
			// to this lexer entry
			Automata.Entry autoEntry =
				new Automata.Entry(tentry.name, tentry.args, 
						det.tempPending ? det.nextMemCell + 1 : det.nextMemCell, 
						initNum, initActions, tentry.actions);
			automataEntries.add(autoEntry);
		}
		
		// Gather all constructed cells in an array
		if (det.nextStateNum != indexedCells.size())
			throw new IllegalStateException();
		DFA.Cell[] cells = new DFA.Cell[det.nextStateNum];
		indexedCells.forEach(icell -> {
			if (cells[icell.index] != null)
				throw new IllegalStateException();
			cells[icell.index]= icell.elt;
		});
		// Because we checked for duplicates and we set
		// as many cells as the size, we know there are no
		// nulls anymore
		@SuppressWarnings("null")
		DFA.@NonNull Cell[] checkedCells = cells;
		
		// Job done! We can return the full deterministic automata
		return new Automata(automataEntries, checkedCells);
	}
	
}