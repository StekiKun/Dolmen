package automaton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import common.Maps;
import common.Sets;
import tagged.NFA;
import tagged.TRegular.TagInfo;

public class DFA {

	private DFA() {
		// Static utilities only
	}

	/** A special number to mark the absence of an action */
	public static final int NO_ACTION = Integer.MAX_VALUE;
	
	public static final class MemMap {
		public final int id;
		public final Map<TagInfo, Integer> locs;
		
		MemMap(int id, Map<TagInfo, Integer> locs) {
			this.id = id;
			this.locs = locs;
		}
	}
	
	public static final class State {
		public final int finalAction;
		public @Nullable final MemMap finisher;
		
		public final Map<Integer, MemMap> others;
		
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
		
		public static final State EMPTY =
			new State(Maps.empty());
		
		public boolean isEmpty() {
			return finalAction == NO_ACTION
				&& others.isEmpty();
		}
		
		public boolean isFinal() {
			return finalAction == NO_ACTION;
		}
	}
	
	public static final class TEquiv {
		public final TagInfo tag;
		public final Set<Set<NFA.Event>> equiv;
		
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
	
	public static final class Key {
		public final Set<NFA.Event> state;
		public final Set<TEquiv> mem;
		
		Key(Set<NFA.Event> state, Set<TEquiv> mem) {
			this.state = state;
			this.mem = mem;
		}
	}
	
	private static class TagEvents {
		final TagInfo tag;
		final Set<NFA.Event> transitions;
		
		TagEvents(TagInfo tag, Set<NFA.Event> transitions) {
			this.tag = tag;
			this.transitions = transitions;
		}
	}
	
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
		// ...
		Set<TEquiv> res = new HashSet<>();
		env1.forEach((tag, ss) ->
			res.add(new TEquiv(tag, ss)));
		return res;
	}
	
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
	
	protected static Key getKey(State s) {
		// The mem part of the key is ...
		Map<Integer, TagEvents> env = new HashMap<>();
		@Nullable MemMap fmap = s.finisher;
		if (fmap != null)
			inverseMemMap(NFA.Event.toAction(s.finalAction),
						  fmap.locs, env);
		s.others.forEach((n, mmap) ->
			inverseMemMap(NFA.Event.onChars(n),
						  mmap.locs, env));
		Set<TEquiv> memKey = Sets.empty();
		
		// The state part of the key is all the NFA states
		// appearing in s
		Set<NFA.Event> stateKey =
			s.isFinal() ? Sets.empty() :
				Sets.singleton(NFA.Event.toAction(s.finalAction));
		s.others.keySet().forEach(
			n -> stateKey.add(NFA.Event.onChars(n)));
		
		return new Key(stateKey, memKey);
	}
}
