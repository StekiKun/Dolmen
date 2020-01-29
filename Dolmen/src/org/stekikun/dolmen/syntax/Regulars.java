package org.stekikun.dolmen.syntax;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Iterables;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.common.Sets;
import org.stekikun.dolmen.syntax.Regular.Alternate;
import org.stekikun.dolmen.syntax.Regular.Binding;
import org.stekikun.dolmen.syntax.Regular.Characters;
import org.stekikun.dolmen.syntax.Regular.Repetition;
import org.stekikun.dolmen.syntax.Regular.Sequence;

/**
 * This class contains various utility methods 
 * about {@linkplain Regular regular expressions}.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Regulars {

	private Regulars() {
		// Static utility class only
	}
	
	/**
	 * <i>This is not an equivalence test between regular
	 * 	regular expressions, in the sense that it can return
	 *  {@code false} on two different regular expressions
	 *  which would otherwise recognize the same language.</i>
	 * 
	 * @param r1
	 * @param r2
	 * @return {@code true} if and only if the two given
	 * 		regular expressions are structurally equal
	 */
	public static boolean equal(Regular r1, Regular r2) {
		// Shortcuts
		if (r1 == r2) return true;
		if (r1.kind != r2.kind) return false;
		if (r1.size != r2.size) return false; 
		if (r1.hasBindings != r2.hasBindings) return false;
		
		switch (r1.getKind()) {
		case EPSILON:
		case EOF:
			return true;
		case CHARACTERS: {
			final Characters chars1 = (Characters) r1;
			final Characters chars2 = (Characters) r2;
			return CSet.equivalent(chars1.chars, chars2.chars);
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
		case BINDING: {
			final Binding binding1 = (Binding) r1;
			final Binding binding2 = (Binding) r2;
			return binding1.name.equals(binding2.name)
					&& equal(binding1.reg, binding2.reg);
		}
		}
		throw new IllegalStateException();
	}

	/**
	 * This <i>folder</i> transforms the received regular
	 * expression in order to remove all nested bindings, so
	 * that no name binding appears below another similar binding.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class RemoveNestedBindings
		implements Regular.Folder<Regular> {

		private final Set<String> toRemove = new HashSet<String>(2);
		
		@Override
		public Regular epsilon() {
			return Regular.EPSILON;
		}

		@Override
		public Regular eof() {
			return Regular.EOF;
		}

		@Override
		public Regular chars(Characters chars) {
			return chars;
		}

		@Override
		public Regular alternate(Alternate alt) {
			if (!alt.hasBindings) return alt;
			Regular newlhs = alt.lhs.fold(this);
			Regular newrhs = alt.rhs.fold(this);
			if (alt.lhs == newlhs && alt.rhs == newrhs)
				return alt;
			return Regular.or(newlhs, newrhs);
		}

		@Override
		public Regular sequence(Sequence seq) {
			if (!seq.hasBindings) return seq;
			Regular newfirst = seq.first.fold(this);
			Regular newsecond = seq.second.fold(this);
			if (seq.first == newfirst && seq.second == newsecond)
				return seq;
			return Regular.seq(newfirst, newsecond);
		}

		@Override
		public Regular repetition(Repetition rep) {
			if (!rep.hasBindings) return rep;
			Regular newreg = rep.reg.fold(this);
			if (rep.reg == newreg) return rep;
			return Regular.star(newreg);
		}

		@Override
		public Regular binding(Binding binding) {
			if (toRemove.contains(binding.name.val))
				return binding.reg.fold(this);
			else {
				toRemove.add(binding.name.val);
				Regular newreg = binding.reg.fold(this);
				toRemove.remove(binding.name.val);
				if (newreg == binding.reg) return binding;
				return Regular.binding(newreg, binding.name);
			}
		}
	}
	/**
	 * Note that nested bindings with the same name are useless
	 * because if the regular expression matches some string,
	 * the last match bound for some name will always be the 
	 * outermost binding with that name.
	 * 
	 * @param r
	 * @return a regular expression matching the same language
	 * 		as {@code r} but where bindings which appear nested
	 * 		below a binding with the same name have been removed
	 */
	public static Regular removeNestedBindings(Regular r) {
		if (!r.hasBindings) return r;
		return r.fold(new RemoveNestedBindings());
	}
	
	private static Regular rnbAux(Regular r, Set<String> toRemove) {
		if (!r.hasBindings) return r;
		
		switch (r.getKind()) {
		case EPSILON:
		case EOF:
		case CHARACTERS:
			return r;
		case ALTERNATE: {
			final Alternate alt = (Alternate) r;
			Regular newlhs = rnbAux(alt.lhs, toRemove);
			Regular newrhs = rnbAux(alt.rhs, toRemove);
			if (alt.lhs == newlhs && alt.rhs == newrhs)
				return alt;
			return Regular.or(newlhs, newrhs);
		}
		case SEQUENCE: {
			final Sequence seq = (Sequence) r;
			Regular newfirst = rnbAux(seq.first, toRemove);
			Regular newsecond = rnbAux(seq.second, toRemove);
			if (seq.first == newfirst && seq.second == newsecond)
				return seq;
			return Regular.seq(newfirst, newsecond);
		}
		case REPETITION: {
			final Repetition rep = (Repetition) r;
			Regular newreg = rnbAux(rep.reg, toRemove);
			if (rep.reg == newreg) return rep;
			return Regular.star(newreg);
		}
		case BINDING: {
			final Binding binding = (Binding) r;
			if (toRemove.contains(binding.name.val))
				return rnbAux(binding.reg, toRemove);
			else {
				toRemove.add(binding.name.val);
				Regular newreg = rnbAux(binding.reg, toRemove);
				toRemove.remove(binding.name.val);
				if (newreg == binding.reg) return binding;
				return Regular.binding(newreg, binding.name);
			}
		}
		}
		throw new IllegalStateException("Unknown kind: " + r.getKind());
	}
	/**
	 * Note that nested bindings with the same name are useless
	 * because if the regular expression matches some string,
	 * the last match bound for some name will always be the 
	 * outermost binding with that name.
	 * 
	 * @param r
	 * @return a regular expression matching the same language
	 * 		as {@code r} but where bindings which appear nested
	 * 		below a binding with the same name have been removed
	 */
	public static Regular removeNestedBindings2(Regular r) {
		if (!r.hasBindings) return r;
		return rnbAux(r, new HashSet<String>(2));
	}
	
	/**
	 * Gathers information about the nature of various
	 * bindings in a regular expression. This info
	 * can be computed by calling {@link #analyseVars}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class VarsInfo {
		/** All variables bound in the regular expression */
		public final Set<@NonNull String> allVars;
		/** All variables optionally matched in the regular expression */
		public final Set<String> optVars;
		/** 
		 * All variables matched potentially more
		 * than once in the regular expression
		 */
		public final Set<String> dblVars;
		/**
		 * All variables which <b>can</b> match 
		 * sub-expressions that must be of size 1.
		 */
		public final Set<String> chrVars;
		/**
		 * All variables which <b>can</b> match
		 * sub-expressions that are not guaranteed
		 * to be of size 1.
		 */
		public final Set<String> strVars;
		
		/**
		 * @param allVars
		 * @param optVars
		 * @param dblVars
		 * @param chrVars
		 * @param strVars
		 */
		public VarsInfo(Set<String> allVars,
					Set<String> optVars, Set<String> dblVars, 
					Set<String> chrVars, Set<String> strVars) {
			this.allVars = allVars;
			this.optVars = optVars;
			this.dblVars = dblVars;
			this.chrVars = chrVars;
			this.strVars = strVars;
		}
		
		/**
		 * @return a set of variables which, according
		 * 	to this analysis, are guaranteed to always be
		 * 	matched to a substring of size 1, if matched at all
		 */
		public Set<String> getCharVars() {
			return Sets.diff(chrVars, strVars);
		}
	}
	/**
	 * A special instance of {@link VarsInfo} for regular expressions
	 * without any bindings
	 */
	private static VarsInfo NO_VARS =
		new VarsInfo(Collections.emptySet(), Collections.emptySet(),
					 Collections.emptySet(), Collections.emptySet(),
					 Collections.emptySet());
	
	/**
	 * @param regular
	 * @return a {@link VarsInfo} structure describing 
	 * 	the nature of the various bound names appearing
	 * 	in the given regular expression
	 */
	public static VarsInfo analyseVars(Regular regular) {
		if (!regular.hasBindings) return NO_VARS;
		
		switch (regular.getKind()) {
		case EPSILON:
		case EOF:
		case CHARACTERS:
			return NO_VARS;
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			VarsInfo info1 = analyseVars(alternate.lhs);
			VarsInfo info2 = analyseVars(alternate.rhs);
			return new VarsInfo(
				Sets.union(info1.allVars, info2.allVars),
				Sets.union(
					Sets.symdiff(info1.allVars, info2.allVars),
					Sets.union(info1.optVars, info2.optVars)),
				Sets.union(info1.dblVars, info2.dblVars),
				Sets.union(info1.chrVars, info2.chrVars),
				Sets.union(info1.strVars, info2.strVars));
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			VarsInfo info1 = analyseVars(sequence.first);
			VarsInfo info2 = analyseVars(sequence.second);
			return new VarsInfo(
				Sets.union(info1.allVars, info2.allVars),
				Sets.union(info1.optVars, info2.optVars),
				Sets.union(
					Sets.inter(info1.allVars, info2.allVars),
					Sets.union(info1.dblVars, info2.dblVars)),
				Sets.union(info1.chrVars, info2.chrVars),
				Sets.union(info1.strVars, info2.strVars));
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			VarsInfo info = analyseVars(repetition.reg);
			if (info.allVars.equals(info.optVars)
				&& info.allVars.equals(info.dblVars))
				return info;
			return new VarsInfo(info.allVars,
					info.allVars, info.allVars,
					info.chrVars, info.strVars);
		}
		case BINDING: {
			final Binding binding = (Binding) regular;
			VarsInfo info = analyseVars(binding.reg);
			return new VarsInfo(
				Sets.add(binding.name.val, info.allVars),
				info.optVars,
				info.allVars.contains(binding.name.val) ?
					Sets.add(binding.name.val, info.dblVars) :
					info.dblVars,
				binding.reg.size == 1 ?
					Sets.add(binding.name.val, info.chrVars) :
					info.chrVars,
				binding.reg.size == 1 ?
					info.strVars :
					Sets.add(binding.name.val, info.strVars));
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * The result of a successful match of a
	 * regular expression against some input string
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class MatchResult {
		/** 
		 * Contains all bound substrings which were
		 * matched by {@link Regular.Binding} regexps
		 */
		public final Map<String, String> bindings;
		/**
		 * The size of the input string prefix that matched
		 * the regular expression.
		 */
		public final int matchedLength;
		/**
		 * Whether the end-of-input has been matched,
		 * in which case {@link #matchedLength} should be
		 * the length of the whole input, and no more 
		 * can be matched (even {@link Regular#EPSILON}, 
		 * except if non-strict matching is used).
		 */
		public final boolean reachedEOF;
		
		/**
		 * @param bindings
		 * @param matchedLength
		 */
		public MatchResult(Map<String, String> bindings, 
				int matchedLength, boolean reachedEOF) {
			this.bindings = bindings;
			this.matchedLength = matchedLength;
			this.reachedEOF = reachedEOF;
		}
		
		@Override
		public @NonNull String toString() {
			return "Matched " + matchedLength + 
					" chars" + (reachedEOF ? " including EOF" : "") +
					", bindings: " + bindings.toString();
		}
	}
	
	private static final Iterable<MatchResult> NO_MATCH = Iterables.empty();
	
	/**
	 * @param regular
	 * @param input
	 * @param from
	 * @return an iterable of all potential matchings
	 * 		of (a suffix of) the {@code input} string
	 * 		starting at offset {@code from}
	 */
	private static Iterable<MatchResult>
		match(Regular regular, String input, int from, boolean strict) {
		int rem = input.length() - from;
		// There's no hope there if not enough remaining characters
		if (regular.size > rem) return NO_MATCH;
		
		switch (regular.getKind()) {
		case EPSILON: {
			return Iterables.singleton(
					new MatchResult(Maps.empty(), from, false));
		}
		case EOF: {
			if (from != input.length())
				return NO_MATCH;
			return Iterables.singleton(
						new MatchResult(Maps.empty(), from, true));
		}
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			if (from >= input.length())
				return NO_MATCH;
			char next = input.charAt(from);
			if (characters.chars.contains(next))
				return Iterables.singleton(
						new MatchResult(Maps.empty(), from + 1, false));
			else
				return NO_MATCH;
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			Iterable<MatchResult> res1 = match(alternate.lhs, input, from, strict);
			Iterable<MatchResult> res2 = match(alternate.rhs, input, from, strict);
			return Iterables.concat(res1, res2);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			Iterable<MatchResult> res1 = match(sequence.first, input, from, strict);
			// For each match of the first part, try to match the second part
			Function<MatchResult, Iterable<MatchResult>> f = mr1 -> {
				// If already reached end-of-input, nothing more can be matched
				// unless we use non-strict matching in which case a nullable regexp
				// could match
				if (mr1.reachedEOF) {
					if (!strict && sequence.second.nullable)
						return Iterables.singleton(mr1);
					return Iterables.empty();
				}
				Iterable<MatchResult> res2 = match(sequence.second, input, mr1.matchedLength, strict);
				// If no bindings to reconcile
				if (mr1.bindings.isEmpty()) return res2;
				// Otherwise, we need to extend res1 bindings with res2
				return Iterables.transform(res2, (MatchResult mr2) -> {
					Map<String, String> extended;
					if (mr2.bindings.isEmpty())
						extended = mr1.bindings;
					else {
						extended = new HashMap<String, String>(mr1.bindings);
						extended.putAll(mr2.bindings);
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
						private Regular unfolded = Regular.EPSILON;
						
						@Override
						public boolean hasNext() {
							// arbitrary bound
							return level <= 2;
						}

						@Override
						public @NonNull Iterable<@NonNull MatchResult> next() {
							if (!hasNext()) throw new NoSuchElementException();
							Iterable<MatchResult> res = match(unfolded, input, from, strict);
							unfolded = Regular.seq(repetition.reg, unfolded);
							++level;
							return res;
						}
					};
				}
			});
		}
		case BINDING: {
			final Binding binding = (Binding) regular;
			// For each match of the regular expression, compute
			// the associated binding and return the updated match result
			Iterable<MatchResult> res = match(binding.reg, input, from, strict);
			return Iterables.transform(res, mr -> {
				@NonNull String bound = input.substring(from, mr.matchedLength);
				// Save this binding, potentially shadowing other nested bindings
				if (mr.bindings.isEmpty()) {
					Map<String, String> bindings = new HashMap<>();
					bindings.put(binding.name.val, bound);
					return new MatchResult(bindings, mr.matchedLength, mr.reachedEOF);
				}
				mr.bindings.put(binding.name.val, bound);
				return mr;
			});
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * @param regular
	 * @param input
	 * @param strict	if set, nothing can be matched after EOF;
	 * 					if not set, empty strings can still match after EOF
	 * @return an iterable of all potential match results of the
	 * 		regular expression {@code regular} and a prefix of {@code input}
	 */
	public static Iterable<MatchResult> allMatches(Regular regular, String input, boolean strict) {
		return match(regular, input, 0, strict);
	}

	/**
	 * 
	 * @param regular
	 * @param input
	 * @return {@link #allMatches(Regular, String, boolean) allMatches(regular, input, true)}
	 */
	public static Iterable<MatchResult> allMatches(Regular regular, String input) {
		return allMatches(regular, input, true);
	}
	
	/**
	 * @param regular
	 * @param input
	 * @param strict	if set, nothing can be matched after EOF;
	 * 					if not set, empty strings can still match after EOF
	 * @return {@code null} if {@code regular} does not match the full
	 * 		{@code input} string, or the map of bound substrings if a match
	 * 		was found
	 */
	public static @Nullable Map<String, String> matches(Regular regular, String input, boolean strict) {
		final int size = input.length();
		// Find a match which covered the whole input string
		for (MatchResult mr : allMatches(regular, input, strict)) {
			if (mr.matchedLength == size) return mr.bindings;
		}
		return null;
	}

	/**
	 * @param regular
	 * @param input
	 * @return {@link #matches(Regular, String, boolean) matches(regular, input, true)}
	 */
	public static @Nullable Map<String, String> matches(Regular regular, String input) {
		return matches(regular, input, true);
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
	 * @param regular
	 * @return an iterable view of potential matching strings
	 * 	for the given regular expression
	 */
	private static Iterable<Witness> witnesses_(Regular regular) {
		switch (regular.getKind()) {
		case EPSILON: {
			return Iterables.singleton(new Witness(""));
		}
		case EOF: {
			Witness eof = new Witness("", true);
			return Iterables.singleton(eof);
		}
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			return Iterables.transform(CSet.witnesses(characters.chars),
					c -> new Witness("" + c));
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			Iterable<Witness> wit1 = witnesses_(alternate.lhs);
			Iterable<Witness> wit2 = witnesses_(alternate.rhs);
			// TODO: could be nicer with an interleaved union?
			return Iterables.concat(wit1, wit2);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			final Iterable<Witness> wit1 = witnesses_(sequence.first);
			final Iterable<Witness> wit2 = witnesses_(sequence.second); 
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
						witnesses_(Regular.EPSILON),
						witnesses_(repetition.reg),
						witnesses_(Regular.seq(repetition.reg, repetition.reg)));
		}
		case BINDING: {
			final Binding binding = (Binding) regular;
			return witnesses_(binding.reg);
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * @param regular
	 * @return an iterable view of potential matching strings
	 * 	for the given regular expression
	 */
	public static Iterable<String> witnesses(Regular regular) {
		return Iterables.transform(witnesses_(regular), w -> w.witness);
	}
	
	/**
	 * @param regular
	 * @return the set of characters that can start a
	 * 	(non-empty) match of the given regular expression
	 */
	public static CSet first(Regular regular) {
		switch (regular.getKind()) {
		case EPSILON:
			return CSet.EMPTY;
		case EOF:
			return CSet.EOF;
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			return characters.chars;
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			return CSet.union(first(alternate.lhs), first(alternate.rhs));
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			CSet res = first(sequence.first);
			if (sequence.first.nullable)
				res = CSet.union(res, first(sequence.second));
			return res;
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			return first(repetition.reg);
		}
		case BINDING: {
			final Binding binding = (Binding) regular;
			return first(binding.reg);
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * The two maps {@code m1} and {@code m2} must be injective, i.e. they must
	 * have keys which are pairwise disjoint character sets. The returned map
	 * has the same property.
	 * 
	 * @param m1
	 * @param m2
	 * @return result of merging the two character maps {@code m1} and {@code m2}
	 * 	such that if a <i>character</i> was mapped to some regular expressions {@code r1}
	 *  and {@code r2} respectively, it is mapped to {@code Regular.or(r1, r2)} in the
	 *  result
	 */
	private static Map<CSet, Regular> merge(Map<CSet, Regular> m1, Map<CSet, Regular> m2) {
		if (m1.isEmpty()) return m2;
		if (m2.isEmpty()) return m1;
		// Merge the smaller map into the other
		if (m2.size() > m1.size()) return merge(m2, m1);
		// Merge m2 into m1. At any time in the partitioning process
		// [res]'s keys are all pairwise disjoint character sets
		Map<CSet, Regular> res = new HashMap<>(m1);
		m2.forEach((cs, r) -> {
			// Merging a binding cs -> r in the map res:
			//  - for every part of cs that intersects with some existing
			//    binding k -> rk, rebind k - cs -> rk, k inter cs to rk | r
			//    and do cs <- cs - k
			//  - if anything remains of cs, bind it to r
			CSet rem = cs;
			Map<CSet, Regular> refined = new HashMap<>(); 
			Iterator<Map.@NonNull Entry<@NonNull CSet, @NonNull Regular>> it = res.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<@NonNull CSet, @NonNull Regular> e = it.next();
				CSet k = e.getKey(); Regular rk = e.getValue();
				CSet inter = CSet.inter(k, rem);
				if (inter.isEmpty()) continue;
				// Remove k -> rk from the current map
				// Schedule refined bindings for later (avoiding concurrent
				// modifications with the iteration)
				it.remove();
				refined.put(inter, Regular.or(r, rk));
				CSet diff = CSet.diff(k, rem);
				if (!diff.isEmpty())
					refined.put(diff, rk);
				rem = CSet.diff(rem, inter);
				// Break early if possible
				if (rem.isEmpty()) break;
			}
			res.putAll(refined);
			if (!rem.isEmpty())
				res.put(rem, r);
		});
		// Sanity checks
		CSet dom1 = m1.keySet().stream().reduce(CSet.EMPTY, CSet::union);
		CSet dom2 = m2.keySet().stream().reduce(CSet.EMPTY, CSet::union);
		CSet dom12 = res.keySet().stream().reduce(CSet.EMPTY, CSet::union);
		assert (CSet.equivalent(CSet.union(dom1, dom2), dom12)) :
			"Sanity check failed: " + m1 + m2 + " and " + res;
		return res;
	}
	
	/**
	 * The <i>projection</i> of a regular expression on a character set provides
	 * regular expressions for all character in the set, with the following 
	 * meaning: a string {@code s} with first character {@code c} matches the
	 * regular expression {@code r} if and only if {@code s.substring(1)} matches
	 * the projection of {@code r} on the character {@code c}.
	 * <p>
	 * This method returns a map which groups together all characters for which
	 * the projection is the same: it associates various character sets to their
	 * common corresponding projection of the given regular expression. The map
	 * has the following properties:
	 * <ul>
	 * <li> it does not contain empty character sets
	 * <li> characters which cannot start a match of {@code regular}, or which
	 * 		do not belong to {@code first}, are not mapped at all
	 * <li> all the keys are pairwise disjoint character sets
	 * </ul>
	 * <p><i>Important: The description of a projection only holds when
	 * considering <b>non-strict</b> matching 
	 * (cf {@link Regulars#matches(Regular, String, boolean)}). Indeed projecting
	 * the Kleene star of a regexp that ends with EOF does not preserve the
	 * semantics unless it is allowed to match the empty string after EOF. 
	 * </i>
	 * 
	 * @param regular
	 * @param first
	 * @return the projection map of {@code regular} on the given character set
	 */
	public static Map<CSet, Regular> project(Regular regular, CSet first) {
		switch (regular.getKind()) {
		case EPSILON: {
			return Maps.empty();
		}
		case EOF: {
			final CSet inter = CSet.inter(first, CSet.EOF);
			if (inter.isEmpty()) return Maps.empty();
			return Maps.singleton(inter, Regular.EPSILON);
		}
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			final CSet inter = CSet.inter(first, characters.chars);
			if (inter.isEmpty()) return Maps.empty();
			return Maps.singleton(inter, Regular.EPSILON);
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			Map<CSet, Regular> alt1 = project(alternate.lhs, first);
			Map<CSet, Regular> alt2 = project(alternate.rhs, first);
			return merge(alt1, alt2);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			Map<CSet, Regular> seq1 = project(sequence.first, first);
			Map<CSet, Regular> seq12 = new HashMap<>();
			for (Map.Entry<CSet, Regular> e : seq1.entrySet())
				seq12.put(e.getKey(), Regular.seq(e.getValue(), sequence.second));
			if (!sequence.first.nullable) return seq12;
			Map<CSet, Regular> seq2 = project(sequence.second, first);
			return merge(seq12, seq2);
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			Map<CSet, Regular> rep1 = project(repetition.reg, first);
			if (rep1.isEmpty()) return Maps.empty();
			Map<CSet, Regular> repn = new HashMap<>();
			for (Map.Entry<CSet, Regular> e : rep1.entrySet())
				repn.put(e.getKey(), Regular.seq(e.getValue(), repetition));
			return repn;
		}
		case BINDING: {
			final Binding binding = (Binding) regular;
			return project(binding.reg, first);
		}
		}
		throw new IllegalStateException();
	}
}