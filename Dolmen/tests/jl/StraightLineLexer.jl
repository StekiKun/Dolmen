import test.examples.StraightLineParser.Token;
import static test.examples.StraightLineParser.Token.*;
{ }

digit = ['0'-'9'];
integer = digit+;
ws = [' ' '\t' '\b' '\n' '\r'];
alpha = ['a'-'z' 'A'-'Z'];
istart = alpha | '_';
ibody = istart | digit;
ident = istart ibody*;

public {Token} rule main =
| ws+	{ return main(); }
| integer { return INT(Integer.parseInt(getLexeme())); }
| "print" { return PRINT; }
| ident { return ID(getLexeme()); }
| '+'   { return PLUS; }
| '*'   { return TIMES; }
| '-'   { return MINUS; }
| '/'   { return DIV; }
| ','   { return COMMA; }
| '('   { return LPAREN; }
| ')'   { return RPAREN; }
| ":="  { return ASSIGN; }
| ';'   { return SEMICOLON; }
| eof   { return EOF; }

{ }