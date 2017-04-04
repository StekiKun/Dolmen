package syntax;

import java.util.HashSet;
import java.util.Set;

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
}
