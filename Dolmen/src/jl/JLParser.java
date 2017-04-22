package jl;

import static jl.JLToken.AS;
import static jl.JLToken.DOT;
import static jl.JLToken.END;
import static jl.JLToken.HASH;
import static jl.JLToken.IMPORT;
import static jl.JLToken.OR;
import static jl.JLToken.STATIC;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.CSet;
import common.Lists;
import common.Maps;
import common.Prompt;
import jl.JLToken.Action;
import jl.JLToken.Ident;
import jl.JLToken.Kind;
import jl.JLToken.LChar;
import jl.JLToken.LString;
import syntax.Lexer;
import syntax.Location;
import syntax.Regular;
import syntax.Regular.Characters;

/**
 * A manually written top-down parser for lexer descriptions,
 * fed by a stream of {@link JLToken}s as provided by the
 * generated lexer {@link JLLexerGenerated}.
 * 
 * @author Stéphane Lescuyer
 */
public class JLParser {

	/**
	 * Exception raised by parsing errors
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
	private static ParsingException error(JLToken token, Kind...expected) {
		StringBuilder buf = new StringBuilder();
		buf.append("Found token ").append(token);
		buf.append(", expected any of {");
		for (int i = 0; i < expected.length; ++i) {
			if (i != 0) buf.append(',');
			buf.append(expected[i]);
		}
		buf.append('}');
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return new ParsingException(res);
	}
	
	private final Iterator<JLToken> tokens;
	private @Nullable JLToken nextToken;
	
	// private final Stack<List<JLToken>> stack;
	
	/**
	 * Construct a new parser which will feed on the
	 * given iterator of tokens
	 * 
	 * @param tokens
	 */
	private JLParser(Iterator<JLToken> tokens) {
		this.tokens = tokens;
		// this.stack = new Stack<>();
		this.nextToken = null;
		this.definitions = Maps.empty();
	}
	
	/**
	 * 
	 * @param tokenizer	a supplier of tokens
	 * @param terminal	the special token that terminates the stream
	 * @return a parser based on the given token supplier and 
	 * 	terminated by {@code terminal}. It is guaranteed that 
	 * 	{@code tokenizer} will not be called after the first {@code terminal}
	 * 	token has been met.
	 */
	public static JLParser of(Supplier<JLToken> tokenizer, JLToken terminal) {
		return new JLParser(new Iterator<JLToken>() {
			@Nullable JLToken nextToken = tokenizer.get();
			
			@Override
			public boolean hasNext() {
				return nextToken != null;
			}

			@Override
			public @NonNull JLToken next() {
				JLToken tok = nextToken;
				if (tok == null)
					throw new NoSuchElementException();
				if (tok == terminal) nextToken = null;
				else nextToken = tokenizer.get();
				return tok;
			}
		});
	}

	private JLToken peek() {
		if (nextToken != null) return nextToken;
		if (!tokens.hasNext())
			throw new ParsingException("No more tokens!");
		nextToken = tokens.next();
		return nextToken;
	}
	
	private void eat() {
		peek(); nextToken = null;
	}
	
	private JLToken eat(JLToken.Kind kind) {
		JLToken ctoken = peek();
		if (kind != ctoken.getKind())
			throw error(ctoken, kind);
		nextToken = null;
		return ctoken;
	}
	
	/**
	 * <i>NB: One can technically parse several lexers in
	 * 	a row with one token stream.</i> 
	 * 
	 * @return a lexer definition parsed from the
	 * 	token stream that was given to this parsing object
	 */
	public Lexer parseLexer() {
		definitions = new HashMap<>();
		List<String> imports = parseImports();
		Action header = (Action) (eat(Kind.ACTION));
		return new Lexer(imports, header.value, Lists.empty(), Location.DUMMY);
	}
	
	private List<String> parseImports() {
		List<String> imports = new ArrayList<>(2);
		while (peek() == IMPORT)
			imports.add(parseImport());
		if (imports.isEmpty()) return Lists.empty();
		return imports;
	}
	
