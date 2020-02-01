package org.stekikun.dolmen.tagged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.syntax.Lexer;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.syntax.Regulars;
import org.stekikun.dolmen.syntax.Lexer.Clause;
import org.stekikun.dolmen.syntax.Regular.Alternate;
import org.stekikun.dolmen.syntax.Regular.Binding;
import org.stekikun.dolmen.syntax.Regular.Characters;
import org.stekikun.dolmen.syntax.Regular.Repetition;
import org.stekikun.dolmen.syntax.Regular.Sequence;
import org.stekikun.dolmen.syntax.Regulars.VarsInfo;
import org.stekikun.dolmen.tagged.Optimiser.Allocated;
import org.stekikun.dolmen.tagged.TLexerEntry.Finisher;

/**
 * An instance of {@link Encoder} can be used
 * to encode syntactic {@linkplain Regular regular expressions}
 * in tagged {@linkplain TRegular regular expressions}.
 * <p>
 * All regular expressions encoded within the same instance
 * share a common pool of character sets, which can be
 * retrieved via {@link #getCharacterSets()}.
 * 
 * @author Stéphane Lescuyer
 */
public final class Encoder {

	/** 
	 * Whether tagged regexps should also be optimised
	 * when tags positions can be statically determined
	 * with respect to the start or end of the input	
	 */
	private boolean optimisation;
	
	private int nextIndex;
	private final List<CSet> charSets;

	/**
	 * @param optimisation	whether optimisation should be applied
	 * Returns a freshly initialized encoder 
	 */
	public Encoder(boolean optimisation) {
		this.optimisation = optimisation;
		this.nextIndex = 0;
		this.charSets = new ArrayList<CSet>();
	}

	/**
	 * @return an unmodifiable view of the character
	 * 	sets recorded by this encoder, suitable
	 * 	for interpreting character set indices in
	 * 	tagged regular expressions encoded by {@code this}
	 */
	public List<CSet> getCharacterSets() {
		return Collections.unmodifiableList(charSets);
	}
	
	private int getCharSet(CSet cset) {
		// No more sharing here!
//		int i = 0;
//		for (CSet cs : charSets) {
//			if (CSet.equivalent(cset, cs)) return i;
//			++i;
//		}
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
			TRegular tstart = TRegular.tag(binding.name.val, true, action);
			if (charVars.contains(binding.name.val))
				return TRegular.seq(tstart, tr);
			else {
				TRegular tend = TRegular.tag(binding.name.val, false, action);
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

	private TLexerEntry encodeEntry(Lexer.Entry entry) {
		// Start with empty reg exp, empty actions, no tags
		TRegular tr = TRegular.EPSILON;
		List<Finisher> actions = new ArrayList<>(entry.clauses.size());
		int count = 0;
		int ntags = 0;
		// Go through all clauses and encode them, building a giant
		// disjunction in tr
		for (Clause clause : entry.clauses) {
			final Regular expr = Regulars.removeNestedBindings2(clause.regular.val);
			final Extent act = clause.action;
			final VarsInfo varsInfo = Regulars.analyseVars(expr);
			final Set<String> charVars = varsInfo.getCharVars();
			
			final TRegular texpr = encode_(expr, charVars, count);
			final Allocated allocated =
				Optimiser.optimise(varsInfo, optimisation, texpr);
			
			TRegular rclause =
				TRegular.seq(allocated.regular, TRegular.action(count));
			if (count == 0)
				tr = rclause;
			else
				tr = TRegular.or(tr, rclause);
					
			actions.add(new Finisher(count, allocated.identInfos, act));
			++count;
			if (ntags < allocated.numCells)
				ntags = allocated.numCells;
		}
		
		return new TLexerEntry(entry.visibility, entry.name.val, 
				entry.returnType, entry.shortest, entry.args,
				tr, ntags, actions);
	}
	
	/**
	 * @param optimisation	whether optimisation should be applied
	 * @param lexer
	 * @return a tagged lexer definition from {@code lexer}
	 * @see TLexer
	 */
	public static TLexer encodeLexer(Lexer lexer, boolean optimisation) {
		Encoder encoder = new Encoder(optimisation);
		List<TLexerEntry> entries = new ArrayList<>(lexer.entryPoints.size());
		for (Lexer.Entry entry : lexer.entryPoints)
			entries.add(encoder.encodeEntry(entry));
		// No need to copy charsets defensively since
		// we are done with this encoder
		return new TLexer(lexer.imports, lexer.header, 
			entries, encoder.getCharacterSets(), lexer.footer);
	}

}