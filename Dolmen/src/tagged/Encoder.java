package tagged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import common.CSet;
import syntax.Regular;
import syntax.Regular.Alternate;
import syntax.Regular.Binding;
import syntax.Regular.Characters;
import syntax.Regular.Repetition;
import syntax.Regular.Sequence;
import syntax.Regulars;

/**
 * An instance of {@link Encoder} can be used
 * to encode syntactic {@link Regular regular expressions}
 * in tagged {@link TRegular regular expressions}.
 * <p>
 * All regular expressions encoded within the same instance
 * share a common pool of character sets, which can be
 * retrieved via {@link #getCharacterSets()}.
 * 
 * @author Stéphane Lescuyer
 */
public final class Encoder {

	private int nextIndex;
	private final List<CSet> charSets;

	/**
	 * Returns a freshly initialized encoder 
	 */
	public Encoder() {
		this.nextIndex = 0;
		this.charSets = new ArrayList<CSet>();
	}

	/**
	 * @return an unmodifiable view of the character
	 * 	sets recorded by this encoder, suitable
	 * 	for interpreting character set indices in
	 * 	tagged regular expressions encoded by {@code this}
	 */
	@SuppressWarnings("null")
	public List<CSet> getCharacterSets() {
		return Collections.unmodifiableList(charSets);
	}
	
	private int getCharSet(CSet cset) {
		int i = 0;
		for (CSet cs : charSets) {
			if (CSet.equivalent(cset, cs)) return i;
			++i;
		}
		charSets.add(cset);
		return nextIndex++;
	}
	
	/**
	 * <b>The regular expression in input must not contain
	 * 	nested bindings with the same name, or the returned
	 *  tagged regular expression will not behave correctly.</b>
	 * 
	 * @param regular	the regular expression to encode
	 * @param charVars	bound variables guaranteed to be matching single characters
	 * @param action	the semantic action associated to this regular expression
	 * @return a tagged regular expression corresponding to
	 * 	the given regular expression {@code regular}. Character
	 * 	sets in the resulting expression are encoded in this
	 * 	encoder
	 * @see #getCharacterSets()
	 */
	private TRegular encode_(Regular regular, Set<String> charVars, int action) {
		switch (regular.getKind()) {
		case EPSILON:
			return TRegular.EPSILON;
		case EOF: {
			int eof = getCharSet(CSet.EOF); 
			return TRegular.chars(eof, true);
		}
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			int idx = getCharSet(characters.chars);
			return TRegular.chars(idx, false);
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			return TRegular.or(encode_(alternate.lhs, charVars, action),
							   encode_(alternate.rhs, charVars, action));
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			return TRegular.seq(encode_(sequence.first, charVars, action),
								encode_(sequence.second, charVars, action));
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			return TRegular.star(encode_(repetition.reg, charVars, action));
		}
		case BINDING: {
			final Binding binding = (Binding) regular;
			TRegular tr = encode_(binding.reg, charVars, action);
			TRegular tstart = TRegular.tag(binding.name, true, action);
			if (charVars.contains(binding.name))
				return TRegular.seq(tstart, tr);
			else {
				TRegular tend = TRegular.tag(binding.name, false, action);
				return TRegular.seq(tstart, TRegular.seq(tr, tend));
			}
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * @param regular	the regular expression to encode
	 * @param charVars	bound variables guaranteed to be matching single characters
	 * @param action	the semantic action associated to this regular expression
	 * @return a tagged regular expression corresponding to
	 * 	the given regular expression {@code regular}. Character
	 * 	sets in the resulting expression are encoded in this
	 * 	encoder
	 * @see #getCharacterSets()
	 */
	public TRegular encode(Regular regular, Set<String> charVars, int action) {
		return encode_(Regulars.removeNestedBindings2(regular), charVars, action);
	}
}
