package jl;

import static syntax.Regular.binding;
import static syntax.Regular.chars;
import static syntax.Regular.plus;
import static syntax.Regular.seq;
import static syntax.Regular.string;

import java.util.Arrays;

import common.CSet;
import common.Lists;
import syntax.Lexer;
import syntax.Location;
import syntax.Regular;

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
	 * public { JLToken } 
	 * rule main =
	 * | ws+			{ return main(); }
	 * | newline		{ newline(); return main(); }
	 * | "/*"			{ comment(); return main(); }
	 * | "//" [^\r\n]*	{ return main(); }
	 * | '"'			{ stringBuffer.clear();
	 * 					  string();
	 * 					  return STRING(stringBuffer.toString()); 
	 * 				    }
	 * | '{'			{ braceDepth = 1;
	 * 					  .. record current position ..
	 * 					  action();
	 * 					  return ACTION(.. recorded pos, current pos..);
	 * 					}
	 * | ident			{ ... "rule" -> RULE
	 * 					  ... "shortest" -> SHORTEST
	 * 					  ... "eof" -> EOF
	 * 					  ... "as" -> AS
	 * 					  ... _ -> IDENT(getLexeme())
	 * 					}
	 * | "'" ([^\\] as c) "'"
	 * 					{ return CHAR(c); }
	 * | "'" '\\' (escaped as c) "'"
	 * 					{ return CHAR(forBackslash(c)); }
	 * // other character syntaxes? like \\uxxxx and \377 
	 * | '_'			{ return UNDERSCORE; }
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
	 * | eof			{ return END; }
	 * | _				{ throw new LexicalError("..."); }
	 */
	private final static Lexer.Entry mainEntry =
		new Lexer.Entry.Builder("main", Location.inlined("Token"), Lists.empty())
			.add(plus(chars(ws)), "return main();")
			.add(nl, "newline(); return main();")
			.add(string("/*"), "comment(); return main();")
			.add(seq(string("//"), plus(notnl)), "return main();")
		 	.add(rchar('"'), "stringBuffer.clear();\n" +
							 "string();\n" +
							 "return STRING(stringBuffer.toString());")
			.add(rchar('{'), "TODO")
			.add(ident, "TODO")
			.add(seq(rchar('\''),
					binding(chars(CSet.complement(CSet.singleton('\\'))), 
							"c", Location.DUMMY),
					rchar('\'')),
				"return CHAR(c);")
			.add(seq(rchar('\''), rchar('\\'), 
					binding(escaped, "c", Location.DUMMY), rchar('\'')),
				"return CHAR(forBackslash(c));")
			.add(rchar('_'), "return UNDERSCORE;")
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
			.add(chars(CSet.EOF), "return END;")
			.add(any, "throw new LexicalError(\"Unfinished token\");")
			.build();
	
	
	private final static Regular inComment =
		chars(CSet.complement(
			CSet.chars('*', '"', '\'', '\r', '\n')));
	/**
	 * private rule comment =
	 * | '*' '/'		{ return; }
	 * | '"'			{ stringBuffer.clear();
	 * 					  string();
	 * 					  stringBuffer.clear();
	 * 					  comment(); return; }
	 * | "'"			{ skipChar(); comment(); return; }
	 * | eof			{ throw new LexicalError("Unterminated comment"); }
	 * | newline		{ newline(); comment(); return; }
	 * // cannot do char-by-char without tail-call elimination
	 * | [^*"'\r\n]+	{ comment(); return; }
	 */
	private final static Lexer.Entry commentEntry =
		new Lexer.Entry.Builder("comment", VOID, Lists.empty())
			.add(string("*/"), "return;")
			.add(rchar('"'), "stringBuffer.clear();\n" +
						     "string();\n" +
						     "stringBuffer.clear();" +
						     "comment(); return;\n")
			.add(rchar('\''), "skipChar(); comment(); return;")
			.add(chars(CSet.EOF), "throw new LexicalError(\"Unterminated comment\");")
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
	 * | eof					{ throw new LexicalError("Unterminated string"); }
	 * | [^"\\]+				{ stringBuffer.appends(getLexeme());
	 * 							  string(); return; }
	 */
	private final static Lexer.Entry stringEntry =
		new Lexer.Entry.Builder("string", VOID, Lists.empty())
			.add(rchar('"'), "return;")
			.add(seq(rchar('\\'), binding(escaped, "c", Location.DUMMY)), 
					"stringBuffer.appends(forBackslash(c)); string(); return;")
			.add(seq(rchar('\\'), binding(any, "c", Location.DUMMY)),
					"stringBuffer.append('\\').append(c); string(); return;")
			.add(chars(CSet.EOF), "throw new LexicalError(\"Unterminated string\");")
			.add(plus(inString), 
					"stringBuffer.appends(getLexeme()); string(); return;")
			.build();
	
	private final static Regular inAction =
		chars(CSet.complement(
			CSet.chars('{', '}', '"', '\'', '/', '\r', '\n')));
	/**
	 * private { int }
	 * rule action =
	 * | '{'			{ ++braceDepth; return action(); }
	 * | '}'			{ --braceDepth;
	 * 					  if (braceDepth == 0) return absPos;
	 * 					  return action(); }
	 * | '"'			{ stringBuffer.clear();
	 * 					  string();
	 * 					  stringBuffer.clear();
	 * 					  return action(); }
	 * | "'"			{ skipChar(); return action(); }
	 * | "/*"			{ comment(); return action(); }
	 * | "//" [^\r\n]*	{ return action(); }
	 * | eof			{ throw new LexicalError("Unterminated action"); }
	 * | newline		{ newline(); return action(); }
	 * // cannot do char-by-char without tail-call elimination
	 * | [^{}"'/\r\n]+  { return action(); }
	 */
	private final static Lexer.Entry actionEntry =
		new Lexer.Entry.Builder("action", Location.inlined("int"), Lists.empty())
			.add(rchar('{'), "++braceDepth; return action();")
			.add(rchar('}'), "--braceDepth;\n" + 
							 "if (braceDepth == 0) return absPos;\n" +
							 "return action();")
			.add(rchar('"'), "stringBuffer.clear();\n" +
							 "string();" +
							 "stringBuffer.clear();\n" +
							 "return action();")
			.add(rchar('\''), "skipChar(); return action();")
			.add(string("/*"), "comment(); return action();")
			.add(slcomment, "return action();")
			.add(chars(CSet.EOF), "throw new LexicalError(\"Unterminated action\");")
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
		new Lexer.Entry.Builder("skipChar", VOID, Lists.empty())
			.add(seq(inChar,  rchar('\'')), "return;")
			.add(seq(rchar('\\'), any, rchar('\'')), "return;")
			.add(Regular.EPSILON, "return;")
			.build();
	
	private final static String header = "";
	private final static String footer = "";
	
	/**
	 * The lexer definition for .jl lexer descriptions
	 */
	@SuppressWarnings("null")
	public final static Lexer INSTANCE =
		new Lexer(Location.inlined(header),
				Arrays.asList(mainEntry, commentEntry, 
					stringEntry, actionEntry, skipCharEntry),
					Location.inlined(footer));
}