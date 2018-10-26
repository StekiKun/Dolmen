package codegen;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import codegen.LexBuffer.Position;
import common.Prompt;

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
	 * @author Stéphane Lescuyer
	 */
	public static class ParsingException extends RuntimeException {
		private static final long serialVersionUID = 1L;

        /** The position in input at which the error occurred */
        public final @Nullable Position pos;
        
        /** The length of the part of input at which the error occurred */
        public final int length;
		
		/**
		 * A parsing error exception with the given error message,
		 * optionally specifying the position in the input stream
		 * where the error occurred
		 * @param pos
		 * @param msg
		 */
		public ParsingException(@Nullable Position pos, String msg) {
            super(msg + (pos == null ? "" : 
            	String.format(" (at line %d, column %d)", pos.line, pos.column())));
			this.pos = pos;
			this.length = 0;
		}
		
		/**
		 * A parsing error exception with the given error message,
		 * specifying the position and length of the part in the input
		 * stream where the error occurred
		 * @param pos
		 * @param length
		 * @param msg
		 */
		public ParsingException(Position pos, int length, String msg) {
            super(msg + (pos == null ? "" : 
            	String.format(" (at line %d, column %d)", pos.line, pos.column())));
			this.pos = pos;
			this.length = length;
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
	protected ParsingException tokenError(Object token, Object...expectedKinds) {
		StringBuilder buf = new StringBuilder();
		buf.append("Found token ").append(token);
		buf.append(", expected any of {");
		for (int i = 0; i < expectedKinds.length; ++i) {
			if (i != 0) buf.append(',');
			buf.append(expectedKinds[i]);
		}
		buf.append('}');
		@NonNull String res = buf.toString();
		// The position is that of the peeked token
		Position start = _jl_lexbuf.getLexemeStart();
		int length = _jl_lexbuf.getLexemeEnd().offset - start.offset;
		return new ParsingException(start, length, res);
	}
	
    /**
     * Convenience helper which returns a {@link ParsingException}
     * located at the last token consumed by the parser.
     * 
     * @param msg
     * @return the exception with the given message and the current
     * 	parsing position
     */
    protected ParsingException parsingError(String msg) {
    	return new ParsingException(_jl_lastTokenStart,
    		_jl_lastTokenEnd.offset - _jl_lastTokenStart.offset, msg);
    }
	
	/** The underlying lexing buffer */
	protected final LexBuffer _jl_lexbuf;
	
	/** The actual tokenizer */
	private final Supplier<@NonNull Token> _jl_tokens;
	
	/** 
	 * The last token read and not yet consumed, or {@code null}
	 * if the next token must be fetched from {@link #_jl_tokens}
	 */
	protected @Nullable Token _jl_nextToken;

	/**
	 * The start position of the last token that was consumed by
	 * the parser, or the start-of-input position if no token
	 * was consumed yet
	 */
	protected LexBuffer.Position _jl_lastTokenStart;

	/**
	 * The end position of the last token that was consumed by
	 * the parser, or the start-of-input position if no token
	 * was consumed yet
	 */
	protected LexBuffer.Position _jl_lastTokenEnd;
	
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
	protected <T extends LexBuffer> 
		BaseParser(T lexbuf, Function<T, @NonNull Token> tokens) {
		this._jl_lexbuf = lexbuf;
		this._jl_tokens = () -> tokens.apply(lexbuf);
		this._jl_nextToken = null;
		this._jl_lastTokenStart = new Position(lexbuf.filename);
		this._jl_lastTokenEnd = new Position(lexbuf.filename);
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
	 * Consumes the next token and returns it
	 */
	protected final Token eat() {
		Token t = peek(); _jl_nextToken = null;
		_jl_lastTokenStart = _jl_lexbuf.getLexemeStart();
		_jl_lastTokenEnd = _jl_lexbuf.getLexemeEnd();
		return t;
	}
	
	/**
	 * TODO doc
	 * 
	 * @author Stéphane Lescuyer
	 */
	public abstract static class WithPositions<Token> extends BaseParser<Token> {

		/**
		 * Represents a range in a source stream being parsed, corresponding
		 * to a (terminal or non-terminal) symbol of the grammar. The range
		 * is described by a pair of {@link #start} and {@link #end} positions.
		 * If the symbol is bound to some identifier in the grammar, it is
		 * recorded in {@link #name}.
		 * 
		 * @author Stéphane Lescuyer
		 */
		private final static class Range {
			final @Nullable String name;
			final LexBuffer.Position start;
			final LexBuffer.Position end;
			
			Range(@Nullable String name, 
					LexBuffer.Position start, LexBuffer.Position end) {
				this.name = name;
				this.start = start;
				this.end = end;
			}
			
			@Override
			public String toString() {
				StringBuilder buf = new StringBuilder();
				buf.append("[");
				if (name != null)
					buf.append("name=").append(name).append(", ");
				buf.append("start=").append(start).append(", ");
				buf.append("end=").append(end).append("]");
				return buf.toString();
			}
		}
		
		private Stack<@NonNull List<@NonNull Range>> _jl_locationStack;
		
		protected <T extends LexBuffer> 
			WithPositions(T lexbuf, Function<T, @NonNull Token> tokens) {
			super(lexbuf, tokens);
			this._jl_locationStack = new Stack<>();
		}

		private final static boolean withDebug = false;
		private final void print() {
			System.out.println(String.format("  lastToken=%s", Objects.toString(_jl_lastTokenEnd)));
			System.out.println(String.format("  stack=%s", _jl_locationStack));
			Prompt.getInputLine("Press to continue");
		}
		
		private int maxStack = 0;
		private int maxWidth = 0;
		protected final void enter(int ruleSize) {	// ruleSize is not strictly necessary
			_jl_locationStack.push(new ArrayList<Range>(ruleSize));
			if (!withDebug) return;
			int sz = _jl_locationStack.size();
			if (sz > maxStack) {
				maxStack = sz;
				System.out.println("Stack reached depth " + sz);
			}
			System.out.println(String.format("Entering %d",  ruleSize));
			print();
		}
		
		protected final void shift(@Nullable String name) {
			LexBuffer.Position start = _jl_lexbuf.getLexemeStart();
			LexBuffer.Position end = _jl_lexbuf.getLexemeEnd();
			_jl_locationStack.peek().add(new Range(name, start, end));
			if (!withDebug) return;
			int n = _jl_locationStack.peek().size();
			if (n > maxWidth) {
				maxWidth = n;
				System.out.println("Stack element reached width " + n);
			}
			System.out.println(String.format("Shift (%s)",  Objects.toString(name)));
			print();
		}
		
		protected final void leave(@Nullable String name) {
			LexBuffer.Position start = getStartPos();
			LexBuffer.Position end = getEndPos();
			_jl_locationStack.pop();
			_jl_locationStack.peek().add(new Range(name, start, end));
			if (!withDebug) return;
			int n = _jl_locationStack.peek().size();
			if (n > maxWidth) {
				maxWidth = n;
				System.out.println("Stack element reached width " + n);
			}
			System.out.println(String.format("Leave (%s)",  Objects.toString(name)));
			print();
		}
		
		protected final void rewind() {
			_jl_locationStack.peek().clear();
			if (!withDebug) return;
			System.out.println("Rewind");
			print();
		}
		
		private ParsingException noRule(String method) {
			return new ParsingException(getEndPos(), 
					"No current rule production. Are you using " 
					+ method + " outside of a semantic action?");
		}
		
		protected final LexBuffer.Position getStartPos() {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getStartPos");
			}
			if (locs.isEmpty())
				return _jl_lastTokenEnd;
			return locs.get(0).start;
		}
		
		protected final LexBuffer.Position getEndPos() {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getEndPos");
			}
			if (locs.isEmpty())
				return _jl_lastTokenEnd;
			return locs.get(locs.size() - 1).end;
		}
		
		protected final LexBuffer.Position getSymbolStartPos() {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getSymbolStartPos");
			}
			@Nullable Range found = null;
			for (@NonNull Range e : locs) {
				if (e.start.offset != e.end.offset) {
					found = e; break;
				}
			}
			if (found == null)
				return _jl_lastTokenEnd;
			return found.start;
		}

		private ParsingException noActual(int i, int size) {
			return new ParsingException(getEndPos(),
				"Cannot find actual " + i + " in the current production, which has " + size);
		}
		
		protected final LexBuffer.Position getStartPos(int i) {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getStartPos(int)");
			}
			if (i <= 0 || i > locs.size())
				throw noActual(i, locs.size());
			return locs.get(i - 1).start;
		}
		
		protected final LexBuffer.Position getEndPos(int i) {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getEndPos(int)");
			}
			if (i <= 0 || i > locs.size())
				throw noActual(i, locs.size());
			return locs.get(i - 1).end;
		}
		
		private ParsingException noBinding(String id) {
			return new ParsingException(getEndPos(), 
				"Cannot find actual with name " + id + " in the current production");
		}
		
		protected final LexBuffer.Position getStartPos(String id) {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getStartPos(String)");
			}
			@Nullable Range found = null;
			for (Range e : locs) {
				if (id.equals(e.name)) {
					found = e; break;
				}
			}
			if (found == null)
				throw noBinding(id);
			return found.start;
		}
		
		protected final LexBuffer.Position getEndPos(String id) {
			List<@NonNull Range> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getEndPos(String)");
			}
			@Nullable Range found = null;
			for (Range e : locs) {
				if (id.equals(e.name)) {
					found = e; break;
				}
			}
			if (found == null)
				throw noBinding(id);
			return found.end;
		}
	}
	
}
