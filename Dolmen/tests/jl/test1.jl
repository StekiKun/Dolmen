// Imports
import static org.stekikun.dolmen.jl.JLToken.*;
import org.stekikun.dolmen.jl.JLToken;

/* Lexer class header */
{
	public enum Op {
		PLUS, MINUS, MULT, DIV;
	}

	// Inner single-line comment "
	private int loc = 1;
	/* Inner multi-line comment } } */
	private int bol = 0;
	
	// Inner block
	static {
	  System.out.println("Loaded lexer class");
	}
}

/* Definitions */
lalpha = ['a'-'z'];
ualpha = ['A'-'Z'];
digit =  ['0'-'9'];
istart = lalpha | ualpha | '_';
ibody = istart | digit;
integer = digit+;
ident = istart ibody*;

/** Entries */
public { Object } rule arithExpr =
| ident	{ return getLexeme(); }
| '+'   { return PLUS; }
| '*'   { return MINUS; }
| '-'   { return MINUS; }
| '/'   { return MULT; }
| "/*"  { comment(); return arithExpr(); }
| integer { return Integer.parseInt(getLexeme()); }
| _ as c  { throw new LexicalError(getLexemeStart(), "Unexpected character " + c); }

private { void } rule comment =
| "*/" { return; }
| '*' [^'/'] [^'*']*  { continue comment; }
| orelse              { continue comment; }
| eof				  { throw new LexicalError(getLexemeStart(), "Unterminated comment"); }

{ }