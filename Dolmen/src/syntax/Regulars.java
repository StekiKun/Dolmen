package syntax;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import common.CSet;
import common.Sets;
import syntax.Regular.Alternate;
import syntax.Regular.Binding;
import syntax.Regular.Characters;
import syntax.Regular.Repetition;
import syntax.Regular.Sequence;

/**
 * This class contains various utility methods 
 * about {@link Regular regular expressions}.
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
					&& binding1.loc.equals(binding2.loc)
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
			if (toRemove.contains(binding.name))
				return binding.reg.fold(this);
			else {
				toRemove.add(binding.name);
				Regular newreg = binding.reg.fold(this);
				toRemove.remove(binding.name);
				if (newreg == binding.reg) return binding;
				return Regular.binding(newreg, binding.name, binding.loc);
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
			if (toRemove.contains(binding.name))
				return rnbAux(binding.reg, toRemove);
			else {
				toRemove.add(binding.name);
				Regular newreg = rnbAux(binding.reg, toRemove);
				toRemove.remove(binding.name);
				if (newreg == binding.reg) return binding;
				return Regular.binding(newreg, binding.name, binding.loc);
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
		public final Set<String> allVars;
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
	}
	/**
	 * A special instance of {@link VarsInfo} for regular expressions
	 * without any bindings
	 */
	@SuppressWarnings("null")
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
			if (info.allVars.equals(info.chrVars)
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
				Sets.add(binding.name, info.allVars),
				info.optVars,
				info.allVars.contains(binding.name) ?
					Sets.add(binding.name, info.dblVars) :
					info.dblVars,
				binding.reg.size == 1 ?
					Sets.add(binding.name, info.chrVars) :
					info.chrVars,
				binding.reg.size == 1 ?
					info.strVars :
					Sets.add(binding.name, info.strVars));
		}
		}
		throw new IllegalStateException();
	}
}