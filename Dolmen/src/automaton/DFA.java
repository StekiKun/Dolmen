package automaton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import automaton.NFA.Event;
import common.Maps;
import common.Sets;
import tagged.TRegular.TagInfo;

/**
 * Describes a tagged Deterministic Finite Automaton 
 * recognizing the language of some given tagged regular
 * expression. This automaton's states represent sets
 * of states in the Non-determinitic Finite Automaton,
 * along with memory maps describing where tags values
 * can be found in memory cells.
 * 
 * @author Stéphane Lescuyer
 */
public class DFA {

	private DFA() {
		// Static utilities only
	}

	/** A special number to mark the absence of an action */
	public static final int NO_ACTION = Integer.MAX_VALUE;
	
	/**
	 * A <i>memory map</i> describes what memory cell 
	 * corresponds to each tag. Tags which are not 
	 * bound in {@link #locs} are unititialized.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class MemMap {
		/** Priority */
		public final int priority;
		/** Memory addresses associated to each initialized tag */
		public final Map<TagInfo, @NonNull Integer> locs;
		
		MemMap(int priority, Map<TagInfo, Integer> locs) {
			this.priority = priority;
			this.locs = locs;
		}
	}
	
	/**
	 * A state in the (tagged) deterministic finite automaton
	 * corresponds to a set of states in the non-deterministic
	 * automaton, each being associated with a 
	 * {@link MemMap memory map}.
	 * <p>
	 * NFA States of the form {@link Event#onChars(int)}
	 * are accounted for in {@link #others}, and are indexed
	 * by the corresponding character set index. Besides,
	 * there can be at most one state of the form 
	 * {@link Event#toAction(int)}, i.e. a final state, in
	 * which case the DFA state is final itself. If there were
	 * more than one final state, there would be ambiguity
	 * as to what action should be performed when accepting
	 * the input.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class State {
		/** 
		 * Describes the semantic action associated
		 * to this state, if final, or {@link DFA#NO_ACTION}
		 * if the state is not final
		 */
		public final int finalAction;
		/** 
		 * The memory map associated to the final action
		 * or {@code null} if the state isn't final
		 */
		public @Nullable final MemMap finisher;
		
		/**
		 * Maps character sets indices to memory maps.
		 * The NFA states corresponding to these character
		 * sets are the non-final states in this DFA state.
		 */
		public final Map<@NonNull Integer, MemMap> others;
		
		State(int finalAction, MemMap finisher,
				Map<Integer, MemMap> others) {
			this.finalAction = finalAction;
			this.finisher = finisher;
			this.others = others;
		}
		
		State(Map<Integer, MemMap> others) {
			this.finalAction = NO_ACTION;
			this.finisher = null;
			this.others = others;
		}
		
		/** The empty state */
		public static final State EMPTY =
			new State(Maps.empty());
		
		/** Whether the DFA state is empty */
		public boolean isEmpty() {
			return finalAction == NO_ACTION
				&& others.isEmpty();
		}
		
		/** Whether the DFA state is final */
		public boolean isFinal() {
			return finalAction != NO_ACTION;
		}
		
		/**
		 * @return the location map associated to the final
		 *  action in this state, or an empty map if the
		 *  state isn't final
		 */
		public Map<TagInfo, Integer> getFinalLocs() {
			@Nullable MemMap mmap = finisher;
			if (mmap == null)
				return Maps.empty();
			return mmap.locs;
		}
		
		/**
		 * @param event
		 * @return the location map associated to the given NFA state
		 * @throws IllegalArgumentException if the given state is not
		 * 		part of this DFA state (in particular, if it is a different
		 * 		final action that the one specified in the DFA state)
		 */
		public Map<TagInfo, Integer> getLocsFor(NFA.Event event) {
			switch (event.kind) {
			case ON_CHARS: {
				@Nullable MemMap mmap = Maps.get(others, event.n);
				if (mmap == null) throw new IllegalArgumentException();
				return mmap.locs;
			}
			case TO_ACTION: {
				if (!isFinal()) throw new IllegalArgumentException();
				if (finalAction != event.n) throw new IllegalArgumentException();
				return getFinalLocs();
			}
			}
			throw new IllegalStateException();
		}
		
		/**
		 * @param n
		 * @param newMmap
		 * @return a new DFA state exactly like the receiver except
		 * 	that the character set {@code n} is mapped to {@code newMap}
		 */
		public DFA.State withLocs(int n, MemMap newMmap) {
			Map<Integer, MemMap> newOthers = new HashMap<>(others);
			newOthers.put(n, newMmap);
			if (finisher != null)
				return new State(finalAction, finisher, newOthers);
			else
				return new State(newOthers);
		}
	}
	
	/**
	 * Abstraction of the memory maps in a DFA state
	 * 
	 * Describes a {@link TEquiv#tag} and all the possible
	 * sets of transitions that use this tag with some common
	 * memory cell. This representation is not dependent of
	 * the particular memory cells used and therefore DFA
	 * states with the same {@link TEquiv} structure have
	 * similar memory maps that could be made the same by
	 * moving some memory cells around.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TEquiv {
		/** A tag */
		public final TagInfo tag;
		/** 
		 * Sets of NFA states which define the tag
		 * in a common memory cell
		 */
		public final Set<Set<NFA.@NonNull Event>> equiv;
		
		TEquiv(TagInfo tag, Set<Set<NFA.Event>> equiv) {
			this.tag = tag;
			this.equiv = equiv;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + equiv.hashCode();
			result = prime * result + tag.hashCode();
			return result;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TEquiv other = (TEquiv) obj;
			if (!equiv.equals(other.equiv))
				return false;
			if (!tag.equals(other.tag))
				return false;
			return true;
		}
		
	}
	
	/**
	 * A key is an abstraction of a {@link State DFA state}
	 * which represents the set of NFA states and memory maps
	 * that the DFA state stands for, in such a way that two
	 * different states with the same key can be made equal
	 * by copying some memory cells in others.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Key {
		/** The set of NFA states (the 'untagged' DFA state) */
		public final Set<NFA.Event> state;
		/** 
		 * The set of {@link TEquiv} structures which abstract,
		 * for every tag defined in the DFA state, 
		 */
		public final Set<@NonNull TEquiv> mem;
		
		Key(Set<NFA.Event> state, Set<TEquiv> mem) {
			this.state = state;
			this.mem = mem;
		}
	}
	
	/**
	 * Packs together some {@link #tag tag} and a set of
	 * NFA states (taken from a DFA state) that define 
	 * this tag in the same memory cell. Used as an 
	 * intermediate structure when computing the key of 
	 * some DFA state.
	 * 
	 * @author Stéphane Lescuyer
	 * @see DFA#inverseMemMap(Event, Map, Map)
	 * @see DFA#envToClass(Map)
	 */
	private static class TagEvents {
		final TagInfo tag;
		final Set<NFA.Event> transitions;
		
		TagEvents(TagInfo tag, Set<NFA.Event> transitions) {
			this.tag = tag;
			this.transitions = transitions;
		}
	}
	
	/**
	 * @param m
	 * @return the memory map abstraction corresponding to
	 * 	the provided map {@code m}, which associates 
	 *  {@link TagEvent}s to memory cells
	 */
	private static Set<TEquiv> envToClass(Map<Integer, TagEvents> m) {
		// First compute the set of sets of transitions
		// which are associated to some tag info in the
		// provided tag events
		Map<TagInfo, Set<Set<NFA.Event>>> env1 = new HashMap<>();
		m.values().forEach(te -> {
			@Nullable Set<Set<NFA.Event>> x = Maps.get(env1, te.tag);
			if (x == null)
				env1.put(te.tag, Sets.singleton(te.transitions));
			else
				x.add(te.transitions);
		});
		// Then pack them together and return the corresponding set
		Set<TEquiv> res = new HashSet<>();
		env1.forEach((tag, ss) ->
			res.add(new TEquiv(tag, ss)));
		return res;
	}
	
	/**
	 * Extends the map from memory cells to {@link TagEvent}s
	 * with contributions from the NFA state {@code trans} and
	 * memory map {@code m}
	 * 
	 * @param trans
	 * @param m
	 * @param acc
	 */
	private static void inverseMemMap(
		NFA.Event trans, Map<TagInfo, Integer> m, 
		Map<Integer, TagEvents> acc) {
		m.forEach((tag, addr) -> {
			@Nullable TagEvents te = Maps.get(acc, addr);
			if (te == null)
				acc.put(addr, new TagEvents(tag, Sets.singleton(trans)));
			else {
				if (!tag.equals(te.tag))
					throw new IllegalStateException();
				te.transitions.add(trans);
			}
		});
	}
	
	/**
	 * @param s
	 * @return the key correponding to the DFA state {@code s}
	 */
	protected static Key getKey(State s) {
		// The mem part of the key is a set of TEquiv,
		// one per tag appearing in one of s' memory maps
		Map<Integer, TagEvents> env = new HashMap<>();
		@Nullable MemMap fmap = s.finisher;
		if (fmap != null)
			inverseMemMap(NFA.Event.toAction(s.finalAction),
						  fmap.locs, env);
		s.others.forEach((n, mmap) ->
			inverseMemMap(NFA.Event.onChars(n),
						  mmap.locs, env));
		Set<TEquiv> memKey = envToClass(env);
		
		// The state part of the key is all the NFA states
		// appearing in s
		Set<NFA.Event> stateKey =
			s.isFinal() ? Sets.empty() :
				Sets.singleton(NFA.Event.toAction(s.finalAction));
		s.others.keySet().forEach(
			n -> stateKey.add(NFA.Event.onChars(n)));
		
		return new Key(stateKey, memKey);
	}
	
	/**
	 * Action on memory cells to take when following
	 * a DFA transition
	 * <p>
	 * Actions are of two kinds: either {@link Set setting}
	 * some memory cell to the current position input, or
	 * {@link Copy copying} one memory cell's contents into
	 * another. 
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static abstract class MemAction {
		private MemAction() { }
		
		/**
		 * @return the destination of this memory action
		 */
		public abstract int getDest();
		/**
		 * @return the source of this memory action, or
		 * 		-1 if it is a set action and not a copy
		 */
		public abstract int getSrc();
		
		/**
		 * Represents a memory action where some memory
		 * cells {@link #dst} must be set to the current
		 * position input
		 * 
		 * @author Stéphane Lescuyer
		 */
		public static final class Set extends MemAction {
			/** Memory cell to set */
			public final int dst;
			
			private Set(int dst) { this.dst = dst; }

			@Override
			public int getDest() {
				return dst;
			}

			@Override
			public int getSrc() {
				return -1;
			}
		}
		/**
		 * @param dst
		 * @return the memory action which sets the memory cell {@code dst}
		 */
		public static Set set(int dst) {
			return new Set(dst);
		}
		
		/**
		 * Represents a memory action where some meory
		 * cell {@link #src} must be copied into 
		 * another cell {@link #dst}
		 * 
		 * @author Stéphane Lescuyer
		 */
		public static final class Copy extends MemAction {
			/** The memory cell to copy from */
			public final int src;
			/** The memory cell to copy to */
			public final int dst;
			
			private Copy(int src, int dst) {
				this.src = src;
				this.dst = dst;
			}

			@Override
			public int getDest() {
				return dst;
			}

			@Override
			public int getSrc() {
				return src;
			}
		}
		/**
		 * @param src
		 * @param dst
		 * @return the memory action which copies the
		 * 	contents of {@code src} to memory cell {@code dst}
		 */
		public static Copy copy(int src, int dst) {
			return new Copy(src, dst);
		}
	}
	
	/**
	 * Describes an automaton action, i.e. either jumping
	 * to another state with number {@link #target},
	 * or back-tracking
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class GotoAction {
		/** The state to go to */
		public final int target;
		
		private GotoAction(int target) {
			this.target = target;
		}
		
		/** Special back-tracking action */
		public static final GotoAction BACKTRACK =
			new GotoAction(Integer.MIN_VALUE);
		/**
		 * @param n
		 * @return the action to jum to state {@code n}
		 */
		public static GotoAction Goto(int n) {
			if (n < 0) throw new IllegalArgumentException();
			return new GotoAction(n);
		}
	}
}