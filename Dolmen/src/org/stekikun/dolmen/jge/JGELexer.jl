/**
 * Lexer description for extended Dolmen grammar descriptions ('.jg' files)
 */
import java.util.Stack;
 
import static org.stekikun.dolmen.jge.JGEParser.Token.*;
import org.stekikun.dolmen.jge.JGEParser.Token;
import org.stekikun.dolmen.syntax.PExtent;

// JGLexer class header
{
	// A buffer to lex string literals
	private final StringBuilder stringBuffer = new StringBuilder();
	// The current depth of { } blocks
	private int braceDepth = 0;
	// The current depth of ( ) blocks
	private int parenDepth = 0;
	
	private char forBackslash(char c) {
		switch (c) {
		case 'n': return '\012';	// 10
		case 'r': return '\015';	// 13
		case 'b': return '\010';	// 8
		case 't': return '\011';    // 9
		case 'f': return '\014';    // 12
		default: return c;
		}
	}

    private char fromOctalCode(String code) {
        return (char)(Integer.parseInt(code, 8));
    }
    
    private char fromHexCode(String code) {
        return (char)(Integer.parseInt(code, 16));
    }
	
	private Token identOrKeyword(String id) {
		if (id.equals("import")) return IMPORT;
		else if (id.equals("static")) return STATIC;
		else if (id.equals("public")) return PUBLIC;
		else if (id.equals("private")) return PRIVATE;
		else if (id.equals("token")) return TOKEN;
		else if (id.equals("rule")) return RULE;
		else if (id.equals("continue")) return CONTINUE;
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
nl = "\r" | '\012' | "\015\n";
notnl = [^ '\r' '\n'];

// Comments
slcomment = "//" notnl*;

// Alpha-numeric characters
lalpha = ['a'-'z'];
ualpha = ['A'-'Z'];
digit = ['0'-'9'];
nzdigit = ['1'-'9'];

// Identifiers
idstart = '_' | lalpha | ualpha;
idbody = idstart | digit;
ident = idstart idbody*;

// Integral values
decimal = "0" | (nzdigit digit*);
odigit = ['0'-'7'];
octal = odigit | odigit odigit | ['0'-'3'] odigit odigit;
hexdigit = digit | ['a'-'f' 'A'-'F'];

// Escape sequences
escaped = ['\\' '\'' '"' 'n' 't' 'b' 'f' 'r'];

// Place-holders in parametric extents
hole = '#' (lalpha idbody* as hole_name);

// Lexer rules
public {Token} rule main =
| ws+		{ continue main; }
| nl		{ newline(); continue main; }
| "/*"		{ comment(); continue main; }
| slcomment	{ continue main; }
| '{'		{ braceDepth = 1;
              Position start = getLexemeStart();
              errLocs.push(start);
			  Position p = getLexemeEnd();
			  PExtent ext = action(new PExtent.Builder(filename, p.offset, p.line, p.column()));
              startLoc = start;
              errLocs.pop();
			  return ACTION(ext);
			}
| '('		{ parenDepth = 1;
              Position start = getLexemeStart();
              errLocs.push(start);
			  Position p = getLexemeEnd();
			  PExtent ext = arguments(new PExtent.Builder(filename, p.offset, p.line, p.column()));
			  startLoc = start;
			  errLocs.pop();
			  return ARGUMENTS(ext);
			}
| '"'		{ Position start = getLexemeStart();
			  errLocs.push(start); 
			  stringBuffer.setLength(0);
			  string(true);
			  startLoc = start;
			  errLocs.pop();
			  return MLSTRING(stringBuffer.toString());
			}
| ident		{ return identOrKeyword(getLexeme()); }
| '['		{ return LSQUARE; }
| ']'		{ return RSQUARE; }
| '<'		{ return LANGLE; }
| '>'		{ return RANGLE; }
| ','		{ return COMMA; }
| ';'		{ return SEMICOL; }
| '.'		{ return DOT; }
| '*'		{ return STAR; }
| '='		{ return EQUAL; }
| '|'		{ return BAR; }
| eof		{ return EOF; }
| _			{ throw error("Unfinished token"); }

/**
 * Matches multi-line comments, both in and outside Java actions
 */
private {void} rule comment =
| "*/"		{ return; }
| "*"		{ continue comment; }
| eof		{ throw error("Unterminated comment"); }
| nl		{ newline(); continue comment; }
| orelse	{ continue comment; }

/**
 * Matches string literals, both in and outside Java actions
 *
 * Parameter [multi] describes whether multi-line string literals
 * are allowed in the calling context. This is used to forbid line
 * terminators in literals in Java actions and allow them elsewhere.
 */
private {void} rule string{boolean multi} =
| '"'		{ return; }
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
 * Matches Java actions, i.e. Java snippets delimited by '{ .. }'
 */
private {PExtent} rule action{PExtent.Builder builder} =
| '{'		{ ++braceDepth; continue action; }
| '}'		{ --braceDepth;
			  if (braceDepth == 0)
			  	return builder.build(getLexemeStart().offset - 1);
			  continue action;
			}
| hole		{ Position p = getLexemeStart();
			  builder.addHole(p.offset, hole_name, p.line, p.column()); 
			  continue action;
			}
| '"'		{ errLocs.push(getLexemeStart());
			  stringBuffer.setLength(0);
			  string(false);	// Java string literals are single-line
			  stringBuffer.setLength(0);
			  errLocs.pop();
			  continue action;
			}
| "'"		{ errLocs.push(getLexemeStart());
			  character();
			  errLocs.pop();
			  continue action;
			}
| "/*"		{ comment(); continue action; }
| slcomment { continue action; }
| eof		{ throw error("Unterminated action"); }
| nl		{ newline(); continue action; }
| orelse    { continue action; }
| _         { continue action; }

/**
 * Matches Java arguments, i.e. Java snippets delimited by '( .. )'
 */
private {PExtent} rule arguments{PExtent.Builder builder} =
| '('		{ ++parenDepth; continue arguments; }
| ')'		{ --parenDepth;
			  if (parenDepth == 0)
			  	return builder.build(getLexemeStart().offset - 1);
			  continue arguments;
			}
| hole		{ Position p = getLexemeStart();
			  builder.addHole(p.offset, hole_name, p.line, p.column()); 
			  continue arguments;
			}
| '"'		{ errLocs.push(getLexemeStart());
			  stringBuffer.setLength(0);
			  string(false);	// Java string literals are single-line
			  stringBuffer.setLength(0);
			  errLocs.pop();
			  continue arguments;
			}
| "'"		{ errLocs.push(getLexemeStart());
			  character();
			  errLocs.pop();
			  continue arguments;
			}
| "/*"		{ comment(); continue arguments; }
| slcomment { continue arguments; }
| eof		{ throw error("Unterminated arguments"); }
| nl		{ newline(); continue arguments; }
| orelse	{ continue arguments; }
| _         { continue arguments; }

/**
 * Matches the contents of a character literal in a Java action
 */
private { char } rule character =
| "'"			{ throw error("Invalid character literal"); }
| ([^ '\r' '\n' '\\'] as c)
				{ characterClose(); return c; }
| '\\'			{ errLocs.push(getLexemeStart());
				  char c = escapeSequence();
				  errLocs.pop();
				  characterClose();
				  return c;
				}
| (nl | eof)
				{ throw error("Unterminated character literal"); }

/**
 * Matches the closing quote of a character literal in a Java action
 *
 * This separate rule is necessary to be able to factor character escape sequences
 * between string and character literals.
 */
private { void } rule characterClose =
| "'"			{ return; }
| (_ | eof)	{ throw error("Unterminated character literal"); }

// JGLexer class footer
{ }