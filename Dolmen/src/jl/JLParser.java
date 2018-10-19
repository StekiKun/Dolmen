package jl;

import static jl.JLToken.AS;
import static jl.JLToken.DOT;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import codegen.BaseParser;
import codegen.LexBuffer.LexicalError;
import codegen.LexBuffer.Position;
import common.CSet;
import common.Lists;
import common.Maps;
import common.Prompt;
import jl.JLToken.Action;
import jl.JLToken.INTEGER;
import jl.JLToken.Ident;
import jl.JLToken.Kind;
import jl.JLToken.LChar;
import jl.JLToken.LString;
import syntax.Lexer;
import syntax.Lexer.Clause;
import syntax.Located;
import syntax.Regular;
import syntax.Regular.Characters;
import syntax.Regulars;

/**
 * A manually written top-down parser for lexer descriptions,
 * fed by a stream of {@link JLToken}s as provided by the
 * generated lexer {@link JLLexerGenerated}.
 * 
 * @author Stéphane Lescuyer
 */
public final class JLParser extends BaseParser<JLToken> {

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
	public JLParser(JLLexerGenerated lexbuf, Function<JLLexerGenerated, @NonNull JLToken> tokens) {
		super(lexbuf, tokens);
		this.definitions = Maps.empty();
	}
	
	private JLToken eat(JLToken.Kind kind) {
		JLToken ctoken = eat();
		if (kind != ctoken.getKind())
			throw tokenError(ctoken, kind);
		return ctoken;
	}
	
	private <@NonNull T> Located<T> withLoc(T value) {
		return Located.of(value, _jl_lastTokenStart, _jl_lastTokenEnd);
	}
	
	/**
	 * Lexer :=
	 * 	Import* ACTION Definition* Entry+ ACTION END
	 * 
	 * @return a lexer definition parsed from the
	 * 	token stream that was given to this parsing object
	 * @throws LexicalError	if no token can be read using
	 * 	the supplier, either because the input does not
	 * 	correspond to a token, or because of a lower-level
	 * 	IO error
	 * @throws ParsingException if some unexpected token
	 * 	is encountered
	 */
	public Lexer parseLexer() {
		definitions = new LinkedHashMap<>();
		List<@NonNull Located<String>> imports = parseImports();
		Action header = (Action) (eat(Kind.ACTION));
		parseDefinitions();
		List<Lexer.Entry> entries = parseEntries();
		Action footer= (Action) (eat(Kind.ACTION));
		eat(Kind.END);
		Lexer.Builder builder =
			new Lexer.Builder(imports, header.value, definitions, footer.value);
		for (Lexer.Entry entry : entries)
			builder.addEntry(entry);
		return builder.build();
	}
	
	/**
	 * Import :=
	 * | IMPORT STATIC? TypeName SEMICOL
	 * 
	 * TypeName :=
	 * | IDENT
	 * | IDENT DOT TypeName
	 * | IDENT DOT STAR
	 */

	private List<@NonNull Located<String>> parseImports() {
		List<@NonNull Located<String>> imports = new ArrayList<>(2);
		while (peek() == IMPORT)
			imports.add(parseImport());
		if (imports.isEmpty()) return Lists.empty();
		return imports;
	}
	
