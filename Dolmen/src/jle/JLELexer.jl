/**
 * Lexer description for extended Dolmen lexers (.jl)
 *
 * @author Stéphane Lescuyer
 */

import static jle.JLEParser.Token.*;
import jle.JLEParser.Token;

// Java header
{
    private final StringBuilder stringBuffer = new StringBuilder();
	private int braceDepth = 0;
    
    private char forBackslash(char c) {
        switch (c) {
        case 'n': return '\012';
        case 'r': return '\015';
        case 'b': return '\010';
        case 't': return '\011';
        case 'f': return '\014';
        default: return c;
        }
    }
    
    private char fromOctalCode(String code) {
        return (char)(Integer.parseInt(code, 8));    }
    
    private char fromHexCode(String code) {
        return (char)(Integer.parseInt(code, 16));    }
    
    private Token identOrKeyword(String id) {
        if (id.equals("rule")) return RULE;
        else if (id.equals("shortest")) return SHORTEST;
        else if (id.equals("eof")) return EOF;
        else if (id.equals("as")) return AS;
        else if (id.equals("orelse")) return ORELSE;
        else if (id.equals("import")) return IMPORT;
        else if (id.equals("static")) return STATIC;
        else if (id.equals("public")) return PUBLIC;
        else if (id.equals("private")) return PRIVATE;
        else return IDENT(id);
    }
}

// Whitespace and newline
ws = [' ' '\t' '\f'];
nl = '\r' | '\n' | "\r\n";
notnl = [^ '\r' '\n'];

// Comments
slcomment = "//" notnl*;

// Alpha-numeric characters
lalpha = ['a'-'z'];
ualpha = ['A'-'Z'];
digit = ['0'-'9'];
nzdigit = digit # ['0'];

// Identifiers
identStart = (lalpha | ualpha | '_');
ident = identStart (identStart | digit)*;

// Integral values
decimal = "0" | (nzdigit digit*);
odigit = ['0'-'7'];
octal = odigit | odigit odigit | ['0'-'3'] odigit odigit;
hexdigit = digit | ['a'-'f' 'A'-'F'];

// Escape sequences
escaped = ['\\' '\'' '"' 'n' 't' 'b' 'f' 'r' ' '];

/**
 * The lexer rules
 */

public { Token } rule main =
| ws+			{ return main(); }
| nl			{ newline(); return main(); }
| "/*"			{ comment(); return main(); }
| slcomment		{ return main(); }
| '"'			{ Position stringStart = getLexemeStart();
				  stringBuffer.setLength(0);
				  string();
				  startLoc = stringStart;
				  Token res = LSTRING(stringBuffer.toString());
				  return res;
				}
| '{'			{ braceDepth = 1;
				  Position start = getLexemeStart();
				  Position p = getLexemeEnd();
				  int endOffset = action();
				  syntax.Extent ext = new syntax.Extent(
				  	filename, p.offset, endOffset, p.line, p.column());
				  startLoc = start;
				  return ACTION(ext);
				}
| '_'			{ return UNDERSCORE; }
| ident			{ return identOrKeyword(getLexeme()); }
| decimal		{ return INTEGER(Integer.parseInt(getLexeme())); }
| "'" ([^ '\\'] as c) "'"
				{ return LCHAR(c); }
| "'" '\\' (escaped as c) "'"
				{ return LCHAR(forBackslash(c)); }
| "'" '\\' (octal as code) "'"
				{ return LCHAR(fromOctalCode(c)); }
| "'" '\\' 'u'+ (hexdigit<4> as code) "'"
				{ return LCHAR(fromHexCode(c)); }
| "'" '\\' 'u'+ { throw error("Invalid Unicode escape sequence"); }
| '='			{ return EQUAL; }
| '|'			{ return OR; }
| '['			{ return LBRACKET; }
| ']'			{ return RBRACKET; }
| '*'			{ return STAR; }
| '?'			{ return MAYBE; }
| '+'			{ return PLUS; }
| '('			{ return LPAREN; }
| ')'			{ return RPAREN; }
| '^'			{ return CARET; }
| '-'			{ return DASH; }
| '#'			{ return HASH; }
| '.'			{ return DOT; }
| '<'			{ return LANGLE; }
| '>'			{ return RANGLE; }
| ','			{ return COMMA; }
| ';'			{ return SEMICOL; }
| eof			{ return END; }
| _				{ throw error("Unfinished token"); }

/**
 * Matches multi-line comments, both in and outside Java actions
 */
private { void } rule comment =
| "*/"				{ return; }
| '*'				{ comment(); return; }
| '"'				{ stringBuffer.setLength(0);
					  string();
					  stringBuffer.setLength(0);
					  comment(); return;
					}
| "'"				{ skipChar(); comment(); return; }
| eof				{ throw error("Unterminated comment"); }
| nl				{ newline(); comment(); return; }
| orelse			{ comment(); return; } 

/**
 * Matches string literals, both in and outside Java actions
 */
private { void } rule string =
| '"'					{ return; }
| '\\' (escaped as c)	{ stringBuffer.appends(forBackslash(c));
						  string(); return;
						}
| '\\' (octal as code)	{ stringBuffer.appends(fromOctalCode(code));
						  string(); return;
						}
| '\\' 'u'+ (hexdigit<4> as code)
						{ stringBuffer.appends(fromHexCode(code));
						  string(); return; 
						}
| '\\' 'u'+				{ throw error("Invalid Unicode escape sequence"); }
| '\\' (_ as c)			{ stringBuffer.appends('\\').appends(c);
						  string(); return;
						}
| '\\'					{ throw error("Unterminated escape sequence in string literal"); }
| eof					{ throw error("Unterminated string literal"); }
| orelse				{ stringBuffer.appends(getLexeme());
						  string(); return;
						}

/**
 * Matches Java actions
 */
private { int } rule action =
| '{'		{ ++braceDepth; return action(); }
| '}'		{ --braceDepth;
			  if (braceDepth == 0) return getLexemeStart().offset - 1;
			  return action();
			}
| '"'		{ stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  return action();
			}
| '\''		{ skipChar(); return action(); }
| "/*"		{ comment(); return action(); }
| slcomment { return action(); }
| eof		{ throw error("Unterminated action"); }
| nl		{ newline(); return action(); }
// Cannot do char-by-char without tail-call elimination
| '/' 		{ return action(); }
| orelse	{ return action(); }

/**
 * Matches character literals in Java actions
 */
private { void } rule skipChar =
| [^ '\\' '\''] "'"	{ return; }
| '\\' octal "'"	{ return; }
| '\\' _ "'"		{ return; }
// Don't jeopardize everything for a syntax error in a Java action
| ""				{ return; }

// Java footer
{ }