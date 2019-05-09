/**
 * Lexer description for extended Dolmen lexers (.jl)
 *
 * @author Stéphane Lescuyer
 */

import java.util.Stack;

import static org.stekikun.dolmen.jle.JLEParser.Token.*;
import org.stekikun.dolmen.jle.JLEParser.Token;
import org.stekikun.dolmen.syntax.Extent;

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
    
    // We override the default behaviour of #error(String) to conveniently
    // report errors at the beginning of a saved position instead of the current
    // lexeme start.
    
    private Stack<Position> errLocs = new Stack<>();
    
    @Override
    protected LexicalError error(String msg) {
    	Position err = errLocs.isEmpty() ? getLexemeStart() : errLocs.peek();
    	return new LexicalError(err, msg);
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
escaped = ['\\' '\'' '"' 'n' 't' 'b' 'f' 'r'];

/**
 * The lexer rules
 */

public { Token } rule main =
| ws+			{ continue main; }
| nl			{ newline(); continue main; }
| "/*"			{ comment(); continue main; }
| slcomment		{ continue main; }
| '"'			{ Position stringStart = getLexemeStart();
				  errLocs.push(stringStart);
				  stringBuffer.setLength(0);
				  int eline = string(true);
				  startLoc = stringStart;
				  Token res = stringStart.line == eline ? 
				 	LSTRING(stringBuffer.toString()) :
				 	MLSTRING(stringBuffer.toString());
				  errLocs.pop();
				  return res;
				}
| '{'			{ braceDepth = 1;
				  Position start = getLexemeStart();
				  errLocs.push(start);
				  Position p = getLexemeEnd();
				  int endOffset = action();
				  Extent ext = new Extent(
				  	filename, p.offset, endOffset, p.line, p.column());
				  startLoc = start;
				  errLocs.pop();
				  return ACTION(ext);
				}
| '_'			{ return UNDERSCORE; }
| ident			{ return identOrKeyword(getLexeme()); }
| decimal		{ return INTEGER(Integer.parseInt(getLexeme())); }
| "'"			{ Position start = getLexemeStart();
				  errLocs.push(start);
				  char c = character();
				  characterClose();
				  startLoc = start;
				  errLocs.pop();
				  return LCHAR(c);
				 }
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
| _ as c		{ throw error("Unexpected character: " + c); }

/**
 * Matches multi-line comments, both in and outside Java actions
 */
private { void } rule comment =
| "*/"				{ return; }
| '*'				{ continue comment; }
| eof				{ throw error("Unterminated comment"); }
| nl				{ newline(); continue comment; }
| orelse			{ continue comment; } 

/**
 * Matches string literals, both in and outside Java actions
 *
 * Parameter [multi] describes whether multi-line string literals
 * are allowed in the calling context. This is used to forbid line
 * terminators in literals in Java actions and allow them elsewhere.
 */
private { int } rule string{boolean multi} =
| '"'					{ return getLexemeEnd().line; }
| '\\'					{ errLocs.push(getLexemeStart());
						  char c = escapeSequence();
						  stringBuffer.append(c);
						  errLocs.pop();
						  continue string;
						}
| nl					{ if (!multi)
							throw error("String literal in Java action not properly closed");
						  newline();
						  stringBuffer.append(getLexeme());
						  continue string; }
| eof					{ throw error("Unterminated string literal"); }
| orelse				{ stringBuffer.append(getLexeme());
						  continue string;
						}

/**
 * Matches the contents of a character literal, both in and outside Java actions
 */
private { char } rule character =
| "'"			{ throw error("Invalid character literal"); }
| ([^ '\r' '\n' '\\'] as c)
				{ return c; }
| '\\'			{ errLocs.push(getLexemeStart());
				  char c = escapeSequence();
				  errLocs.pop();
				  return c;
				}
| (nl | eof)
				{ throw error("Unterminated character literal"); }

/**
 * Matches the closing quote of a character literal, both in and outside Java actions
 *
 * This separate rule is necessary to be able to factor character escape sequences
 * between string and character literals.
 */
private { void } rule characterClose =
| "'"			{ return; }
| (_ | eof)	{ throw error("Unterminated character literal"); }

/**
 * Matches a character escape sequence, both in character and string literals
 *
 * Parameter [start] is the position of the escaping '\\' character and is used
 * for better error reporting. 
 */
private { char } rule escapeSequence =
| escaped as c	{ return forBackslash(c); }
| octal as code { return fromOctalCode(code); }
| 'u'+ (hexdigit<4> as code)
				{ return fromHexCode(code); }
| 'u'+			{ throw error("Invalid Unicode escape sequence: " +
					"expected four hexadecimal digits after \\" + getLexeme()); }
| _				{ throw error("Invalid escape sequence: " + 
					"only \\\\, \\\', \\\", \\n, \\t, \\b, \\f, \\r are supported"); } 
| eof			{ throw error("Unterminated escape sequence"); }

/**
 * Matches Java actions
 */
private { int } rule action =
| '{'		{ ++braceDepth; continue action; }
| '}'		{ --braceDepth;
			  if (braceDepth == 0) return getLexemeStart().offset - 1;
			  continue action;
			}
| '"'		{ errLocs.push(getLexemeStart());
		      stringBuffer.setLength(0);
			  string(false);		// Java string literals are single-line
			  stringBuffer.setLength(0);
			  errLocs.pop();
			  continue action;
			}
| '\''		{ errLocs.push(getLexemeStart());
			  character();
			  characterClose();
			  errLocs.pop();
			  continue action;
			}
| "/*"		{ comment(); continue action; }
| slcomment { continue action; }
| eof		{ throw error("Unterminated action"); }
| nl		{ newline(); continue action; }
| '/' 		{ continue action; }
| orelse	{ continue action; }

// Java footer
{ }