	private String parseImport() {
		eat(Kind.IMPORT);
		StringBuilder buf = new StringBuilder();
		buf.append("import ");
		if (peek() == STATIC) {
			eat(); buf.append("static ");
		}
		parseImportString(buf);
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}
	
	private void parseImportString(StringBuilder buf) {
		JLToken id = eat(Kind.IDENT);
		buf.append(((Ident) id).value);
		typename:
		while (peek() == DOT) {
			eat(); buf.append('.');
			JLToken ctoken = peek();
			switch (ctoken.getKind()) {
			case IDENT:
				buf.append(((Ident) id).value); eat(); break;
			case STAR:
				buf.append('*'); eat(); break typename;
			default:
				throw error(ctoken, Kind.IDENT, Kind.STAR);
			}
		}
		eat(Kind.SEMICOL);
	}
	
	/** Environment for regular expression definitions */
	private Map<String, Regular> definitions;

	/**
	 * Regular :=
	 * | Regular AS IDENT			(allows r as bla as foo)
	 * | AltRegular
	 * 
	 * AltRegular :=
	 * | SeqRegular OR AltRegular
	 * | SeqRegular
	 * 
	 * SeqRegular :=
	 * | PostfixRegular SeqRegular	(FIRST(SeqRegular) = FIRST(AtomicRegular) = 
	 * | PostfixRegular                UNDERSCORE EOF LCHAR LSTRING IDENT LBRACKET LPAREN)
	 * 
	 * PostfixRegular :=
	 * | DiffRegular STAR
	 * | DiffRegular PLUS
	 * | DiffRegular MAYBE
	 * | DiffRegular
	 * 
	 * DiffRegular :=
	 * | AtomicRegular HASH AtomicRegular	(only if char sets)
	 * | AtomicRegular	 
	 * 
	 * AtomicRegular :=
	 * 	 UNDERSCORE
	 * | EOF
	 * | LCHAR
	 * | LSTRING
	 * | IDENT
	 * | LBRACKET CSet RBRACKET
	 * | LPAREN Regular RPAREN
	 */
	
	protected Regular parseRegular() {
		/**
		 * Manual left-factoring of the Regular rule
		 * Regular := AltRegular (AS IDENT)*
		 */
		Regular res = parseAltRegular();
		while (peek() == AS) {
			eat();
			Ident id = (Ident) eat(Kind.IDENT);
			res = Regular.binding(res, id.value, Location.DUMMY);
		}
		return res;
	}
	
	private Regular parseAltRegular() {
		Regular r = parseSeqRegular();
		if (peek() == OR) {
			eat();
			Regular r2 = parseAltRegular();
			return Regular.or(r, r2);
		}
		return r;
	}
	
	private Regular parseSeqRegular() {
		Regular r = parsePostfixRegular();
		EnumSet<Kind> first =
			EnumSet.of(Kind.UNDERSCORE, Kind.EOF, Kind.LCHAR, Kind.LSTRING,
					Kind.IDENT, Kind.LBRACKET, Kind.LPAREN);
		if (first.contains(peek().getKind())) {
			Regular r2 = parseSeqRegular();
			return Regular.seq(r, r2);
		}
		return r;
	}
	
	private Regular parsePostfixRegular() {
		Regular r = parseDiffRegular();
		switch (peek().getKind()) {
		case STAR: 
			eat(); return Regular.star(r);
		case PLUS:
			eat(); return Regular.plus(r);
		case MAYBE:
			eat(); return Regular.or(Regular.EPSILON, r);
		default:
			return r;
		}
	}
	
	private Regular parseDiffRegular() {
		Regular r1 = parseAtomicRegular();
		if (peek() == HASH) {
			eat();
			Regular r2 = parseAtomicRegular();
			return Regular.chars(CSet.diff(asCSet(r1), asCSet(r2)));
		}
		return r1;
	}
	
