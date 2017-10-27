package codegen;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
		private final static class Extent {
			final @Nullable String name;
			final LexBuffer.Position start;
			final LexBuffer.Position end;
			
			Extent(@Nullable String name, 
					LexBuffer.Position start, LexBuffer.Position end) {
				this.name = name;
				this.start = start;
				this.end = end;
			}
			
			@SuppressWarnings("null")
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
		
		private final LexBuffer lexbuf;
		private LexBuffer.Position _jl_lastTokenEnd;
		private Stack<@NonNull List<@NonNull Extent>> _jl_locationStack;
		
		protected WithPositions(LexBuffer lexbuf, Supplier<@NonNull Token> tokens) {
			super(tokens);
			this.lexbuf = lexbuf;
			this._jl_lastTokenEnd = new LexBuffer.Position(lexbuf.filename);
			this._jl_locationStack = new Stack<>();
		}

		private final static boolean withDebug = false;
		private final void print() {
			System.out.println(String.format("  lastToken=%s", Objects.toString(_jl_lastTokenEnd)));
			System.out.println(String.format("  stack=%s", _jl_locationStack));
			Prompt.getInputLine("Press to continue");
		}
		
		private int maxStack = 0;
		protected final void enter(int ruleSize) {	// ruleSize is not strictly necessary
			_jl_locationStack.push(new ArrayList<Extent>(ruleSize));
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
			LexBuffer.Position start = lexbuf.getLexemeStart();
			LexBuffer.Position end = lexbuf.getLexemeEnd();
			_jl_lastTokenEnd = end;
			_jl_locationStack.peek().add(
				new Extent(name, start, end));
			if (!withDebug) return;
			System.out.println(String.format("Shift (%s)",  Objects.toString(name)));
			print();
		}
		
		protected final void leave(@Nullable String name) {
			LexBuffer.Position start = getStartPos();
			LexBuffer.Position end = getEndPos();
			_jl_locationStack.pop();
			_jl_locationStack.peek().add(new Extent(name, start, end));
			if (!withDebug) return;
			System.out.println(String.format("Leave (%s)",  Objects.toString(name)));
			print();
		}
		
		private static ParsingException noRule(String method) {
			return new ParsingException("No current rule production. Are you using " 
					+ method + " outside of a semantic action?");
		}
		
		protected final LexBuffer.Position getStartPos() {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getStartPos");
			}
			if (locs.isEmpty())
				return _jl_lastTokenEnd;
			return locs.get(0).start;
		}
		
		protected final LexBuffer.Position getEndPos() {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getEndPos");
			}
			if (locs.isEmpty())
				return _jl_lastTokenEnd;
			return locs.get(locs.size() - 1).end;
		}
		
		protected final LexBuffer.Position getSymbolStartPos() {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getSymbolStartPos");
			}
			@Nullable Extent found = null;
			for (@NonNull Extent e : locs) {
				if (e.start.offset != e.end.offset) {
					found = e; break;
				}
			}
			if (found == null)
				return _jl_lastTokenEnd;
			return found.start;
		}

		private static ParsingException noActual(int i, int size) {
			return new ParsingException("Cannot find actual " + i + " in the current production, which has " + size);
		}
		
		protected final LexBuffer.Position getStartPos(int i) {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getStartPos(int)");
			}
			if (i <= 0 || i > locs.size())
				throw noActual(i, locs.size());
			return locs.get(i - 1).start;
		}
		
		protected final LexBuffer.Position getEndPos(int i) {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getEndPos(int)");
			}
			if (i <= 0 || i > locs.size())
				throw noActual(i, locs.size());
			return locs.get(i - 1).end;
		}
		
		private static ParsingException noBinding(String id) {
			return new ParsingException("Cannot find actual with name " + id + " in the current production");
		}
		
		protected final LexBuffer.Position getStartPos(String id) {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getStartPos(String)");
			}
			@Nullable Extent found = null;
			for (Extent e : locs) {
				if (id.equals(e.name)) {
					found = e; break;
				}
			}
			if (found == null)
				throw noBinding(id);
			return found.start;
		}
		
		protected final LexBuffer.Position getEndPos(String id) {
			List<@NonNull Extent> locs;
			try { locs = _jl_locationStack.peek(); }
			catch (EmptyStackException e) {
				throw noRule("getEndPos(String)");
			}
			@Nullable Extent found = null;
			for (Extent e : locs) {
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
