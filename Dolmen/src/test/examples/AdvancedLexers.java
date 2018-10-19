package test.examples;

import static test.examples.BasicLexers.test;
import static test.examples.BasicLexers.testOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import common.CSet;
import common.Lists;
import common.Maps;
import syntax.Extent;
import syntax.Lexer;
import syntax.Located;
import syntax.Regular;

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
	static List<Lexer.Clause> inlinedClauses(@NonNull Object... objs) {
		if (objs.length % 2 != 0) throw new IllegalArgumentException();
		int n = objs.length / 2;
		List<Lexer.Clause> res = new ArrayList<>(n);
		for (int i = 0; i < n; ++i) {
			int k = 2 * i;
			Object reg = objs[k];
			Object msg = objs[k+1];
			if (!(reg instanceof Regular)
				|| !(msg instanceof String))
				throw new IllegalArgumentException();
			res.add(new Lexer.Clause(
					Located.dummy((Regular) reg), 
					Extent.inlined((String) msg)));
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
	 * | [_a-zA-Z][_a-zA-Z0-9]*		{ return IDENT(getLexeme()); }
	 * | [0-9]+						{ return INT(getLexeme(), 10); }
	 * | "0x" ([0-9a-fA-F]+	as hex)	{ return INT(hex, 16); }
	 * | "/*"       { comment(); return main(); }
	 * | '+'		{ return PLUS; }
	 * | '-'        { return MINUS; }
	 * | '*'        { return MULT; }
	 * | '/'        { return DIV; }
	 * | '('        { return LPAREN; }
	 * | ')'        { return RPAREN; }
	 * | eof        { return EOF; }
	 * | _          { throw error("Unexpected char"); }
	 * 
	 * rule comment:
	 * | '*' '/'	{ return; }
	 * | '\n'       { newline(); comment(); return; }
	 * | '*'        { comment(); return; }
	 * | [^*\r\n]   { comment(); return; }
	 * | eof		{ throw error("EOF in comment"); }
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
				Regular.binding(
					Regular.star(Regular.chars(hexdigit)),
					Located.dummy("hex")));
		
		private static final List<Lexer.Clause> mainClauses =
			inlinedClauses(
				ws,						" return main(); ",
				newline,				" newline(); return main(); ",
				ident,					" return Token.IDENT(getLexeme()); ",
				decimal,				" return Token.INT(getLexeme(), 10); ",
				hexadecimal,			" return Token.INT(hex, 16); ",
				Regular.string("/*"),	" comment(); return main(); ",
				Regular.string("+"),	" return Token.PLUS; ",
				Regular.string("-"),	" return Token.MINUS; ",
				Regular.string("*"),	" return Token.MULT; ",
				Regular.string("/"),	" return Token.DIV; ",
				Regular.string("("),	" return Token.LPAREN; ",
				Regular.string(")"),	" return Token.RPAREN; ",
				Regular.chars(CSet.EOF),	" return Token.EOF; ",
				Regular.chars(CSet.ALL),	" throw error(\"Unexpected char\"); "
			);
		
		private static final CSet inComment =
			CSet.complement(
				CSet.union(CSet.singleton('\r'), CSet.singleton('\n')));
		private static final List<Lexer.Clause> commentClauses =
			inlinedClauses(
				Regular.string("*/"),		" return; ",
				newline,		 			" newline(); comment(); return;",
				Regular.string("*"), 		" comment(); return;",
				Regular.chars(inComment),   " comment(); return;",
				Regular.chars(CSet.EOF),    " throw error(\"EOF in comment\"); "
			);
		private final static Lexer.Entry mainEntry =
			new Lexer.Entry(true, Located.dummy("main"),
					Extent.inlined("Token"), false, null, mainClauses);
		private final static Lexer.Entry commentEntry =
			new Lexer.Entry(false, Located.dummy("comment"), 
					Extent.inlined("void"), false, null, commentClauses);
			
		final static Lexer LEXER =
			Lexer.of(
				Lists.empty(),
				Extent.inlined(
		"public static abstract class Token {\n" +
		"	private String rep;\n" +
		"\n" +
		"	private Token(String rep) { this.rep = rep; }\n" +
		"\n" +
		"	@Override public String toString() { return rep; }\n" +
		"\n" +
		"   public static class Ident extends Token {\n" +
		"   	public final String id;\n" +
		"		private Ident(String id) {\n" +
		"			super(String.format(\"IDENT(%s)\", id));\n" +
		"			this.id = id;\n" +
		"		}\n" +
		"	}\n" +
		"	public static Ident IDENT(String id) { return new Ident(id); }\n" +
		"\n" +
		"   public static class Int extends Token {\n" +
		"   	public final int val;\n" +
		"		private Int(int val) {\n" +
		"			super(String.format(\"INT(%d)\", val));\n" +
		"			this.val = val;\n" +
		"		}\n" +
		"	}\n" +
		"	public static Int INT(String sval, int radix) {\n" +
		"		return new Int(Integer.parseInt(sval, radix));\n" +
		"	}\n" +
		"\n" +
		"	public final static Token PLUS = new Token(\"PLUS\") {};\n" +
		"	public final static Token MINUS = new Token(\"MINUS\") {};\n" +
		"	public final static Token MULT = new Token(\"MULT\") {};\n" +
		"	public final static Token DIV = new Token(\"DIV\") {};\n" +
		"	public final static Token LPAREN = new Token(\"LPAREN\") {};\n" +
		"	public final static Token RPAREN = new Token(\"RPAREN\") {};\n" +
		"	public final static Token EOF = new Token(\"EOF\") {};\n" +
		"}\n" +
		"\n"),
				Maps.empty(),
				Arrays.asList(mainEntry, commentEntry), 
				Extent.inlined(
		"public static void main(String[] args) throws java.io.IOException {\n" +
    	"    ArithExprsComment lexer = new ArithExprsComment(\n" +
    	"        \"-\", new java.io.InputStreamReader(System.in));\n" +
    	"    Token tok;\n" +
    	"    while ((tok = lexer.main()) != Token.EOF)\n" +
    	"        System.out.println(tok);\n" +
    	"}\n"));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test(ArithExprsComment.LEXER, true);
		testOutput("ArithExprsComment", ArithExprsComment.LEXER, true);
	}
	
}