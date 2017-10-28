package test.examples;
import static test.examples.JSonLWParser.Token.*;
import test.examples.JSonLWParser.Token;
/**
 * Lexer generated by Dolmen 
 */
@org.eclipse.jdt.annotation.NonNullByDefault({})
public final class JSonLWLexer extends codegen.LexBuffer {
    
    
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

    
    /**
     * Returns a fresh lexer based on the given character stream
     * @param inputname
     * @param reader
     */
    public JSonLWLexer(String inputname, java.io.Reader reader) {
        super(inputname, reader);
    }
    
    /**
     * Entry point for rule main
     */
    public  Token  main() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell0();
        endToken();
        switch (result) {
        case 0:  {
             return main(); 
        }
        case 1:  {
             newline(); return main(); 
        }
        case 2:  {
             return LBRACKET; 
        }
        case 3:  {
             return RBRACKET; 
        }
        case 4:  {
             return COMMA; 
        }
        case 5:  {
             return COLON; 
        }
        case 6:  {
             return LSQUARE; 
        }
        case 7:  {
             return RSQUARE; 
        }
        case 8:  {
             return TRUE; 
        }
        case 9:  {
             return FALSE; 
        }
        case 10:  {
             return NULL; 
        }
        case 11:  {
             
              Position stringStart = getLexemeStart();
			  buf.setLength(0); string();
			  startLoc = stringStart;
			  return STRING(buf.toString());
			
        }
        case 12:  {
             return NUMBER(getLexeme()); 
        }
        case 13:  {
             return EOF; 
        }
        default:
            throw error("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule string
     */
    private  void  string() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell35();
        endToken();
        switch (result) {
        case 0:  {
             return; 
        }
        case 1:  {
            final char c = getSubLexemeChar(startPos + 1);
             
			  buf.append(escapedChar(c));
			  string();
			  return;
			
        }
        case 2:  {
             
			  char c = hexUnicode(); 
			  buf.append(c);
			  string();
			  return;
			
        }
        case 3:  {
            final char c = getSubLexemeChar(startPos + 1);
             throw error("Unknown escape sequence: " + c); 
        }
        case 4:  {
             throw error("Unterminated string"); 
        }
        case 5:  {
             
			  buf.append(getLexeme());
			  string();
			  return;
			
        }
        default:
            throw error("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule hexUnicode
     */
    private  char hexUnicode() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell43();
        endToken();
        switch (result) {
        case 0:  {
             return (char)(Integer.parseInt(getLexeme(), 16)); 
        }
        case 1:  {
             throw error("Illegal \\u Unicode sequence"); 
        }
        default:
            throw error("Empty token");
        }
        
    }
    
    private int _jl_cell0() {
        switch (getNextChar()) {
        // [0x0008-0x00090x0020]
        case 8:
        case 9:
        case 32: {
            return _jl_cell17();
        }
        // 0x000a
        case 10: {
            return _jl_cell16();
        }
        // 0x000d
        case 13: {
            return _jl_cell15();
        }
        // 0x0022
        case 34: {
            return _jl_cell4();
        }
        // 0x002c
        case 44: {
            return _jl_cell12();
        }
        // \-
        case 45: {
            return _jl_cell3();
        }
        // 0
        case 48: {
            return _jl_cell1();
        }
        // [1-9]
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell2();
        }
        // 0x003a
        case 58: {
            return _jl_cell11();
        }
        // \[
        case 91: {
            return _jl_cell10();
        }
        // \]
        case 93: {
            return _jl_cell9();
        }
        // f
        case 102: {
            return _jl_cell6();
        }
        // n
        case 110: {
            return _jl_cell5();
        }
        // t
        case 116: {
            return _jl_cell8();
        }
        // 0x007b
        case 123: {
            return _jl_cell14();
        }
        // 0x007d
        case 125: {
            return _jl_cell13();
        }
        // 0xffff
        case 65535: {
            return _jl_cell7();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell1() {
        mark(12);
        switch (getNextChar()) {
        // 0x002e
        case 46: {
            return _jl_cell28();
        }
        // E
        case 69: {
            return _jl_cell30();
        }
        // e
        case 101: {
            return _jl_cell31();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell2() {
        mark(12);
        switch (getNextChar()) {
        // 0x002e
        case 46: {
            return _jl_cell28();
        }
        // [0-9]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell29();
        }
        // E
        case 69: {
            return _jl_cell30();
        }
        // e
        case 101: {
            return _jl_cell31();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell3() {
        switch (getNextChar()) {
        // 0
        case 48: {
            return _jl_cell1();
        }
        // [1-9]
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell2();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell4() {
        return 11;
    }
    
    private int _jl_cell5() {
        switch (getNextChar()) {
        // u
        case 117: {
            return _jl_cell25();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell6() {
        switch (getNextChar()) {
        // a
        case 97: {
            return _jl_cell21();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell7() {
        return 13;
    }
    
    private int _jl_cell8() {
        switch (getNextChar()) {
        // r
        case 114: {
            return _jl_cell18();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell9() {
        return 7;
    }
    
    private int _jl_cell10() {
        return 6;
    }
    
    private int _jl_cell11() {
        return 5;
    }
    
    private int _jl_cell12() {
        return 4;
    }
    
    private int _jl_cell13() {
        return 3;
    }
    
    private int _jl_cell14() {
        return 2;
    }
    
    private int _jl_cell15() {
        switch (getNextChar()) {
        // 0x000a
        case 10: {
            return _jl_cell16();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell16() {
        return 1;
    }
    
    private int _jl_cell17() {
        while (true) {
            mark(0);
            switch (getNextChar()) {
            // [0x0008-0x00090x0020]
            case 8:
            case 9:
            case 32: {
                continue;
            }
            default:  {
                return rewind();
                
            }
            }
            
        }
    }
    
    private int _jl_cell18() {
        switch (getNextChar()) {
        // u
        case 117: {
            return _jl_cell19();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell19() {
        switch (getNextChar()) {
        // e
        case 101: {
            return _jl_cell20();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell20() {
        return 8;
    }
    
    private int _jl_cell21() {
        switch (getNextChar()) {
        // l
        case 108: {
            return _jl_cell22();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell22() {
        switch (getNextChar()) {
        // s
        case 115: {
            return _jl_cell23();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell23() {
        switch (getNextChar()) {
        // e
        case 101: {
            return _jl_cell24();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell24() {
        return 9;
    }
    
    private int _jl_cell25() {
        switch (getNextChar()) {
        // l
        case 108: {
            return _jl_cell26();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell26() {
        switch (getNextChar()) {
        // l
        case 108: {
            return _jl_cell27();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell27() {
        return 10;
    }
    
    private int _jl_cell28() {
        switch (getNextChar()) {
        // [0-9]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell34();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell29() {
        while (true) {
            mark(12);
            switch (getNextChar()) {
            // 0x002e
            case 46: {
                return _jl_cell28();
            }
            // [0-9]
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57: {
                continue;
            }
            // E
            case 69: {
                return _jl_cell30();
            }
            // e
            case 101: {
                return _jl_cell31();
            }
            default:  {
                return rewind();
                
            }
            }
            
        }
    }
    
    private int _jl_cell30() {
        switch (getNextChar()) {
        // 0x002b
        case 43: {
            return _jl_cell33();
        }
        // \-
        case 45: {
            return _jl_cell33();
        }
        // [0-9]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell32();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell31() {
        switch (getNextChar()) {
        // 0x002b
        case 43: {
            return _jl_cell33();
        }
        // \-
        case 45: {
            return _jl_cell33();
        }
        // [0-9]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell32();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell32() {
        while (true) {
            mark(12);
            switch (getNextChar()) {
            // [0-9]
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57: {
                continue;
            }
            default:  {
                return rewind();
                
            }
            }
            
        }
    }
    
    private int _jl_cell33() {
        switch (getNextChar()) {
        // [0-9]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57: {
            return _jl_cell32();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell34() {
        while (true) {
            mark(12);
            switch (getNextChar()) {
            // [0-9]
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57: {
                continue;
            }
            // E
            case 69: {
                return _jl_cell30();
            }
            // e
            case 101: {
                return _jl_cell31();
            }
            default:  {
                return rewind();
                
            }
            }
            
        }
    }
    
    private int _jl_cell35() {
        switch (getNextChar()) {
        // 0x0022
        case 34: {
            return _jl_cell36();
        }
        // \\
        case 92: {
            return _jl_cell39();
        }
        // 0xffff
        case 65535: {
            return _jl_cell38();
        }
        default:  {
            return _jl_cell37();
        }
        }
        
    }
    
    private int _jl_cell36() {
        return 0;
    }
    
    private int _jl_cell37() {
        while (true) {
            mark(5);
            switch (getNextChar()) {
            // [0x0022\\0xffff]
            case 34:
            case 92:
            case 65535: {
                return rewind();
                
            }
            default:  {
                continue;
            }
            }
            
        }
    }
    
    private int _jl_cell38() {
        return 4;
    }
    
    private int _jl_cell39() {
        switch (getNextChar()) {
        // [0x00220x002f\\bfnrt]
        case 34:
        case 47:
        case 92:
        case 98:
        case 102:
        case 110:
        case 114:
        case 116: {
            return _jl_cell42();
        }
        // u
        case 117: {
            return _jl_cell41();
        }
        // 0xffff
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell40();
        }
        }
        
    }
    
    private int _jl_cell40() {
        return 3;
    }
    
    private int _jl_cell41() {
        return 2;
    }
    
    private int _jl_cell42() {
        return 1;
    }
    
    private int _jl_cell43() {
        mark(1);
        switch (getNextChar()) {
        // [0-9A-Fa-f]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 97:
        case 98:
        case 99:
        case 100:
        case 101:
        case 102: {
            return _jl_cell44();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell44() {
        switch (getNextChar()) {
        // [0-9A-Fa-f]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 97:
        case 98:
        case 99:
        case 100:
        case 101:
        case 102: {
            return _jl_cell45();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell45() {
        switch (getNextChar()) {
        // [0-9A-Fa-f]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 97:
        case 98:
        case 99:
        case 100:
        case 101:
        case 102: {
            return _jl_cell46();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell46() {
        switch (getNextChar()) {
        // [0-9A-Fa-f]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 97:
        case 98:
        case 99:
        case 100:
        case 101:
        case 102: {
            return _jl_cell47();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell47() {
        return 0;
    }
    
     
    
}
