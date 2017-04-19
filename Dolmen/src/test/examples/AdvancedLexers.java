package test.examples;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import common.CSet;
import common.Lists;
import syntax.Lexer;
import syntax.Location;
import syntax.Regular;
import static test.examples.BasicLexers.test;
import static test.examples.BasicLexers.testOutput;

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
	 * @param objs	clauses and inlined contents, interleaved
	 * @return a list of ordered clauses with inlined locations
	 */
	static Map<Regular, Location> inlinedClauses(@NonNull Object... objs) {
		if (objs.length % 2 != 0) throw new IllegalArgumentException();
		int n = objs.length / 2;
		Map<Regular, Location> res = new LinkedHashMap<>(n);
		for (int i = 0; i < n; ++i) {
			int k = 2 * i;
			Object reg = objs[k];
			Object msg = objs[k+1];
			if (!(reg instanceof Regular)
				|| !(msg instanceof String))
				throw new IllegalArgumentException();
			res.put((Regular) reg, Location.inlined((String) msg));
		}
		return res;
	}

	
	/**
	 * A lexer which handles arithmetic expressions
	 * with four operations, identifiers, decimal and
	 * hexadecimal literals, Java-like multiline 
	 * comments. It is implemented via two rules, a
	 * main one and one for comments:
	 * <pre>
	 * rule main:
	 * | ('\b'  | '\t' | ' ')*		{ return main(); }
	 * | ('\r''\n' | '\n')			{ newline(); return main(); } 
	 * | [_a-zA-Z][_a-zA-Z0-9]*		{ return IDENT; }
	 * | [0-9]+						{ return INT; }
	 * | "0x" [0-9a-fA-F]+			{ return HEX; }
	 * | "/*"       { comment(); return main(); }
	 * | '+'		{ return PLUS; }
	 * | '-'        { return MINUS; }
	 * | '*'        { return MULT; }
	 * | '/'        { return DIV; }
	 * | '('        { return LPAREN; }
	 * | ')'        { return RPAREN; }
	 * | eof        { return EOF; }
	 * | _          { throw new LexicalError("Unexpected char"); }
	 * 
	 * rule comment:
	 * | '*' '/'	{ return; }
	 * | '\n'       { newline(); comment(); return; }
	 * | '*'        { comment(); return; }
	 * | [^*\r\n]   { comment(); return; }
	 * | eof		{ throw new LexicalError("EOF in comment"); }
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
			inlinedClauses(
				ws,						" return main(); ",
				newline,				" newline(); return main(); ",
				ident,					" return IDENT; ",
				decimal,				" return INT; ",
				hexadecimal,			" return HEX; ",
				Regular.string("/*"),	" comment(); return main(); ",
				Regular.string("+"),	" return PLUS; ",
				Regular.string("-"),	" return MINUS; ",
				Regular.string("*"),	" return MULT; ",
				Regular.string("/"),	" return DIV; ",
				Regular.string("("),	" return LPAREN; ",
				Regular.string(")"),	" return RPAREN; ",
				Regular.chars(CSet.EOF),	" return EOF; ",
				Regular.chars(CSet.ALL),	" throw new LexicalError(\"Unexpected char\"); "
			);
		
		private static final CSet inComment =
			CSet.complement(
				CSet.union(CSet.singleton('\r'), CSet.singleton('\n')));
		private static final Map<Regular, Location> commentClauses =
			inlinedClauses(
				Regular.string("*/"),		" return; ",
				newline,		 			" newline(); comment(); return;",
				Regular.string("*"), 		" comment(); return;",
				Regular.chars(inComment),   " comment(); return;",
				Regular.chars(CSet.EOF),    " throw new LexicalError(\"EOF in comment\"); "
			);
		private final static Lexer.Entry mainEntry =
			new Lexer.Entry("main", Location.inlined("Object"), false, 
					Lists.empty(), mainClauses);
		private final static Lexer.Entry commentEntry =
			new Lexer.Entry("comment", Location.inlined("void"), false, 
					Lists.empty(), commentClauses);
			
		@SuppressWarnings("null")
		final static Lexer LEXER =
			new Lexer(
				Location.inlined("private void newline() { }"),
				Arrays.asList(mainEntry, commentEntry), 
				Location.DUMMY);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test(ArithExprsComment.LEXER, true);
		testOutput("ArithExprsComment", ArithExprsComment.LEXER, true);
	}
	
}