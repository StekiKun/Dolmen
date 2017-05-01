package codegen;

import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Base class for generated parsers.
 * <p>
 * Generated parsers extend this class to inherit
 * the tokenizer mechanism, as well as the parsing exceptions
 * utilities.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class BaseParser<Token> {

	/**
	 * Exception raised by parsing errors in generated parsers
	 * 
	 * @author Stéphane Lescuyers
	 */
	public static class ParsingException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/**
		 * A parsing error exception with the given error message
		 * @param s
		 */
		public ParsingException(String s) {
			super(s);
		}
	}
	
	/**
	 * Convenience function to build a {@link ParsingException} when 
	 * encountering some {@code token} which does not correspond to the
	 * set of expected token kinds
	 * 
	 * @param token
	 * @param expectedKinds
	 * @return an exception with a suitable error message, ready to be thrown
	 */
	protected static ParsingException tokenError(Object token, Object...expectedKinds) {
		StringBuilder buf = new StringBuilder();
		buf.append("Found token ").append(token);
		buf.append(", expected any of {");
		for (int i = 0; i < expectedKinds.length; ++i) {
			if (i != 0) buf.append(',');
			buf.append(expectedKinds[i]);
		}
		buf.append('}');
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return new ParsingException(res);
	}
	
	/** The tokenizer */
	private final Supplier<@NonNull Token> _jl_tokens;
	/** 
	 * The last token read and not yet consumed, or {@code null}
	 * if the next token must be fetched from {@link #_jl_tokens}
	 */
	protected @Nullable Token _jl_nextToken;
	
	/**
	 * Construct a new parser which will feed on the
	 * given tokenizer. The parser is responsible for
	 * not calling the supplier unless at least one more
	 * token must be consumed:
	 * <ul>
	 * <li> the same supplier can be used to parse several
	 * 	top-level entries if applicable;
	 * <li> it is down to the parser to stop asking for
	 * 	tokens once end-of-input has been reached (which in
	 * 	turn is usually down to the lexer to generate one special
	 * 	token for end-of-input).
	 * </ul>
	 * 
	 * @param tokens
	 */
	protected BaseParser(Supplier<@NonNull Token> tokens) {
		this._jl_tokens = tokens;
		this._jl_nextToken = null;
	}
	
	/**
	 * @return the next token to consume, without consuming it
	 */
	protected final @NonNull Token peek() {
		if (_jl_nextToken != null) return _jl_nextToken;
		_jl_nextToken = _jl_tokens.get();
		return _jl_nextToken;
	}
	
	/**
	 * Consumes the next token
	 */
	protected final void eat() {
		peek(); _jl_nextToken = null;
	}
	
}
