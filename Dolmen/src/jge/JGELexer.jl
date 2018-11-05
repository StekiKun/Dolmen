/**
 * Lexer description for extended Dolmen grammar descriptions ('.jg' files)
 */
import static jge.JGEParser.Token.*;
import jge.JGEParser.Token;
import syntax.PExtent;

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
}

// Auxiliary definitions for lexer rules
ws = [' ' '\t' '\b'];
digit = ['0'-'9'];
nzdigit = ['1'-'9'];

lalpha = ['a'-'z'];
ualpha = ['A'-'Z'];
idstart = '_' | lalpha | ualpha;
idbody = idstart | digit;

nl = "\r" | '\012' | "\015\n";
notnl = [^ '\r' '\n'];
ident = idstart idbody*;
escaped = ['\\' '\'' '"' 'n' 't' 'b' 'r' ' '];
slcomment = "//" notnl*;

hole = '#' (lalpha idbody* as hole_name);

// Lexer rules
public {Token} rule main =
| ws+		{ continue main; }
| nl		{ newline(); continue main; }
| "/*"		{ comment(); continue main; }
| slcomment	{ continue main; }
| '{'		{ braceDepth = 1;
              Position start = getLexemeStart();
			  Position p = getLexemeEnd();
			  PExtent ext = action(new PExtent.Builder(filename, p.offset, p.line, p.column()));
              startLoc = start;
			  return ACTION(ext);
			}
| '('		{ parenDepth = 1;
              Position start = getLexemeStart();
			  Position p = getLexemeEnd();
			  PExtent ext = arguments(new PExtent.Builder(filename, p.offset, p.line, p.column()));
			  startLoc = start;
			  return ARGUMENTS(ext);
			}
| '"'		{ Position start = getLexemeStart(); 
			  stringBuffer.setLength(0);
			  string();
			  startLoc = start;
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
| '='		{ return EQUAL; }
| '|'		{ return BAR; }
| eof		{ return EOF; }
| _			{ throw error("Unfinished token"); }

private {void} rule comment =
| "*/"		{ return; }
| "*"		{ continue comment; }
| eof		{ throw error("Unterminated comment"); }
| nl		{ newline(); continue comment; }
| orelse	{ continue comment; }

private {void} rule string =
| '"'		{ return; }
| '\\' (escaped as c)
			{ stringBuffer.append(forBackslash(c));
			  continue string;
			}
| '\\' (_ as c)
			{ stringBuffer.append('\\').append(c);
			  continue string; 
			}
| '\\' eof  { throw error("Unterminated escape sequence in string literal"); }
| nl		{ newline(); 
			  stringBuffer.append(getLexeme());
			  continue string; }
| eof 		{ throw error("Unterminated string literal"); }
| orelse	{ stringBuffer.append(getLexeme()); 
			  continue string; 
			}

private {PExtent} rule action{PExtent.Builder builder} =
| '{'		{ ++braceDepth; continue action; }
| '}'		{ --braceDepth;
			  if (braceDepth == 0)
			  	return builder.build(getLexemeStart().offset - 1);
			  continue action;
			}
| hole		{ builder.addHole(getLexemeStart().offset, hole_name); 
			  continue action;
			}
| '"'		{ stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  continue action;
			}
| "'"		{ skipChar(); continue action; }
| "/*"		{ comment(); continue action; }
| slcomment { continue action; }
| eof		{ throw error("Unterminated action"); }
| nl		{ newline(); continue action; }
| orelse    { continue action; }
| _         { continue action; }

private {PExtent} rule arguments{PExtent.Builder builder} =
| '('		{ ++parenDepth; continue arguments; }
| ')'		{ --parenDepth;
			  if (parenDepth == 0)
			  	return builder.build(getLexemeStart().offset - 1);
			  continue arguments;
			}
| hole		{ builder.addHole(getLexemeStart().offset, hole_name);
			  continue arguments;
			}
| '"'		{ stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  continue arguments;
			}
| "'"		{ skipChar(); continue arguments; }
| "/*"		{ comment(); continue arguments; }
| slcomment { continue arguments; }
| eof		{ throw error("Unterminated arguments"); }
| nl		{ newline(); continue arguments; }
| orelse	{ continue arguments; }
| _         { continue arguments; }

private {void} rule skipChar =
| (notnl # ['\\' '\'']) "'"
			{ return; }
| '\\' _ "'"
			{ return; }
// Do not jeopardize everything for a syntax error in a Java action
| ""		{ return; }

// JGLexer class footer
{ }