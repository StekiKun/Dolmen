import test.examples.ArithGroundParser.Token;
import static test.examples.ArithGroundParser.Token.*;
{ }

digit = ['0'-'9'];
integer = digit+;
ws = [' ' '\t' '\b' '\n' '\r'];

public {Token} rule main =
| ws+	{ continue main; }
| integer { return INT(Integer.parseInt(getLexeme())); }
| '+'   { return PLUS; }
| '*'   { return TIMES; }
| '-'   { return MINUS; }
| eof   { return EOF; }

{ }