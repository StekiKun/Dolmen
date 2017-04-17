package test.examples;

import java.util.Arrays;
import java.util.Map;

import common.CSet;
import common.Lists;
import syntax.Lexer;
import syntax.Location;
import syntax.Regular;
import static test.examples.BasicLexers.test;

/**
 * More advanced lexer definitions, manually
 * constructed as examples for testing
 * 
 * @author Stéphane Lescuyer
 */
public abstract class AdvancedLexers {

	private AdvancedLexers() {
		// Static utility only
	}

	/**
	 * A lexer which handles arithmetic expressions
	 * with four operations, identifiers, decimal and
	 * hexadecimal literals, Java-like multiline 
	 * comments. It is implemented via two rules, a
	 * main one and one for comments:
	 * <pre>
	 * rule main:
	 * | ('\b'  | '\t' | ' ')*		{ main(lexbuf); }
	 * | ('\r''\n' | '\n')			{ newline(); main(lexbuf); } 
	 * | [_a-zA-Z][_a-zA-Z0-9]*		{ return IDENT; }
	 * | [0-9]+						{ return INT; }
	 * | "0x" [0-9a-fA-F]+			{ return HEX; }
	 * | "/*"       { comment(lexbuf); main(lexbuf); }
	 * | '+'		{ return PLUS; }
	 * | '-'        { return MINUS; }
	 * | '*'        { return MULT; }
	 * | '/'        { return DIV; }
	 * | '('        { return LPAREN; }
	 * | ')'        { return RPAREN; }
	 * | eof        { return EOF; }
	 * | _          { throw new LexicalError(); }
	 * 
	 * rule comment:
	 * | '*' '/'	{ return; }
	 * | '\n'       { newline(); comment(lexbuf); }
	 * | '*'        { comment(lexbuf); }
	 * | [^*\r\n]   { comment(lexbuf); }
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class ArithExprsComment {
		private final static CSet space =
			CSet.union(CSet.singleton(' '),
				CSet.union(CSet.singleton('\b'), CSet.singleton('\t')));
		private final static CSet idstart =
			CSet.union(
				CSet.union(BasicLexers.lalpha, BasicLexers.ualpha), 
				CSet.singleton('_'));
		private final static CSet idbody =
			CSet.union(BasicLexers.digit, idstart);
		private final static CSet hexdigit =
			CSet.union(BasicLexers.digit,
				CSet.union(CSet.interval('a', 'f'), CSet.interval('A', 'F')));
		
		private static final Regular ws =
			Regular.star(Regular.chars(space));
		private static final Regular newline =
			Regular.or(
				Regular.string("\r\n"),
				Regular.string("\n"));
		private static final Regular ident =
			Regular.seq(
				Regular.chars(idstart), 
				Regular.star(Regular.chars(idbody)));
		private static final Regular decimal =
			Regular.seq(
				Regular.chars(BasicLexers.digit), 
				Regular.star(Regular.chars(BasicLexers.digit)));
		private static final Regular hexadecimal =
			Regular.seq(Regular.string("0x"),
				Regular.star(Regular.chars(hexdigit)));
		
		private static final Map<Regular, Location> mainClauses =
			BasicLexers.clauses(
					ws,
					newline,
					ident,
					decimal,
					hexadecimal,
					Regular.string("/*"),
					Regular.string("+"),
					Regular.string("-"),
					Regular.string("*"),
					Regular.string("/"),
					Regular.string("("),
					Regular.string(")"),
					Regular.chars(CSet.EOF),
					Regular.chars(CSet.ALL));
		
		private static final CSet inComment =
			CSet.complement(
				CSet.union(CSet.singleton('\r'), CSet.singleton('\n')));
		private static final Map<Regular, Location> commentClauses =
			BasicLexers.clauses(
					Regular.string("*/"),
					Regular.string("*"),
					newline,
					Regular.chars(inComment));
		
		private final static Lexer.Entry mainEntry =
			new Lexer.Entry("main", false, Lists.empty(), mainClauses);
		private final static Lexer.Entry commentEntry =
			new Lexer.Entry("comment",  false, Lists.empty(), commentClauses);
			
		@SuppressWarnings("null")
		final static Lexer LEXER =
			new Lexer(Location.DUMMY,
				Arrays.asList(mainEntry, commentEntry), 
				Location.DUMMY);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test(ArithExprsComment.LEXER, true);
	}
	
}