	private CSet asCSet(Regular reg) {
		switch (reg.getKind()) {
		case EPSILON:
		case EOF:
		case ALTERNATE:
		case SEQUENCE:
		case REPETITION:
		case BINDING:
			throw new ParsingException
				("Regular expression " + reg + " is not a character set.");
		case CHARACTERS: {
			final Characters characters = (Characters) reg;
			return characters.chars;
		}
		}
		throw new IllegalStateException();
	}
	
	private Regular parseAtomicRegular() {
		switch (peek().getKind()) {
		case UNDERSCORE:
			eat();
			return Regular.chars(CSet.ALL_BUT_EOF);
		case EOF:
			eat();
			return Regular.chars(CSet.EOF);
		case LCHAR: {
			LChar tok = (LChar) eat(Kind.LCHAR);
			return Regular.chars(CSet.singleton(tok.value));
		}
		case LSTRING: {
			LString tok = (LString) eat(Kind.LSTRING);
			return Regular.string(tok.value);
		}
		case IDENT: {
			Ident tok = (Ident) eat(Kind.IDENT);
			@Nullable Regular reg = Maps.get(definitions, tok.value);
			if (reg == null)
				throw new ParsingException("Undefined regular expression " + tok.value);
			return reg;
		}
		case LBRACKET:
			return Regular.chars(parseCharClass());
		case LPAREN: {
			eat(Kind.LPAREN);
			Regular res = parseRegular();
			eat(Kind.RPAREN);
			return res;
		}
		default:
			throw error(peek(), Kind.UNDERSCORE, Kind.EOF, Kind.LCHAR, 
					Kind.LSTRING, Kind.IDENT, Kind.LBRACKET);
		}
	}
	
	private CSet parseCharClass() {
		eat(Kind.LBRACKET);
		CSet res = parseCharSet();
		eat(Kind.RBRACKET);
		return res;
	}
	
	private CSet parseCharSet() {
		if (peek() == JLToken.CARET) {
			eat();
			return CSet.complement(parsePositiveCharSet());
		}
		return parsePositiveCharSet();
	}
	
	private CSet parsePositiveCharSet() {
		CSet res;
		LChar ch1 = (LChar) eat(Kind.LCHAR);
		if (peek() == JLToken.DASH) {
			eat();
			LChar ch2 = (LChar) eat(Kind.LCHAR);
			res = CSet.interval(ch1.value, ch2.value); 
		}
		else
			res = CSet.singleton(ch1.value);
		if (peek().getKind() == Kind.LCHAR)
			return CSet.union(res, parsePositiveCharSet());
		return res;
	}
	
	/** Whether tokens should be printed along the way, for debug */
	private static boolean tokenize = true;

	private static JLParser of(JLLexerGenerated lexer) {
		return of(new Supplier<JLToken>() {
			@SuppressWarnings("null")
			@Override
			public JLToken get() {
				try {
					JLToken tok = lexer.main();
					if (tokenize)
						System.out.println(tok);
					return tok;
				} catch (IOException e) {
					e.printStackTrace();
					return END;
				}
			}
		}, END);
	}
	
	static void testParse(String filename) throws IOException {
		FileReader reader = new FileReader(filename);
		JLLexerGenerated lexer = new JLLexerGenerated(filename, reader);
		JLParser parser = of(lexer);
		Lexer lexerDef = parser.parseLexer();
		reader.close();
		System.out.println(lexerDef.toString());
	}
	
	static void testCharClass(String contents) throws IOException {
		Reader reader = new StringReader(contents);
		JLLexerGenerated lexer = new JLLexerGenerated("-", reader);
		JLParser parser = of(lexer);
		CSet cset = parser.parseCharClass();
		reader.close();
		System.out.println(cset.toString());
	}

	static void testRegular(String contents) throws IOException {
		Reader reader = new StringReader(contents);
		JLLexerGenerated lexer = new JLLexerGenerated("-", reader);
		JLParser parser = of(lexer);
		Regular reg = parser.parseRegular();
		reader.close();
		System.out.println(reg.toString());
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		testParse("tests/jl/test1.jl");
		String prompt;
		while ((prompt = Prompt.getInputLine(">")) != null) {
			try {
				testRegular(prompt);
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
	}
}