	private Located<String> parseImport() {
		eat(Kind.IMPORT);
		Position posStart = _jl_lastTokenStart;
		StringBuilder buf = new StringBuilder();
		buf.append("import ");
		if (peek() == STATIC) {
			eat(); buf.append("static ");
		}
		parseImportString(buf);
		return Located.of(buf.toString(), posStart, _jl_lastTokenEnd);
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
				buf.append(((Ident) ctoken).value); eat(); break;
			case STAR:
				buf.append('*'); eat(); break typename;
			default:
				throw tokenError(ctoken, Kind.IDENT, Kind.STAR);
			}
		}
		eat(Kind.SEMICOL);
		buf.append(';');
	}
	
	/** Environment for regular expression definitions */
	private Map<Located<String>, Regular> definitions;

	/**
	 * <i>NB: Ending semi-colon is necessary to avoid lookahead of
	 * 	2 to distinguish new definition from concatenation with 
	 *  an ident</i>
	 * 
	 * Definitions :=
	 * 	Definition*
	 * 
	 * Definition :=
	 * 	IDENT EQUAL Regular SEMICOL
	 */
	private void parseDefinitions() {
		while (peek().getKind() == Kind.IDENT) {
			Ident id = (Ident) eat(Kind.IDENT);
			Located<String> lid = withLoc(id.value);
			eat(Kind.EQUAL);
			Regular reg = parseRegular();
			eat(Kind.SEMICOL);
			definitions.put(lid, reg);
		}
	}
	
	/**
	 * Entries :=
	 * | Entry Entries
	 * | Entry
	 *  
	 * Entry :=
	 * | Visibility ACTION RULE IDENT ACTION?
	 * 		EQUAL SHORTEST? Clauses
	 * 
	 * Visibility :=
	 * | PUBLIC | PRIVATE
	 */
	
	private List<Lexer.Entry> parseEntries() {
		List<Lexer.Entry> entries = new ArrayList<>();
		// At least one entry
		entries.add(parseEntry());
		while (peek().getKind() != Kind.ACTION) {
			entries.add(parseEntry());
		}
		return entries;
	}
	
	private Lexer.Entry parseEntry() {
		boolean vis = parseVisibility();
		Action returnType = (Action) eat(Kind.ACTION);
		
		eat(Kind.RULE);
		
		Ident name = (Ident) eat(Kind.IDENT);
		Located<String> lname = Located.of(name.value,
			_jl_lexbuf.getLexemeStart(), _jl_lexbuf.getLexemeEnd());
		
		@Nullable Action args = null;
		if (peek().getKind() == Kind.ACTION) {
			args = (Action) eat(Kind.ACTION);
		}
		
		eat(Kind.EQUAL);
		boolean shortest = false;
		if (peek().getKind() == Kind.SHORTEST) {
			eat();
			shortest = true;
		}
		
		List<Clause> clauses = parseClauses();
		
		return new Lexer.Entry(vis, lname, returnType.value, shortest, 
				args == null ? null : args.value, clauses);
	}

	private boolean parseVisibility() {
		switch (peek().getKind()) {
		case PUBLIC:
			eat(); return true;
		case PRIVATE:
			eat(); return false;
		default:
			throw tokenError(peek(), Kind.PUBLIC, Kind.PRIVATE);
		}
	}
	
	/**
	 * Clauses :=
	 * 	Clause Clause*
	 * 
	 * Clause :=
	 * 	OR Regular ACTION
	 */
	
	private List<Clause> parseClauses() {
		List<Clause> clauses = new ArrayList<>();
		parseClause(clauses);
		while (peek().getKind() == Kind.OR)
			parseClause(clauses);
		return clauses;
	}
	
	private void parseClause(List<Clause> acc) {
		eat(Kind.OR);
		Located<Regular> lreg;
		if (peek().getKind() == Kind.ORELSE) {
			eat(Kind.ORELSE);
			// Deal with the special default clause by finding
			// all possible first characters matched by other
			// clauses
			CSet possible = CSet.EMPTY;
			for (Clause cl : acc)
				possible = CSet.union(possible, Regulars.first(cl.regular.val));
			CSet others = CSet.complement(possible);
			Regular reg = Regular.plus(Regular.chars(others));
			lreg = withLoc(reg);	// location of 'orelse'
		}
		else {
			Position start = _jl_lastTokenEnd;
			Regular reg = parseRegular();
			lreg = Located.of(reg, start, _jl_lastTokenEnd);
		}
		Action action = (Action) eat(Kind.ACTION);
		acc.add(new Clause(lreg, action.value));
	}
	
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
	 * | DiffRegular LANGLE INTEGER Repetitions
	 * | DiffRegular
	 * 
	 * Repetitions :=
	 * | RANGLE
	 * | COMMA INTEGER RANGLE
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
	
	private Regular parseRegular() {
		/**
		 * Manual left-factoring of the Regular rule
		 * Regular := AltRegular (AS IDENT)*
		 */
		Regular res = parseAltRegular();
		while (peek() == AS) {
			eat();
			Ident id = (Ident) eat(Kind.IDENT);
			res = Regular.binding(res, withLoc(id.value));
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
		case LANGLE:
			eat();
			INTEGER tok = (INTEGER) eat(Kind.INTEGER);
			if (tok.value < 0)
				throw parsingError("Invalid repetition count " + tok.value);
			return parseRepetitions(r, tok.value);
		default:
			return r;
		}
	}
	
	private Regular parseRepetitions(Regular r, int min) {
		switch (peek().getKind()) {
		case RANGLE:
			eat(); return Regular.repeat(r, min);
		case COMMA:
			eat();
			INTEGER tok = (INTEGER) eat(Kind.INTEGER);
			if (tok.value < 0)
				throw parsingError("Invalid repetition count " + tok.value);
			if (tok.value < min)
				throw parsingError("Maximum repetition " 
						+ tok.value + " is strictly smaller than minimum " + min);
			eat(Kind.RANGLE);
			return Regular.repeat(r, min, tok.value);
		default:
			throw tokenError(peek(), Kind.RANGLE, Kind.COMMA);
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
			throw parsingError("Regular expression " + reg + " is not a character set.");
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
			@Nullable Regular reg = Maps.get(definitions, Located.dummy(tok.value));
			if (reg == null)
				throw parsingError("Undefined regular expression " + tok.value);
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
			throw tokenError(peek(), Kind.UNDERSCORE, Kind.EOF, Kind.LCHAR, 
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
		if (!tokenize)
			return new JLParser(lexer, JLLexerGenerated::main);
		else
			return new JLParser(lexer, lexbuf -> {
					JLToken res = lexbuf.main();
					System.out.println(res);
					return res;
				});
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