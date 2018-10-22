import static test.examples.JSonPosParser.Token.*;
import test.examples.JSonPosParser.Token;

// Lexical analyzer for non-standard JSON values
//
// It supports extra keywords 'object', 'array' to test
// positions with optional items in the corresponding parser.

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
nl = '\u000A' | "\u000D\uuuu000A";
escaped = '"' | '\\' | '/' | 'b' | 'f' | 'n' | 'r' | 't';

// Reg-exp fragments for number literals
digit = ['0'-'9'];
int = '-'? (digit | (['1'-'9'] digit+));
e = "e" | "e+" | "e-" | "E" | "E+" | "E-";
exp = e digit+;
frac = '.' digit+;
number = int<1,1> frac<0,1> exp<0,1>;

// Hex-digit are used in \uxxxx sequences
hex = ['0'-'9''a'-'f''A'-'F'];

public { Token } rule main =
| ws		{ continue main; }
| nl		{ newline(); continue main; }
| '{'		{ return LBRACKET; }
| '}'		{ return RBRACKET; }
| ','		{ return COMMA; }
| ':'		{ return COLON; }
| '['		{ return LSQUARE; }
| ']'		{ return RSQUARE; }
| "true"<1>	{ return TRUE; }
| "false"	{ return FALSE; }
| "null"	{ return NULL; }
| "object"  { return OBJECT; }
| "array"   { return ARRAY; }
| '"'		{ 
              Position stringStart = getLexemeStart();
			  buf.setLength(0); string();
			  startLoc = stringStart;
			  return STRING(buf.toString());
			}
| number	{ return NUMBER(Double.parseDouble(getLexeme())); }
| eof		{ return EOF; }

private { void } rule string =
| '"'		{ return; }
| '\\' (escaped as c)
			{ 
			  buf.append(escapedChar(c));
			  continue string;
			}
| '\\' 'u'
			{ 
			  char c = hexUnicode(); 
			  buf.append(c);
			  continue string;
			}
| '\\' (_ as c)
			{ throw error("Unknown escape sequence: " + c); }
| eof		{ throw error("Unterminated string"); }
| orelse	{ 
			  buf.append(getLexeme());
			  continue string;
			}

private { char} rule hexUnicode =
| hex<4>
			{ return (char)(Integer.parseInt(getLexeme(), 16)); }
| ""		{ throw error("Illegal \\u Unicode sequence"); }

{ }