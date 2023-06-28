package org.stekikun.dolmen.jl;

import static org.stekikun.dolmen.syntax.Regular.binding;
import static org.stekikun.dolmen.syntax.Regular.chars;
import static org.stekikun.dolmen.syntax.Regular.plus;
import static org.stekikun.dolmen.syntax.Regular.seq;
import static org.stekikun.dolmen.syntax.Regular.star;
import static org.stekikun.dolmen.syntax.Regular.string;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.stekikun.dolmen.automaton.Automata;
import org.stekikun.dolmen.automaton.Determinize;
import org.stekikun.dolmen.codegen.AutomataOutput;
import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Lists;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.syntax.Lexer;
import org.stekikun.dolmen.syntax.Located;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.tagged.Encoder;
import org.stekikun.dolmen.tagged.TLexer;

/**
 * Manual construction of a {@link Lexer} description
 * for the concrete syntax of Dolmen lexer files, 
 * with extension {@code .jl}. The concrete syntax is
 * summarized in docs/LEXER.syntax.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class JLLexer {

	// Common inlined extends
	
	final static Extent VOID = Extent.inlined("void");
	
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
	final static Regular integer =
		Regular.or(Regular.string("0"),
				Regular.seq(Regular.chars(nzdigit), 
					Regular.star(Regular.chars(digit))));
	final static Regular escaped =
		Regular.chars(CSet.chars('\\', '\'', '"', 'n', 't', 'b', 'f', 'r', ' '));
	final static Regular slcomment =
		Regular.seq(string("//"), Regular.star(notnl));
	final static Regular octdigit = Regular.chars(CSet.interval('0', '7'));
	final static Regular octal =
		Regular.or(octdigit,
			Regular.seq(octdigit, octdigit),
			Regular.seq(Regular.chars(CSet.interval('0', '3')), octdigit, octdigit));
	final static Regular hexdigit =
		Regular.or(Regular.chars(digit), 
			Regular.chars(CSet.interval('a', 'f')),
			Regular.chars(CSet.interval('A', 'F')));
	
	
	/**
	 * public { org.stekikun.dolmen.jl.JLToken } 
	 * rule main =
	 * | ws+			{ return main(); }
	 * | newline		{ newline(); return main(); }
	 * | "/*"			{ comment(); return main(); }
	 * | "//" [^\r\n]*	{ return main(); }
	 * | '"'			{ Position stringStart = getLexemeStart();
	 * 					  stringBuffer.setLength(0);
	 * 					  string();
	 * 					  startLoc = stringStart;
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
	 * 					  ... "orelse" -> ORELSE
	 * 					  ... "import" -> IMPORT
	 * 					  ... "static" -> STATIC
	 * 					  ... "public" -> PUBLIC
	 * 					  ... "private" -> PRIVATE
	 * 					  ... _ -> IDENT(getLexeme())
	 * 					}
	 * | integer		{ return INTEGER(Integer.parseInt(getLexeme())); }
	 * | "'" ([^\\] as c) "'"
	 * 					{ return LCHAR(c); }
	 * | "'" '\\' (escaped as c) "'"
	 * 					{ return LCHAR(forBackslash(c)); }
	 * | "'" '\\' (octal as code) "'"
	 * 					{ return LCHAR(fromOctalCode(c)); }
	 * | "'" '\\' 'u'+ (hexdigit&lt;4&gt; as code) "'"
	 * 					{ return LCHAR(fromHexCode(c)); }
	 * | "'" '\\' 'u'+  { throw error("Invalid Unicode escape sequence"); }
	 * // other character syntaxes? like \\uxxxx 
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
	 * | '<'			{ return LANGLE; }
	 * | '>'			{ return RANGLE; }
	 * | '.'			{ return COMMA; }
	 * | ';'			{ return SEMICOL; }
	 * | eof			{ return END; }
	 * | _				{ throw error("..."); }
	 */
	private final static Lexer.Entry mainEntry =
		new Lexer.Entry.Builder(
				true, Located.dummy("main"), 
				Extent.inlined("org.stekikun.dolmen.jl.JLToken"), null)
			.add(plus(chars(ws)), "return main();")
			.add(nl, "newline(); return main();")
			.add(string("/*"), "comment(); return main();")
			.add(seq(string("//"), star(notnl)), "return main();")
		 	.add(rchar('"'), "Position stringStart = getLexemeStart();\n" +
		 					 "stringBuffer.setLength(0);\n" +
							 "string();\n" +
		 					 "startLoc = stringStart;\n" +
							 "org.stekikun.dolmen.jl.JLToken res = LSTRING(stringBuffer.toString());\n" +
							 "return res;")
			.add(rchar('{'), 
				"braceDepth = 1;\n" +
			    "Position start = getLexemeStart();\n" +
				"Position p = getLexemeEnd();\n" +
				"int endOffset = action();\n" +
				"org.stekikun.dolmen.syntax.Extent ext = new org.stekikun.dolmen.syntax.Extent(\n" +
				"    filename, p.offset, endOffset, p.line, p.column());\n" +
				"startLoc = start;\n" +
				"return ACTION(ext);")
			.add(rchar('_'), "return UNDERSCORE;")
			.add(ident, "return identOrKeyword(getLexeme());")
			.add(integer, "return INTEGER(Integer.parseInt(getLexeme()));")
			.add(seq(rchar('\''),
					binding(chars(CSet.complement(CSet.singleton('\\'))), 
							Located.dummy("c")),
					rchar('\'')),
				"return LCHAR(c);")
			.add(seq(rchar('\''), rchar('\\'), 
					binding(escaped, Located.dummy("c")), rchar('\'')),
				"return LCHAR(forBackslash(c));")
			.add(seq(rchar('\''), rchar('\\'), 
					binding(octal, Located.dummy("code")), rchar('\'')),
				"return LCHAR(fromOctalCode(code));")
			.add(seq(rchar('\''), rchar('\\'), plus(rchar('u')),
					binding(Regular.repeat(hexdigit, 4), Located.dummy("code")), 
					rchar('\'')),
				"return LCHAR(fromHexCode(code));")
			.add(seq(rchar('\''), rchar('\\'), plus(rchar('u'))),
					"throw error(\"Invalid Unicode escacpe sequence\");")
			
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
			.add(rchar('<'), "return LANGLE;")
			.add(rchar('>'), "return RANGLE;")
			.add(rchar(','), "return COMMA;")
			.add(rchar(';'), "return SEMICOL;")
			.add(Regular.EOF, "return END;")
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
		new Lexer.Entry.Builder(false, Located.dummy("comment"), VOID, null)
			.add(string("*/"), "return;")
			.add(rchar('*'), "comment(); return;")
			.add(rchar('"'), "stringBuffer.setLength(0);\n" +
						     "string();\n" +
						     "stringBuffer.setLength(0);" +
						     "comment(); return;\n")
			.add(rchar('\''), "skipChar(); comment(); return;")
			.add(Regular.EOF, "throw error(\"Unterminated comment\");")
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
	 * | '\\' (octal as code)	{ stringBuffer.appends(fromOctalCode(code));
	 *                            string(); return; }
	 * | '\\' 'u'+ (hexdigit<4> as code)	
	 * 							{ stringBuffer.appends(fromHexCode(code));
	 *                            string(); return; }
	 * | '\\' 'u'+				{ throw error("Invalid Unicode escape sequence"); }
	 * | '\\' (_ as c)			{ stringBuffer.appends('\\').appends(c);
	 * 							  string(); return; }
	 * | '\\'					{ throw error("Unterminated escape sequence in string literal"); }
	 * | eof					{ throw error("Unterminated string literal"); }
	 * | [^"\\]+				{ stringBuffer.appends(getLexeme());
	 * 							  string(); return; }
	 */
	private final static Lexer.Entry stringEntry =
		new Lexer.Entry.Builder(false, Located.dummy("string"), VOID, null)
			.add(rchar('"'), "return;")
			.add(seq(rchar('\\'), binding(escaped, Located.dummy("c"))), 
					"stringBuffer.append(forBackslash(c)); string(); return;")
			.add(seq(rchar('\\'), binding(octal, Located.dummy("code"))), 
					"stringBuffer.append(fromOctalCode(code)); string(); return;")
			.add(seq(rchar('\\'), plus(rchar('u')), 
					binding(Regular.repeat(hexdigit, 4), Located.dummy("code"))), 
					"stringBuffer.append(fromHexCode(code)); string(); return;")
			.add(seq(rchar('\\'), plus(rchar('u'))),
					"throw error(\"Invalid Unicode escacpe sequence\");")
			.add(seq(rchar('\\'), binding(any, Located.dummy("c"))),
					"stringBuffer.append('\\\\').append(c); string(); return;")
			.add(rchar('\\'), "throw error(\"Unterminated escape sequence in string literal\");")
			.add(Regular.EOF, "throw error(\"Unterminated string literal\");")
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
	 * | '/'			{ return action(); }
	 * | [^{}"'/\r\n]+  { return action(); }
	 */
	private final static Lexer.Entry actionEntry =
		new Lexer.Entry.Builder(false, Located.dummy("action"), Extent.inlined("int"), null)
			.add(rchar('{'), "++braceDepth; return action();")
			.add(rchar('}'), "--braceDepth;\n" +
							 "if (braceDepth == 0) return getLexemeStart().offset - 1;\n" +
							 "return action();")
			.add(rchar('"'), "stringBuffer.setLength(0);\n" +
							 "string();" +
							 "stringBuffer.setLength(0);\n" +
							 "return action();")
			.add(rchar('\''), "skipChar(); return action();")
			.add(string("/*"), "comment(); return action();")
			.add(slcomment, "return action();")
			.add(Regular.EOF, "throw error(\"Unterminated action\");")
			.add(nl, "newline(); return action();")
			.add(rchar('/'), "return action();")
			.add(plus(inAction), "return action();")
			.build();
	
	private final static Regular inChar =
		chars(CSet.complement(CSet.chars('\\', '\'')));
	/**
	 * private rule skipChar =
	 * // other character syntaxes? like \\uxxxx
	 * | [^ '\\' '\''] "'"	{ return; }
	 * | '\\' octal "'"	    { return; }
	 * | '\\' _ "'"			{ return; }
	 * // don't jeopardize everything for a syntax error in a Java action
	 * | ""					{ return; }
	 */
	private final static Lexer.Entry skipCharEntry =
		new Lexer.Entry.Builder(false, Located.dummy("skipChar"), VOID, null)
			.add(seq(inChar,  rchar('\'')), "return;")
			.add(seq(rchar('\\'), octal, rchar('\'')), "return;")
			.add(seq(rchar('\\'), any, rchar('\'')), "return;")
			.add(Regular.EPSILON, "return;")
			.build();
	
	private final static String header = 
	"\n" +
	"    private final StringBuilder stringBuffer = new StringBuilder();\n" +
	"    private int braceDepth = 0;\n" +
	"    \n" +
//	"    private int loc = 1;\n" +
//	"    private int bol = 0;\n" +
//	"    \n" +
//	"    private void newline() {\n" +
//	"        ++loc; bol = absPos + curPos;\n" +
//	"    }\n" +
//	"    \n" +
	"    private char forBackslash(char c) {\n" +
	"        switch (c) {\n" +
	"        case 'n': return '\\012';\n" +	// 10
	"        case 'r': return '\\015';\n" +	// 13
	"        case 'b': return '\\010';\n" +	// 8
	"        case 't': return '\\011';\n" +	// 9
	"        case 'f': return '\\014';\n" + // 12
	"        default: return c;\n" +
	"        }\n" +
	"    }\n" +
	"    \n" +
	"    private char fromOctalCode(String code) {\n" +
	"        return (char)(Integer.parseInt(code, 8));\n" +
	"    }\n" +
	"    \n" +
	"    private char fromHexCode(String code) {\n" +
	"        int c = Integer.parseInt(code, 16);\n" +
	"        if (c == 0xFFFF) throw error(\"Invalid character code \\uffff\");\n" +
	"        return (char)c;\n" +
	"    }\n" +
	"    \n" +
	"    private org.stekikun.dolmen.jl.JLToken identOrKeyword(String id) {\n" +
	"        if (id.equals(\"rule\")) return RULE;\n" +
	"        else if (id.equals(\"shortest\")) return SHORTEST;\n" +
	"        else if (id.equals(\"eof\")) return EOF;\n" +
	"        else if (id.equals(\"as\")) return AS;\n" +
	"        else if (id.equals(\"orelse\")) return ORELSE;\n" +
	"        else if (id.equals(\"import\")) return IMPORT;\n" +
	"        else if (id.equals(\"static\")) return STATIC;\n" +
	"        else if (id.equals(\"public\")) return PUBLIC;\n" +
	"        else if (id.equals(\"private\")) return PRIVATE;\n" +
	"        else return IDENT(id);\n" +
	"    }\n"
	;
	private final static String footer = "";
	
	/**
	 * The lexer definition for .jl lexer descriptions
	 */
	public final static Lexer INSTANCE =
		new Lexer.Builder(
			Lists.empty(),
			Arrays.asList(
				Located.dummy("package org.stekikun.dolmen.jl;"),
				Located.dummy("import static org.stekikun.dolmen.jl.JLToken.*;")),
			Extent.inlined(header),
			Maps.empty(),
			Extent.inlined(footer))
			.addEntries(mainEntry, commentEntry, 
				stringEntry, actionEntry, skipCharEntry)
			.build();
	
	private static void testOutput(String className, Lexer lexer, boolean opt) {
		System.out.println("=========LEXER========");
		System.out.println(lexer);
		System.out.println("--------ENCODED-------");
		TLexer tlexer = Encoder.encodeLexer(lexer, opt);
		System.out.println(tlexer);		
		System.out.println("--------AUTOMATA------");
		Automata aut = Determinize.lexer(lexer, opt);
		System.out.println(aut);
		File file = new File("src/org/stekikun/dolmen/jl/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			AutomataOutput.outputDefault(writer, className, aut);
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