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
| ws+			{ continue main; }
| nl			{ newline(); continue main; }
| "/*"			{ comment(); continue main; }
| slcomment		{ continue main; }
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
				{ return LCHAR(fromOctalCode(code)); }
| "'" '\\' 'u'+ (hexdigit<4> as code) "'"
				{ return LCHAR(fromHexCode(code)); }
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
| '*'				{ continue comment; }
| '"'				{ stringBuffer.setLength(0);
					  string();
					  stringBuffer.setLength(0);
					  continue comment;
					}
| "'"				{ skipChar(); continue comment; }
| eof				{ throw error("Unterminated comment"); }
| nl				{ newline(); continue comment; }
| orelse			{ continue comment; } 

/**
 * Matches string literals, both in and outside Java actions
 */
private { void } rule string =
| '"'					{ return; }
| '\\' (escaped as c)	{ stringBuffer.append(forBackslash(c));
						  continue string;
						}
| '\\' (octal as code)	{ stringBuffer.append(fromOctalCode(code));
						  continue string;
						}
| '\\' 'u'+ (hexdigit<4> as code)
						{ stringBuffer.append(fromHexCode(code));
						  continue string;
						}
| '\\' 'u'+				{ throw error("Invalid Unicode escape sequence"); }
| '\\' (_ as c)			{ stringBuffer.append('\\').append(c);
						  continue string;
						}
| '\\'					{ throw error("Unterminated escape sequence in string literal"); }
| eof					{ throw error("Unterminated string literal"); }
| orelse				{ stringBuffer.append(getLexeme());
						  continue string;
						}

/**
 * Matches Java actions
 */
private { int } rule action =
| '{'		{ ++braceDepth; continue action; }
| '}'		{ --braceDepth;
			  if (braceDepth == 0) return getLexemeStart().offset - 1;
			  continue action;
			}
| '"'		{ stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  continue action;
			}
| '\''		{ skipChar(); continue action; }
| "/*"		{ comment(); continue action; }
| slcomment { continue action; }
| eof		{ throw error("Unterminated action"); }
| nl		{ newline(); continue action; }
// Cannot do char-by-char without tail-call elimination
| '/' 		{ continue action; }
| orelse	{ continue action; }

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