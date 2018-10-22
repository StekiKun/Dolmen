package test.examples;
import test.examples.ArithGroundParser.Token;
import static test.examples.ArithGroundParser.Token.*;

/**
 * Lexer generated by Dolmen 1.0.0
 */
public final class ArithGroundLexer extends codegen.LexBuffer {
    
     
    
    /**
     * Returns a fresh lexer based on the given character stream
     * @param inputname
     * @param reader
     */
    public ArithGroundLexer(String inputname, java.io.Reader reader) {
        super(inputname, reader);
    }
    
    /**
     * Entry point for rule main
     */
    public Token main() {
        main:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell0();
            endToken();
            switch (result) {
            case 0:  {
                 continue main; 
            }
            case 1:  {
                 return INT(Integer.parseInt(getLexeme())); 
            }
            case 2:  {
                 return PLUS; 
            }
            case 3:  {
                 return TIMES; 
            }
            case 4:  {
                 return MINUS; 
            }
            case 5:  {
                 return EOF; 
            }
            default:
                break main;
            }
        }
        throw error("Empty token");
    }
    
    private int _jl_cell0() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0x0008-0x000a0x000d0x0020]
        case 8:
        case 9:
        case 10:
        case 13:
        case 32: {
            return _jl_cell6();
        }
        // 0x002a
        case 42: {
            return _jl_cell3();
        }
        // 0x002b
        case 43: {
            return _jl_cell4();
        }
        // \-
        case 45: {
            return _jl_cell2();
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
            return _jl_cell5();
        }
        // EOF
        case 65535: {
            return _jl_cell1();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell1() {
        return 5;
    }
    
    private int _jl_cell2() {
        return 4;
    }
    
    private int _jl_cell3() {
        return 3;
    }
    
    private int _jl_cell4() {
        return 2;
    }
    
    private int _jl_cell5() {
        while (true) {
            mark(1);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
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
    
    private int _jl_cell6() {
        while (true) {
            mark(0);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x0008-0x000a0x000d0x0020]
            case 8:
            case 9:
            case 10:
            case 13:
            case 32: {
                continue;
            }
            default:  {
                return rewind();
            }
            }
        }
    }
    
     
    
}
