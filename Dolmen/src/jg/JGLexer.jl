/**
 * Lexer description for Dolmen grammar descriptions ('.jg' files)
 */
import static jg.JGParserGenerated.Token.*;
import jg.JGParserGenerated.Token;

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

// Lexer rules
public {Token} rule main =
| ws+		{ return main(); }
| nl		{ newline(); return main(); }
| "/*"		{ comment(); return main(); }
| slcomment	{ return main(); }
| '{'		{ braceDepth = 1;
			  Position p = getLexemeEnd();
			  int endOffset = action();
			  syntax.Extent ext = new syntax.Extent(
			  	filename, p.offset, endOffset, p.line, p.column());
			  return ACTION(ext);
			}
| '('		{ parenDepth = 1;
			  Position p = getLexemeEnd();
			  int endOffset = arguments();
			  syntax.Extent ext = new syntax.Extent(
			    filename, p.offset, endOffset, p.line, p.column());
			  return ARGUMENTS(ext);
			}			
| ident		{ return identOrKeyword(getLexeme()); }
| ';'		{ return SEMICOL; }
| '.'		{ return DOT; }
| '='		{ return EQUAL; }
| '|'		{ return BAR; }
| eof		{ return EOF; }
| _			{ throw error("Unfinished token"); }

private {void} rule comment =
| "*/"		{ return; }
| "*"		{ comment(); return; }
// | '"'		{ stringBuffer.setLength(0);
//  			  string();
//  			  stringBuffer.setLength(0);
//  			  comment(); return;
//  			}
// | "'"		{ skipChar(); comment(); return; }
| eof		{ throw error("Unterminated comment"); }
| nl		{ newline(); comment(); return; }
| orelse	{ comment(); return; }

private {void} rule string =
| '"'		{ return; }
| '\\' (escaped as c)
			{ stringBuffer.append(forBackslash(c));
			  string(); return; 
			}
| '\\' (_ as c)
			{ stringBuffer.append('\\').append(c);
			  string(); return; 
			}
| eof 		{ throw error("Unterminated string literal"); }
| orelse	{ stringBuffer.append(getLexeme()); 
			  string(); return; 
			}

private {int} rule action =
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
| "'"		{ skipChar(); return action(); }
| "/*"		{ comment(); return action(); }
| slcomment { return action(); }
| eof		{ throw error("Unterminated action"); }
| nl		{ newline(); return action(); }
| orelse	{ return action(); }

private {int} rule arguments =
| '('		{ ++parenDepth; return arguments(); }
| ')'		{ --parenDepth;
			  if (parenDepth == 0) return getLexemeStart().offset - 1;
			  return arguments();
			}
| '"'		{ stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  return arguments();
			}
| "'"		{ skipChar(); return arguments(); }
| "/*"		{ comment(); return arguments(); }
| slcomment { return arguments(); }
| eof		{ throw error("Unterminated arguments"); }
| nl		{ newline(); return arguments(); }
| orelse	{ return arguments(); }

private {void} rule skipChar =
| [^ '\\' '\''] "'"
			{ return; }
| '\\' _ "'"
			{ return; }
// Do not jeopardize everything for a syntax error in a Java action
| ""		{ return; }

// JGLexer class footer
{ }