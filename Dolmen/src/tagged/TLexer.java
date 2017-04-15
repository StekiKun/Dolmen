package tagged;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import common.CSet;
import syntax.Lexer;
import syntax.Location;

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

	/** The location of this lexer's header */
	public final Location header;
	/** The list of encoded entries */
	public final List<@NonNull TLexerEntry> entries;
	/** The character set dictionary */
	public final List<@NonNull CSet> charsets;
	/** The location of this lexer's footer */
	public final Location footer;
	
	/**
	 * Builds a tagged lexer definition from the
	 * given arguments
	 * 
	 * @param header
	 * @param entries
	 * @param charsets
	 * @param footer
	 */
	public TLexer(Location header,
			List<TLexerEntry> entries, List<CSet> charsets,
			Location footer) {
		this.header = header;
		this.entries = entries;
		this.charsets = charsets;
		this.footer = footer;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(header).append("\n");
		for (TLexerEntry entry : entries) {
			buf.append("\n");
			buf.append(entry);
		}
		buf.append("\n").append(footer);
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}

}
