package jl;

import static syntax.Regular.binding;
import static syntax.Regular.chars;
import static syntax.Regular.plus;
import static syntax.Regular.seq;
import static syntax.Regular.string;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
import common.CSet;
import syntax.Lexer;
import syntax.Location;
import syntax.Regular;
import tagged.Encoder;
import tagged.TLexer;

/**
 * Manual construction of a {@link Lexer} description
 * for the concrete syntax of Dolmen lexer files, 
 * with extension {@code .jl}. The concrete syntax is
 * summarized in docs/LEXER.syntax.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class JLLexer {

	// Common inlined locations
	
	final static Location VOID = Location.inlined("void");
	
	// Common character sets
	
	final static CSet ws = CSet.chars(' ', '\t', '\f');
	
	final static CSet lalpha = CSet.interval('a', 'z');
	final static CSet ualpha = CSet.interval('A', 'Z');
	final static CSet digit = CSet.interval('0', '9');
	final static CSet nzdigit = CSet.interval('1', '9');
	final static CSet identStart =
		CSet.union(CSet.singleton('_'), lalpha, ualpha);
	final static CSet identBody = CSet.union(identStart, digit);
	
	// Common regular expressions
	
	static Regular rchar(char ch) {
		return Regular.chars(CSet.singleton(ch));
	}
	
	final static Regular any =
		Regular.chars(CSet.ALL_BUT_EOF);
	final static Regular nl =
		Regular.or(Regular.string("\r"), Regular.string("\n"),
			Regular.string("\r\n"));
	final static Regular notnl =
		Regular.chars(CSet.complement(CSet.chars('\r', '\n')));
	final static Regular ident =
		Regular.seq(Regular.chars(identStart),
			Regular.star(Regular.chars(identBody)));
	final static Regular escaped =
		Regular.chars(CSet.chars('\\', '\'', '"', 'n', 't', 'b', 'r', ' '));
	final static Regular slcomment =
		Regular.seq(string("//"), Regular.star(notnl));
	
	/**
	 * public { jl.JLToken } 
	 * rule main =
	 * | ws+			{ return main(); }
	 * | newline		{ newline(); return main(); }
	 * | "/*"			{ comment(); return main(); }
	 * | "//" [^\r\n]*	{ return main(); }
	 * | '"'			{ stringBuffer.setLength(0);
	 * 					  string();
	 * 					  return LSTRING(stringBuffer.toString()); 
	 * 				    }
	 * | '{'			{ braceDepth = 1;
	 * 					  .. record current position ..
	 * 					  action();
	 * 					  return ACTION(.. recorded pos, current pos..);
	 * 					}
	 * | '_'			{ return UNDERSCORE; }
	 * | ident			{ ... "rule" -> RULE
	 * 					  ... "shortest" -> SHORTEST
	 * 					  ... "eof" -> EOF
	 * 					  ... "as" -> AS
	 * 					  ... "import" -> IMPORT
	 * 					  ... "static" -> STATIC
	 * 					  ... "public" -> PUBLIC
	 * 					  ... "private" -> PRIVATE
	 * 					  ... _ -> IDENT(getLexeme())
	 * 					}
	 * | "'" ([^\\] as c) "'"
	 * 					{ return LCHAR(c); }
	 * | "'" '\\' (escaped as c) "'"
	 * 					{ return LCHAR(forBackslash(c)); }
	 * // other character syntaxes? like \\uxxxx and \377 
	 * | '='			{ return EQUAL; }
	 * | '|'			{ return OR; }
	 * | '['			{ return LBRACKET; }
	 * | ']'			{ return RBRACKET; }
	 * | '*'			{ return STAR; }
	 * | '?'			{ return MAYBE; }
	 * | '+'			{ return PLUS; }
	 * | '('			{ return LPAREN; }
	 * | ')'			{ return RPAREN; }
	 * | '^'			{ return CARET; }
	 * | '-'			{ return DASH;	}
	 * | '#'			{ return HASH; }
	 * | '.'			{ return DOT; }
	 * | ';'			{ return SEMICOL; }
	 * | eof			{ return END; }
	 * | _				{ throw error("..."); }
	 */
	private final static Lexer.Entry mainEntry =
		new Lexer.Entry.Builder(true, "main", Location.inlined("jl.JLToken"), null)
			.add(plus(chars(ws)), "return main();")
			.add(nl, "newline(); return main();")
			.add(string("/*"), "comment(); return main();")
			.add(seq(string("//"), plus(notnl)), "return main();")
		 	.add(rchar('"'), "stringBuffer.setLength(0);\n" +
							 "string();\n" +
							 "@SuppressWarnings(\"null\")\n" +
							 "jl.JLToken res = LSTRING(stringBuffer.toString());\n" +
							 "return res;")
			.add(rchar('{'), 
				"braceDepth = 1;\n" +
				"int startLine = loc; int startOffset = absPos + curPos;\n" +
				"int startCol = startOffset - bol;\n" +
				"int endOffset = action();\n" +
				"syntax.Location loc = new syntax.Location(\n" +
				"    filename, startOffset, endOffset, startLine, startCol);\n" +
				"return ACTION(loc);")
			.add(rchar('_'), "return UNDERSCORE;")
			.add(ident, "return identOrKeyword(getLexeme());")
			.add(seq(rchar('\''),
					binding(chars(CSet.complement(CSet.singleton('\\'))), 
							"c", Location.DUMMY),
					rchar('\'')),
				"return LCHAR(c);")
			.add(seq(rchar('\''), rchar('\\'), 
					binding(escaped, "c", Location.DUMMY), rchar('\'')),
				"return LCHAR(forBackslash(c));")
			.add(rchar('='), "return EQUAL;")
			.add(rchar('|'), "return OR;")
			.add(rchar('['), "return LBRACKET;")
			.add(rchar(']'), "return RBRACKET;")
			.add(rchar('*'), "return STAR;")
			.add(rchar('?'), "return MAYBE;")
			.add(rchar('+'), "return PLUS;")
			.add(rchar('('), "return LPAREN;")
			.add(rchar(')'), "return RPAREN;")
			.add(rchar('^'), "return CARET;")
			.add(rchar('-'), "return DASH;")
			.add(rchar('#'), "return HASH;")
			.add(rchar('.'), "return DOT;")
			.add(rchar(';'), "return SEMICOL;")
			.add(chars(CSet.EOF), "return END;")
			.add(any, "throw error(\"Unfinished token\");")
			.build();
	
	
	private final static Regular inComment =
		chars(CSet.complement(
			CSet.chars('*', '"', '\'', '\r', '\n')));
	/**
	 * private rule comment =
	 * | '*' '/'		{ return; }
	 * | '*'			{ comment(); return; }
	 * | '"'			{ stringBuffer.setLength(0);
	 * 					  string();
	 * 					  stringBuffer.setLength(0);
	 * 					  comment(); return; }
	 * | "'"			{ skipChar(); comment(); return; }
	 * | eof			{ throw error("Unterminated comment"); }
	 * | newline		{ newline(); comment(); return; }
	 * // cannot do char-by-char without tail-call elimination
	 * | [^*"'\r\n]+	{ comment(); return; }
	 */
	private final static Lexer.Entry commentEntry =
		new Lexer.Entry.Builder(false, "comment", VOID, null)
			.add(string("*/"), "return;")
			.add(rchar('*'), "comment(); return;")
			.add(rchar('"'), "stringBuffer.setLength(0);\n" +
						     "string();\n" +
						     "stringBuffer.setLength(0);" +
						     "comment(); return;\n")
			.add(rchar('\''), "skipChar(); comment(); return;")
			.add(chars(CSet.EOF), "throw error(\"Unterminated comment\");")
			.add(nl, "newline(); comment(); return;")
			.add(plus(inComment), "comment(); return;")
			.build();
	
	
	private final static Regular inString =
		chars(CSet.complement(CSet.chars('\"', '\\')));
	/**
	 * private rule string =
	 * | '"'					{ return; }
	 * | '\\' (escaped as c)	{ stringBuffer.appends(forBackslash(c));
	 * 							  string(); return; }
	 * | '\\' (_ as c)			{ stringBuffer.appends('\\').appends(c);
	 * 							  string(); return; }
	 * | eof					{ throw error("Unterminated string"); }
	 * | [^"\\]+				{ stringBuffer.appends(getLexeme());
	 * 							  string(); return; }
	 */
	private final static Lexer.Entry stringEntry =
		new Lexer.Entry.Builder(false, "string", VOID, null)
			.add(rchar('"'), "return;")
			.add(seq(rchar('\\'), binding(escaped, "c", Location.DUMMY)), 
					"stringBuffer.append(forBackslash(c)); string(); return;")
			.add(seq(rchar('\\'), binding(any, "c", Location.DUMMY)),
					"stringBuffer.append('\\\\').append(c); string(); return;")
			.add(chars(CSet.EOF), "throw error(\"Unterminated string\");")
			.add(plus(inString), 
					"stringBuffer.append(getLexeme()); string(); return;")
			.build();
	
	private final static Regular inAction =
		chars(CSet.complement(
			CSet.chars('{', '}', '"', '\'', '/', '\r', '\n')));
	/**
	 * private { int }
	 * rule action =
	 * | '{'			{ ++braceDepth; return action(); }
	 * | '}'			{ --braceDepth;
	 * 					  if (braceDepth == 0) return absPos + curPos;
	 * 					  return action(); }
	 * | '"'			{ stringBuffer.setLength(0);
	 * 					  string();
	 * 					  stringBuffer.setLength(0);
	 * 					  return action(); }
	 * | "'"			{ skipChar(); return action(); }
	 * | "/*"			{ comment(); return action(); }
	 * | "//" [^\r\n]*	{ return action(); }
	 * | eof			{ throw error("Unterminated action"); }
	 * | newline		{ newline(); return action(); }
	 * // cannot do char-by-char without tail-call elimination
	 * | [^{}"'/\r\n]+  { return action(); }
	 */
	private final static Lexer.Entry actionEntry =
		new Lexer.Entry.Builder(false, "action", Location.inlined("int"), null)
			.add(rchar('{'), "++braceDepth; return action();")
			.add(rchar('}'), "--braceDepth;\n" +
							 "if (braceDepth == 0) return absPos + startPos - 1;\n" +
							 "return action();")
			.add(rchar('"'), "stringBuffer.setLength(0);\n" +
							 "string();" +
							 "stringBuffer.setLength(0);\n" +
							 "return action();")
			.add(rchar('\''), "skipChar(); return action();")
			.add(string("/*"), "comment(); return action();")
			.add(slcomment, "return action();")
			.add(chars(CSet.EOF), "throw error(\"Unterminated action\");")
			.add(nl, "newline(); return action();")
			.add(plus(inAction), "return action();")
			.build();
	
	private final static Regular inChar =
		chars(CSet.complement(CSet.chars('\\', '\'')));
	/**
	 * private rule skipChar =
	 * // other character syntaxes? like \\uxxxx and \377
	 * | [^ '\\' '\''] "'"	{ return; }
	 * | '\\' _ "'"			{ return; }
	 * // don't jeopardize everything for a syntax error in a Java action
	 * | ""					{ return; }
	 */
	private final static Lexer.Entry skipCharEntry =
		new Lexer.Entry.Builder(false, "skipChar", VOID, null)
			.add(seq(inChar,  rchar('\'')), "return;")
			.add(seq(rchar('\\'), any, rchar('\'')), "return;")
			.add(Regular.EPSILON, "return;")
			.build();
	
	private final static String header = 
	"\n" +
	"    private final StringBuilder stringBuffer = new StringBuilder();\n" +
	"    private int braceDepth = 0;\n" +
	"    \n" +
	"    private int loc = 1;\n" +
	"    private int bol = 0;\n" +
	"    \n" +
	"    private void newline() {\n" +
	"        ++loc; bol = absPos + curPos;\n" +
	"    }\n" +
	"    \n" +
	"    private char forBackslash(char c) {\n" +
	"        switch (c) {\n" +
	"        case 'n': return '\\012';\n" +	// 10
	"        case 'r': return '\\015';\n" +	// 13
	"        case 'b': return '\\010';\n" +	// 8
	"        case 't': return '\\011';\n" +	// 9
	"        default: return c;\n" +
	"        }\n" +
	"    }\n" +
	"    \n" +
	"    private jl.JLToken identOrKeyword(String id) {\n" +
	"        if (id.equals(\"rule\")) return RULE;\n" +
	"        else if (id.equals(\"shortest\")) return SHORTEST;\n" +
	"        else if (id.equals(\"eof\")) return EOF;\n" +
	"        else if (id.equals(\"as\")) return AS;\n" +
	"        else if (id.equals(\"import\")) return IMPORT;\n" +
	"        else if (id.equals(\"static\")) return STATIC;\n" +
	"        else if (id.equals(\"public\")) return PUBLIC;\n" +
	"        else if (id.equals(\"private\")) return PRIVATE;\n" +
	"        else return IDENT(id);\n" +
	"    }\n" +
	"    \n" +
	"    private LexicalError error(String msg) {\n" +
	"        String res = String.format(\"%s (line %d, col %d)\",\n" +
	"            msg, loc, absPos + curPos - bol);\n" +
	"        return new LexicalError(res);\n" +
	"    }\n"
	;
	private final static String footer = "";
	
	/**
	 * The lexer definition for .jl lexer descriptions
	 */
	@SuppressWarnings("null")
	public final static Lexer INSTANCE =
		new Lexer(
			Arrays.asList("package jl;", "import static jl.JLToken.*;"),
			Location.inlined(header),
			Arrays.asList(mainEntry, commentEntry, 
				stringEntry, actionEntry, skipCharEntry),
			Location.inlined(footer));
	
	private static void testOutput(String className, Lexer lexer, boolean opt) {
		System.out.println("=========LEXER========");
		System.out.println(lexer);
		System.out.println("--------ENCODED-------");
		TLexer tlexer = Encoder.encodeLexer(lexer, opt);
		System.out.println(tlexer);		
		System.out.println("--------AUTOMATA------");
		Automata aut = Determinize.lexer(lexer, opt);
		System.out.println(aut);
		File file = new File("src/jl/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			AutomataOutput.output(writer, className, aut);
			System.out.println("----------JAVA--------");
			System.out.println("Generated in " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testOutput("JLLexerGenerated", INSTANCE, true);
	}
	
}