package jge;
import static jge.JGEParser.Token.*;
import jge.JGEParser.Token;

/**
 * Lexer generated by Dolmen 1.0.0
 */
public final class JGELexer extends codegen.LexBuffer {
    
    
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

    
    /**
     * Returns a fresh lexer based on the given character stream
     * @param inputname
     * @param reader
     */
    public JGELexer(String inputname, java.io.Reader reader) {
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
                 newline(); continue main; 
            }
            case 2:  {
                 comment(); continue main; 
            }
            case 3:  {
                 continue main; 
            }
            case 4:  {
                 braceDepth = 1;
              Position start = getLexemeStart();
			  Position p = getLexemeEnd();
			  int endOffset = action();
			  syntax.Extent ext = new syntax.Extent(
			  	filename, p.offset, endOffset, p.line, p.column());
              startLoc = start;
			  return ACTION(ext);
			
            }
            case 5:  {
                 parenDepth = 1;
              Position start = getLexemeStart();
			  Position p = getLexemeEnd();
			  int endOffset = arguments();
			  syntax.Extent ext = new syntax.Extent(
			    filename, p.offset, endOffset, p.line, p.column());
			  startLoc = start;
			  return ARGUMENTS(ext);
			
            }
            case 6:  {
                 Position start = getLexemeStart(); 
			  stringBuffer.setLength(0);
			  string();
			  startLoc = start;
			  return STRING(stringBuffer.toString());
			
            }
            case 7:  {
                 return identOrKeyword(getLexeme()); 
            }
            case 8:  {
                 return LSQUARE; 
            }
            case 9:  {
                 return RSQUARE; 
            }
            case 10:  {
                 return SEMICOL; 
            }
            case 11:  {
                 return DOT; 
            }
            case 12:  {
                 return EQUAL; 
            }
            case 13:  {
                 return BAR; 
            }
            case 14:  {
                 return EOF; 
            }
            case 15:  {
                 throw error("Unfinished token"); 
            }
            default:
                break main;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule comment
     */
    private void comment() {
        comment:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell19();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
            }
            case 1:  {
                 continue comment; 
            }
            case 2:  {
                 throw error("Unterminated comment"); 
            }
            case 3:  {
                 newline(); continue comment; 
            }
            case 4:  {
                 continue comment; 
            }
            default:
                break comment;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule string
     */
    private void string() {
        string:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell26();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
            }
            case 1:  {
                final char c = getSubLexemeChar(startPos + 1);
                 stringBuffer.append(forBackslash(c));
			  continue string;
			
            }
            case 2:  {
                final char c = getSubLexemeChar(startPos + 1);
                 stringBuffer.append('\\').append(c);
			  continue string; 
			
            }
            case 3:  {
                 throw error("Unterminated escape sequence in string literal"); 
            }
            case 4:  {
                 newline(); 
			  stringBuffer.append(getLexeme());
			  continue string; 
            }
            case 5:  {
                 throw error("Unterminated string literal"); 
            }
            case 6:  {
                 stringBuffer.append(getLexeme()); 
			  continue string; 
			
            }
            default:
                break string;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule action
     */
    private int action() {
        action:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell36();
            endToken();
            switch (result) {
            case 0:  {
                 ++braceDepth; continue action; 
            }
            case 1:  {
                 --braceDepth;
			  if (braceDepth == 0) return getLexemeStart().offset - 1;
			  continue action;
			
            }
            case 2:  {
                 stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  continue action;
			
            }
            case 3:  {
                 skipChar(); continue action; 
            }
            case 4:  {
                 comment(); continue action; 
            }
            case 5:  {
                 continue action; 
            }
            case 6:  {
                 throw error("Unterminated action"); 
            }
            case 7:  {
                 newline(); continue action; 
            }
            case 8:  {
                 continue action; 
            }
            case 9:  {
                 continue action; 
            }
            default:
                break action;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule arguments
     */
    private int arguments() {
        arguments:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell48();
            endToken();
            switch (result) {
            case 0:  {
                 ++parenDepth; continue arguments; 
            }
            case 1:  {
                 --parenDepth;
			  if (parenDepth == 0) return getLexemeStart().offset - 1;
			  continue arguments;
			
            }
            case 2:  {
                 stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  continue arguments;
			
            }
            case 3:  {
                 skipChar(); continue arguments; 
            }
            case 4:  {
                 comment(); continue arguments; 
            }
            case 5:  {
                 return arguments(); 
            }
            case 6:  {
                 throw error("Unterminated arguments"); 
            }
            case 7:  {
                 newline(); continue arguments; 
            }
            case 8:  {
                 continue arguments; 
            }
            case 9:  {
                 continue arguments; 
            }
            default:
                break arguments;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule skipChar
     */
    private void skipChar() {
        skipChar:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell60();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
            }
            case 1:  {
                 return; 
            }
            case 2:  {
                 return; 
            }
            default:
                break skipChar;
            }
        }
        throw error("Empty token");
    }
    
    private int _jl_cell0() {
        final char _jl_char = getNextChar();
        if (_jl_char <= 58) {
            switch (_jl_char) {
            // [0x0008-0x00090x0020]
            case 8:
            case 9:
            case 32: {
                return _jl_cell16();
            }
            // 0x000a
            case 10: {
                return _jl_cell14();
            }
            // 0x000d
            case 13: {
                return _jl_cell15();
            }
            // 0x0022
            case 34: {
                return _jl_cell10();
            }
            // 0x0028
            case 40: {
                return _jl_cell11();
            }
            // 0x002e
            case 46: {
                return _jl_cell5();
            }
            // 0x002f
            case 47: {
                return _jl_cell13();
            }
            default:  {
                return _jl_cell1();
            }
            }
        } else  {
            if (_jl_char <= 93) {
                switch (_jl_char) {
                // 0x003b
                case 59: {
                    return _jl_cell6();
                }
                // [0x003c0x003e-0x0040\\]
                case 60:
                case 62:
                case 63:
                case 64:
                case 92: {
                    return _jl_cell1();
                }
                // 0x003d
                case 61: {
                    return _jl_cell4();
                }
                // \[
                case 91: {
                    return _jl_cell8();
                }
                // \]
                case 93: {
                    return _jl_cell7();
                }
                default:  {
                    return _jl_cell9();
                }
                }
            } else  {
                if (_jl_char <= 122) {
                    switch (_jl_char) {
                    // [\^0x0060]
                    case 94:
                    case 96: {
                        return _jl_cell1();
                    }
                    default:  {
                        return _jl_cell9();
                    }
                    }
                } else  {
                    switch (_jl_char) {
                    // 0x007b
                    case 123: {
                        return _jl_cell12();
                    }
                    // 0x007c
                    case 124: {
                        return _jl_cell3();
                    }
                    // EOF
                    case 65535: {
                        return _jl_cell2();
                    }
                    default:  {
                        return _jl_cell1();
                    }
                    }
                }
            }
        }
    }
    
    private int _jl_cell1() {
        return 15;
    }
    
    private int _jl_cell2() {
        return 14;
    }
    
    private int _jl_cell3() {
        return 13;
    }
    
    private int _jl_cell4() {
        return 12;
    }
    
    private int _jl_cell5() {
        return 11;
    }
    
    private int _jl_cell6() {
        return 10;
    }
    
    private int _jl_cell7() {
        return 9;
    }
    
    private int _jl_cell8() {
        return 8;
    }
    
    private int _jl_cell9() {
        while (true) {
            mark(7);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0-9A-Z\_a-z]
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
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 95:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122: {
                continue;
            }
            default:  {
                return rewind();
            }
            }
        }
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
        mark(15);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002a
        case 42: {
            return _jl_cell18();
        }
        // 0x002f
        case 47: {
            return _jl_cell17();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell14() {
        return 1;
    }
    
    private int _jl_cell15() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell14();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell16() {
        while (true) {
            mark(0);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
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
    
    private int _jl_cell17() {
        while (true) {
            mark(3);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000dEOF]
            case 10:
            case 13:
            case 65535: {
                return rewind();
            }
            default:  {
                continue;
            }
            }
        }
    }
    
    private int _jl_cell18() {
        return 2;
    }
    
    private int _jl_cell19() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell20();
        }
        // 0x000d
        case 13: {
            return _jl_cell21();
        }
        // 0x002a
        case 42: {
            return _jl_cell23();
        }
        // EOF
        case 65535: {
            return _jl_cell22();
        }
        default:  {
            return _jl_cell24();
        }
        }
    }
    
    private int _jl_cell20() {
        return 3;
    }
    
    private int _jl_cell21() {
        mark(3);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell20();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell22() {
        return 2;
    }
    
    private int _jl_cell23() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002f
        case 47: {
            return _jl_cell25();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell24() {
        while (true) {
            mark(4);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000d0x002aEOF]
            case 10:
            case 13:
            case 42:
            case 65535: {
                return rewind();
            }
            default:  {
                continue;
            }
            }
        }
    }
    
    private int _jl_cell25() {
        return 0;
    }
    
    private int _jl_cell26() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell29();
        }
        // 0x000d
        case 13: {
            return _jl_cell30();
        }
        // 0x0022
        case 34: {
            return _jl_cell32();
        }
        // \\
        case 92: {
            return _jl_cell31();
        }
        // EOF
        case 65535: {
            return _jl_cell28();
        }
        default:  {
            return _jl_cell27();
        }
        }
    }
    
    private int _jl_cell27() {
        while (true) {
            mark(6);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000d0x0022\\EOF]
            case 10:
            case 13:
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
    
    private int _jl_cell28() {
        return 5;
    }
    
    private int _jl_cell29() {
        return 4;
    }
    
    private int _jl_cell30() {
        mark(4);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell29();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell31() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0x00200x00220x0027\\bnrt]
        case 32:
        case 34:
        case 39:
        case 92:
        case 98:
        case 110:
        case 114:
        case 116: {
            return _jl_cell35();
        }
        // EOF
        case 65535: {
            return _jl_cell33();
        }
        default:  {
            return _jl_cell34();
        }
        }
    }
    
    private int _jl_cell32() {
        return 0;
    }
    
    private int _jl_cell33() {
        return 3;
    }
    
    private int _jl_cell34() {
        return 2;
    }
    
    private int _jl_cell35() {
        return 1;
    }
    
    private int _jl_cell36() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell39();
        }
        // 0x000d
        case 13: {
            return _jl_cell40();
        }
        // 0x0022
        case 34: {
            return _jl_cell43();
        }
        // 0x0027
        case 39: {
            return _jl_cell42();
        }
        // 0x002f
        case 47: {
            return _jl_cell41();
        }
        // 0x007b
        case 123: {
            return _jl_cell45();
        }
        // 0x007d
        case 125: {
            return _jl_cell44();
        }
        // EOF
        case 65535: {
            return _jl_cell37();
        }
        default:  {
            return _jl_cell38();
        }
        }
    }
    
    private int _jl_cell37() {
        return 6;
    }
    
    private int _jl_cell38() {
        while (true) {
            mark(8);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000d0x00220x00270x002f0x007b0x007dEOF]
            case 10:
            case 13:
            case 34:
            case 39:
            case 47:
            case 123:
            case 125:
            case 65535: {
                return rewind();
            }
            default:  {
                continue;
            }
            }
        }
    }
    
    private int _jl_cell39() {
        return 7;
    }
    
    private int _jl_cell40() {
        mark(7);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell39();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell41() {
        mark(9);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002a
        case 42: {
            return _jl_cell47();
        }
        // 0x002f
        case 47: {
            return _jl_cell46();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell42() {
        return 3;
    }
    
    private int _jl_cell43() {
        return 2;
    }
    
    private int _jl_cell44() {
        return 1;
    }
    
    private int _jl_cell45() {
        return 0;
    }
    
    private int _jl_cell46() {
        while (true) {
            mark(5);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000dEOF]
            case 10:
            case 13:
            case 65535: {
                return rewind();
            }
            default:  {
                continue;
            }
            }
        }
    }
    
    private int _jl_cell47() {
        return 4;
    }
    
    private int _jl_cell48() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell51();
        }
        // 0x000d
        case 13: {
            return _jl_cell52();
        }
        // 0x0022
        case 34: {
            return _jl_cell55();
        }
        // 0x0027
        case 39: {
            return _jl_cell54();
        }
        // 0x0028
        case 40: {
            return _jl_cell57();
        }
        // 0x0029
        case 41: {
            return _jl_cell56();
        }
        // 0x002f
        case 47: {
            return _jl_cell53();
        }
        // EOF
        case 65535: {
            return _jl_cell49();
        }
        default:  {
            return _jl_cell50();
        }
        }
    }
    
    private int _jl_cell49() {
        return 6;
    }
    
    private int _jl_cell50() {
        while (true) {
            mark(8);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000d0x00220x0027-0x00290x002fEOF]
            case 10:
            case 13:
            case 34:
            case 39:
            case 40:
            case 41:
            case 47:
            case 65535: {
                return rewind();
            }
            default:  {
                continue;
            }
            }
        }
    }
    
    private int _jl_cell51() {
        return 7;
    }
    
    private int _jl_cell52() {
        mark(7);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell51();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell53() {
        mark(9);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002a
        case 42: {
            return _jl_cell59();
        }
        // 0x002f
        case 47: {
            return _jl_cell58();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell54() {
        return 3;
    }
    
    private int _jl_cell55() {
        return 2;
    }
    
    private int _jl_cell56() {
        return 1;
    }
    
    private int _jl_cell57() {
        return 0;
    }
    
    private int _jl_cell58() {
        while (true) {
            mark(5);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a0x000dEOF]
            case 10:
            case 13:
            case 65535: {
                return rewind();
            }
            default:  {
                continue;
            }
            }
        }
    }
    
    private int _jl_cell59() {
        return 4;
    }
    
    private int _jl_cell60() {
        mark(2);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0x000a0x000d0x0027EOF]
        case 10:
        case 13:
        case 39:
        case 65535: {
            return rewind();
        }
        // \\
        case 92: {
            return _jl_cell61();
        }
        default:  {
            return _jl_cell62();
        }
        }
    }
    
    private int _jl_cell61() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // EOF
        case 65535: {
            return rewind();
        }
        default:  {
            return _jl_cell64();
        }
        }
    }
    
    private int _jl_cell62() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x0027
        case 39: {
            return _jl_cell63();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell63() {
        return 0;
    }
    
    private int _jl_cell64() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x0027
        case 39: {
            return _jl_cell65();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell65() {
        return 1;
    }
    
     
    
}
