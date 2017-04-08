package tagged;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.CSet;
import common.Iterables;
import common.Maps;
import tagged.TRegular.Action;
import tagged.TRegular.Alternate;
import tagged.TRegular.Characters;
import tagged.TRegular.Repetition;
import tagged.TRegular.Sequence;
import tagged.TRegular.Tag;
import tagged.TRegular.TagInfo;

/**
 * This class contains various utility methods 
 * about {@link TRegular tagged regular expressions}.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class TRegulars {

	private TRegulars() {
		// Static utilities only
	}

	/**
	 * <i>This is not an equivalence test between tagged
	 * 	regular expressions, in the sense that it can return
	 *  {@code false} on two different regular expressions
	 *  which would otherwise recognize the same language.</i>
	 * <p>
	 * <b>Beware that this structural test does not make
	 * 	sense if {@code r1} and {@code r2} have been encoded
	 *  in different {@link Encoder encoders}, because they
	 *  would not use the same character maps.</b>
	 * 
	 * @param r1
	 * @param r2
	 * @return {@code true} if and only if the two given
	 * 		tagged regular expressions are structurally equal
	 */
	public static boolean equal(TRegular r1, TRegular r2) {
		// Shortcuts
		if (r1 == r2) return true;
		if (r1.kind != r2.kind) return false;
		if (r1.size != r2.size) return false; 
		if (r1.hasTags != r2.hasTags) return false;
		if (r1.hasActions != r2.hasActions) return false;
		
		switch (r1.getKind()) {
		case EPSILON:
			return true;
		case CHARACTERS: {
			final Characters chars1 = (Characters) r1;
			final Characters chars2 = (Characters) r2;
			return chars1.chars == chars2.chars;
		}
		case TAG: {
			final Tag tag1 = (Tag) r1;
			final Tag tag2 = (Tag) r2;
			return tag1.tag.equals(tag2.tag);
		}
		case ALTERNATE: {
			final Alternate alt1 = (Alternate) r1;
			final Alternate alt2 = (Alternate) r2;
			return equal(alt1.lhs, alt2.lhs)
					&& equal(alt1.rhs, alt2.rhs);
		}
		case SEQUENCE: {
			final Sequence seq1 = (Sequence) r1;
			final Sequence seq2 = (Sequence) r2;
			return equal(seq1.first, seq2.first)
					&& equal(seq1.second, seq2.second);
		}
		case REPETITION: {
			final Repetition rep1 = (Repetition) r1;
			final Repetition rep2 = (Repetition) r2;
			return equal(rep1.reg, rep2.reg);
		}
		case ACTION: {
			final Action act1 = (Action) r1;
			final Action act2 = (Action) r2;
			return act1.action == act2.action;
		}
		}
		throw new IllegalStateException();
	}

	/**
	 * The result of a successful match of a tagged
	 * regular expression against some input string
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class MatchResult {
		/**
		 * Contains all positions in the input string
		 * associated to the tags found when matching
		 */
		public final Map<TagInfo, Integer> markers;
		/**
		 * The size of the input string prefix that matched
		 * the regular expression.
		 */
		public final int matchedLength;
		/**
		 * Whether the end-of-input has been matched,
		 * in which case {@link #matchedLength} should be
		 * the length of the whole input, and no more 
		 * can be matched (even {@link TRegular#EPSILON}).
		 */
		public final boolean reachedEOF;
		
		/**
		 * @param markers
		 * @param matchedLength
		 * @param reachedEOF
		 */
		public MatchResult(Map<TagInfo, Integer> markers, 
				int matchedLength, boolean reachedEOF) {
			this.markers = markers;
			this.matchedLength = matchedLength;
			this.reachedEOF = reachedEOF;
		}
		
		@Override
		public @NonNull String toString() {
			return "Matched " + matchedLength + 
					" chars" + (reachedEOF ? " including EOF" : "") +
					", markers: " + markers.toString();
		}
	}
	
	private static final Iterable<MatchResult> NO_MATCH = Iterables.empty();
	
	/**
	 * @param charSets	the character sets dictionary
	 * @param regular
	 * @param input
	 * @param from
	 * @return an iterable of all potential matchings
	 * 		of (a prefix of) the {@code input} string
	 * 		starting at offset {@code from}
	 */
	private static Iterable<MatchResult>
		match(List<CSet> charSets, TRegular regular, String input, int from) {
		int rem = input.length() - from;
		// There's no hope there if not enough remaining characters
		if (regular.size > rem) return NO_MATCH;
		
		switch (regular.getKind()) {
		case EPSILON: {
			return Iterables.singleton(
					new MatchResult(Maps.empty(), from, false));
		}
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			if (characters.eof) {
				if (from == input.length())
					return Iterables.singleton(
						new MatchResult(Maps.empty(), from, true));
				else
					return NO_MATCH;
			}
			else {
				if (from >= input.length())
					return NO_MATCH;
				char next = input.charAt(from);
				if (charSets.get(characters.chars).contains(next))
					return Iterables.singleton(
						new MatchResult(Maps.empty(), from + 1, false));
				else
					return NO_MATCH;
			}
		}
		case TAG: {
			final Tag tag = (Tag) regular;
			return Iterables.singleton(
				new MatchResult(Maps.singleton(tag.tag, from), from, false));
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			Iterable<MatchResult> res1 = match(charSets, alternate.lhs, input, from);
			Iterable<MatchResult> res2 = match(charSets, alternate.rhs, input, from);
			return Iterables.concat(res1, res2);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			Iterable<MatchResult> res1 = match(charSets, sequence.first, input, from);
			// For each match of the first part, try to match the second part
			Function<MatchResult, Iterable<MatchResult>> f = mr1 -> {
				// If already reached end-of-input, nothing more can be matched
				if (mr1.reachedEOF) return Iterables.empty();
				Iterable<MatchResult> res2 =
					match(charSets, sequence.second, input, mr1.matchedLength);
				// If no markers to reconcile
				if (mr1.markers.isEmpty()) return res2;
				// Otherwise, we need to extend res1 markers with res2
				return Iterables.transform(res2, (MatchResult mr2) -> {
					Map<TagInfo, Integer> extended;
					if (mr2.markers.isEmpty())
						extended = mr1.markers;
					else {
						extended = new HashMap<TagInfo, Integer>(mr1.markers);
						extended.putAll(mr2.markers);
					}
					return new MatchResult(extended, mr2.matchedLength, mr2.reachedEOF);
				});
			};
			return Iterables.concat(Iterables.transform(res1, f));
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			// Either we match the empty string, or the underlying
			// regexp followed by another match of this repetition
			// This could go on infinitely if the regexp below is nullable,
			// so we use an arbitrary bound (>= than the one used in
			// witnesses so all generated witnesses can match)
			return Iterables.concat(new Iterable<Iterable<MatchResult>>() {
				@Override
				public Iterator<@NonNull Iterable<MatchResult>> iterator() {
					return new Iterator<@NonNull Iterable<MatchResult>>() {
						private int level = 0;
						private TRegular unfolded = TRegular.EPSILON;
						
						@Override
						public boolean hasNext() {
							// arbitrary bound
							return level <= 2;
						}

						@Override
						public @NonNull Iterable<@NonNull MatchResult> next() {
							if (!hasNext()) throw new NoSuchElementException();
							Iterable<MatchResult> res =
								match(charSets, unfolded, input, from);
							unfolded = TRegular.seq(repetition.reg, unfolded);
							++level;
							return res;
						}
					};
				}
			});
		}
		case ACTION: {
			@SuppressWarnings("unused")
			final Action action = (Action) regular;
			return Iterables.singleton(new MatchResult(Maps.empty(), from, false));
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * @param charSets	the character set dictionary
	 * @param regular
	 * @param input
	 * @return an iterable of all potential match results of the tagged
	 * 		regular expression {@code regular} and a prefix of {@code input}
	 */
	public static Iterable<MatchResult> 
		allMatches(List<CSet> charSets, TRegular regular, String input) {
		return match(charSets, regular, input, 0);
	}
	
	/**
	 * @param charSets	the character set dictionary
	 * @param regular
	 * @param input
	 * @return {@code null} if {@code regular} does not match the full
	 * 		{@code input} string, or the map of matched tags if a match
	 * 		was found
	 */
	public static @Nullable Map<TagInfo, Integer>
		matches(List<CSet> charSets, TRegular regular, String input) {
		final int size = input.length();
		// Find a match which covered the whole input string
		for (MatchResult mr : allMatches(charSets, regular, input)) {
			if (mr.matchedLength == size) return mr.markers;
		}
		return null;
	}
	
	/**
	 * A witness string, i.e. a potential matcher
	 * along with the info of whether this string
	 * can still be extended or not, i.e. whether
	 * it is a witness of a regexp that matched
	 * end-of-input or not
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static class Witness {
		String witness;
		boolean eof;
		
		Witness(String witness) {
			this.witness = witness;
			this.eof = false;
		}
		Witness(String witness, boolean eof) {
			this.witness = witness;
			this.eof = eof;
		}
	}
	/**
	 * @param charSets	the character set dictionary
	 * @param regular
	 * @return an iterable view of potential matching strings
	 * 	for the given tagged regular expression
	 */
	private static Iterable<Witness>
		witnesses_(List<CSet> charSets, TRegular regular) {
		switch (regular.getKind()) {
		case EPSILON:
		case TAG:
		case ACTION:
			return Iterables.singleton(new Witness(""));
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			if (characters.eof) {
				Witness eof = new Witness("", true);
				return Iterables.singleton(eof);
			}
			else {
				CSet charSet = charSets.get(characters.chars);
				return Iterables.transform(CSet.witnesses(charSet),
						c -> new Witness("" + c));
			}
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			Iterable<Witness> wit1 = witnesses_(charSets, alternate.lhs);
			Iterable<Witness> wit2 = witnesses_(charSets, alternate.rhs);
			// TODO: could be nicer with an interleaved union?
			return Iterables.concat(wit1, wit2);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			final Iterable<Witness> wit1 = witnesses_(charSets, sequence.first);
			final Iterable<Witness> wit2 = witnesses_(charSets, sequence.second); 
			return Iterables.concat(
				Iterables.transform(wit1,
					(Witness w1) -> {
						if (w1.eof) return Iterables.empty();
						return Iterables.transform(wit2, 
							(Witness w2) -> new Witness(w1.witness + w2.witness, w2.eof));
					}));
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			// Finite approx: at most 0, 1 or 2 reps
			return Iterables.concat(
						witnesses_(charSets, TRegular.EPSILON),
						witnesses_(charSets, repetition.reg),
						witnesses_(charSets, TRegular.seq(repetition.reg, repetition.reg)));
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * @param charSets	the character set dictionary
	 * @param regular
	 * @return an iterable view of potential matching strings
	 * 	for the given tagged regular expression
	 */
	public static Iterable<String>
		witnesses(List<CSet> charSets, TRegular regular) {
		return Iterables.transform(
				witnesses_(charSets, regular), w -> w.witness);
	}
}
