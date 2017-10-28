package tagged;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import common.CSet;
import syntax.Lexer;
import syntax.Extent;

/**
 * A tagged lexer definition is the encoded
 * version of a {@link Lexer lexer definition},
 * where all {@link #entries entries} have been
 * encoded and optimised. It also contains the
 * {@link #charsets character set dictionary} for
 * the encoded regular expressions.
 * 
 * @author Stéphane Lescuyer
 * @see Encoder
 * @see Optimiser
 */
public final class TLexer {

	/** The imports to be added to the generated lexer */
	public List<@NonNull String> imports;
	/** The location of this lexer's header */
	public final Extent header;
	/** The list of encoded entries */
	public final List<@NonNull TLexerEntry> entries;
	/** The character set dictionary */
	public final List<@NonNull CSet> charsets;
	/** The location of this lexer's footer */
	public final Extent footer;
	
	/**
	 * Builds a tagged lexer definition from the
	 * given arguments
	 * 
	 * @param imports
	 * @param header
	 * @param entries
	 * @param charsets
	 * @param footer
	 */
	public TLexer(List<String> imports, Extent header,
			List<TLexerEntry> entries, List<CSet> charsets,
			Extent footer) {
		this.imports = imports;
		this.header = header;
		this.entries = entries;
		this.charsets = charsets;
		this.footer = footer;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		imports.forEach(imp -> System.out.println(imp));
		buf.append(header);
		buf.append("\nchar sets:");
		int i = 0;
		for (CSet cset : charsets) {
			buf.append("\n " + i).append(" -> ");
			buf.append(cset);
			++i;
		}
		for (TLexerEntry entry : entries) {
			buf.append("\n");
			buf.append(entry);
		}
		buf.append("\n").append(footer);
		return buf.toString();
	}

}
