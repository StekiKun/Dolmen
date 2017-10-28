import static test.examples.JSonLWParser.Token.*;
import test.examples.JSonLWParser.Token;

{
	private final StringBuilder buf = new StringBuilder();
	
	private static char escapedChar(char c) {
		switch (c) {
		case '"': return '"';
		case '\\': return '\\';
		case '/': return '/';
		case 'b': return '\b';
		case 'f': return '\f';
		case 'n': return '\n';
		case 'r': return '\r';
		case 't': return '\t';
		default: return c;
		}
	}
}

ws = [' ' '\t' '\b']+;
nl = '\n' | "\r\n";
escaped = '"' | '\\' | '/' | 'b' | 'f' | 'n' | 'r' | 't';

// Reg-exp fragments for number literals
digit = ['0'-'9'];
int = '-'? (digit | (['1'-'9'] digit+));
e = "e" | "e+" | "e-" | "E" | "E+" | "E-";
exp = e digit+;
frac = '.' digit+;
number = int frac? exp?;

// Hex-digit are used in \uxxxx sequences
hex = ['0'-'9''a'-'f''A'-'F'];

public { Token } rule main =
| ws		{ return main(); }
| nl		{ newline(); return main(); }
| '{'		{ return LBRACKET; }
| '}'		{ return RBRACKET; }
| ','		{ return COMMA; }
| ':'		{ return COLON; }
| '['		{ return LSQUARE; }
| ']'		{ return RSQUARE; }
| "true"	{ return TRUE; }
| "false"	{ return FALSE; }
| "null"	{ return NULL; }
| '"'		{ 
              Position stringStart = getLexemeStart();
			  buf.setLength(0); string();
			  startLoc = stringStart;
			  return STRING(buf.toString());
			}
| number	{ return NUMBER(getLexeme()); }
| eof		{ return EOF; }

private { void } rule string =
| '"'		{ return; }
| '\\' (escaped as c)
			{ 
			  buf.append(escapedChar(c));
			  string();
			  return;
			}
| '\\' 'u'
			{ 
			  char c = hexUnicode(); 
			  buf.append(c);
			  string();
			  return;
			}
| '\\' (_ as c)
			{ throw error("Unknown escape sequence: " + c); }
| eof		{ throw error("Unterminated string"); }
| orelse	{ 
			  buf.append(getLexeme());
			  string();
			  return;
			}

private { char} rule hexUnicode =
| hex hex hex hex
			{ return (char)(Integer.parseInt(getLexeme(), 16)); }
| ""		{ throw error("Illegal \\u Unicode sequence"); }

{ }