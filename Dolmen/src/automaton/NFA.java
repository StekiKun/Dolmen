package automaton;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Sets;
import tagged.TLexerEntry;
import tagged.TRegular;
import tagged.TRegulars;
import tagged.TRegular.Action;
import tagged.TRegular.Alternate;
import tagged.TRegular.Characters;
import tagged.TRegular.Epsilon;
import tagged.TRegular.Repetition;
import tagged.TRegular.Sequence;
import tagged.TRegular.Tag;
import tagged.TRegular.TagInfo;

/**
 * Describes a tagged Non-deterministic Finite Automaton 
 * recognizing the language of some given tagged regular
 * expression. In particular, each transition is decorated
 * with the set of {@link TagInfo tags} that must be 
 * updated when following this transition.
 * 
 * @author Stéphane Lescuyer
 */
public class NFA {
	
	private NFA() {
		// Static utility only
	}

	/**
	 * The kinds of events that decorate transitions
	 * in the automaton
	 * 
	 * @author Stéphane Lescuyer
	 */
	public enum EventKind {
		/** Some char is read in from a character set */
		ON_CHARS,
		/** A final state is reached: a semantic action must be performed */
		TO_ACTION
	}
	
	/**
	 * Each transition in the NFA is based on an <i>event</i>,
	 * which is either of the following:
	 * <ul>
	 *   <li> a character is read from the character set
	 *   	  whose index in the character set dictionary
	 *   	  is {@link #n}
	 *   <li> a final state is reached and the semantic
	 *   	  action whose index is {@link #n} must be
	 *   	  performed
	 * </ul>
	 * 
	 * @author Stéphane Lescuyer
	 * @see EventKind
	 */
	public static final class Event {
		/** What kind of event this is */
		public final EventKind kind;
		/** Depending on {@link #kind}, a character set or action index */
		public final int n;
		
		private Event(EventKind kind, int n) {
			this.kind = kind;
			this.n = n;
		}
		
		/**
		 * @param n
		 * @return the event describing that a character
		 * 	from the character with index {@code n} was read
		 */
		public static Event onChars(int n) {
			return new Event(EventKind.ON_CHARS, n);
		}
		/**
		 * @param n
		 * @return the event describing that the final
		 * 	state with semantic action {@code n} is reached
		 */
		public static Event toAction(int n) {
			return new Event(EventKind.TO_ACTION, n);
		}
		
		@Override
		public String toString() {
			return kind.name() + "(" + n + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + kind.hashCode();
			result = prime * result + n;
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
			Event other = (Event) obj;
			if (kind != other.kind)
				return false;
			if (n != other.n)
				return false;
			return true;
		}
	}
	
	/**
	 * A <i>transition</i> in the NFA is an 
	 * {@link Event event} decorated with a set of
	 * {@link TagInfo tags}.
	 * <p> 
	 * The tags represent the
	 * markers which must be updated with the current
	 * input string position when following this
	 * transition.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Transition {
		/** The event of the transition */
		public final Event event;
		/** The set of tags to be updated in this transition */
		public final Set<@NonNull TagInfo> tags;
		
		/**
		 * @param event
		 * @param tags
		 */
		public Transition(Event event, Set<TagInfo> tags) {
			this.event = event;
			this.tags = tags;
		}

		@Override
		public String toString() {
			return "{" + event.toString() + "/" + tags + "}";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + event.hashCode();
			result = prime * result + tags.hashCode();
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
			Transition other = (Transition) obj;
			if (!event.equals(other.event))
				return false;
			if (!tags.equals(other.tags))
				return false;
			return true;
		}
		
	}

	/**
	 * @param transitions
	 * @param tags
	 * @return a set of transitions like {@code transitions}
	 * 	but where all sets of tags have been augmented with {@code tags}
	 */
	private static Set<Transition>
		addTags(Set<Transition> transitions, Set<TagInfo> tags) {
		Set<Transition> res = new HashSet<>(transitions.size());
		for (Transition trans : transitions)
			res.add(new Transition(trans.event, 
								   Sets.union(trans.tags, tags)));
		return res;
	}
	
	/**
	 * @param regular
	 * @return the set of transitions which can start the
	 * 	matching of the given regular expression
	 */
	protected static Set<Transition> firstPos(TRegular regular) {
		switch (regular.getKind()) {
		case EPSILON: {
			@SuppressWarnings("unused")
			final Epsilon epsilon = (Epsilon) regular;
			return Sets.empty();
		}
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			return Sets.singleton(
				new Transition(Event.onChars(characters.chars), 
							   Sets.empty()));
		}
		case TAG: {
			@SuppressWarnings("unused")
			final Tag tag = (Tag) regular;
			return Sets.empty();
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			return Sets.union(
				firstPos(alternate.lhs), firstPos(alternate.rhs));
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			if (sequence.first.nullable) {
				return Sets.union(
					firstPos(sequence.first),
					addTags(firstPos(sequence.second), 
							TRegulars.emptyMatched(sequence.first)));
			}
			else
				return firstPos(sequence.first);
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			return firstPos(repetition.reg);
		}
		case ACTION: {
			final Action action = (Action) regular;
			return Sets.singleton(
					new Transition(Event.toAction(action.action), 
								   Sets.empty()));
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * Computation context for {@link NFA#followPos(int, List)}
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static class FollowPos {
		final Set<Transition>[] fpos;
		
		@SuppressWarnings("unchecked")
		FollowPos(int size) {
			this.fpos = new Set[size];
			Arrays.fill(fpos, null);
		}
		
		void fill(Set<Transition> transs, TRegular regular) {
			switch (regular.getKind()) {
			case EPSILON:
			case ACTION:
			case TAG:
				break;
			case CHARACTERS: {
				final Characters characters = (Characters) regular;
				// Correct because each character set index 
				// only appears once in tagged regexps
				fpos[characters.chars] = transs;
				break;
			}
			case ALTERNATE: {
				final Alternate alternate = (Alternate) regular;
				fill(transs, alternate.lhs);
				fill(transs, alternate.rhs);
				break;
			}
			case SEQUENCE: {
				final Sequence sequence = (Sequence) regular;
				Set<Transition> tr1 = firstPos(sequence.second);
				if (sequence.second.nullable) {
					tr1 = Sets.union(tr1, 
							addTags(transs, TRegulars.emptyMatched(sequence.second)));
				}
				fill(tr1, sequence.first);
				fill(transs, sequence.second);
				break;
			}
			case REPETITION: {
				final Repetition repetition = (Repetition) regular;
				TRegular reg = repetition.reg;
				fill(Sets.union(firstPos(reg), transs), reg);
				break;
			}
			}
		}
	}

	/**
	 * @param entries
	 * @return the set of transitions which can follow each
	 * 	character set when matching the given regular expressions
	 */
	protected static Set<Transition>[]
		followPos(int size, List<TLexerEntry> entries) {
		FollowPos fp = new FollowPos(size);
		for (TLexerEntry entry : entries)
			fp.fill(new HashSet<>(), entry.regexp);
		return fp.fpos;
	}

}