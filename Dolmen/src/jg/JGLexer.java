package jg;
import static jg.JGParserGenerated.Token.*;
import jg.JGParserGenerated.Token;

/**
 * Lexer generated by Dolmen 1.0.0
 */
public final class JGLexer extends codegen.LexBuffer {
    
    
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
    public JGLexer(String inputname, java.io.Reader reader) {
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
                 return main(); 
            }
            case 1:  {
                 newline(); return main(); 
            }
            case 2:  {
                 comment(); return main(); 
            }
            case 3:  {
                 return main(); 
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
                 return identOrKeyword(getLexeme()); 
            }
            case 7:  {
                 return SEMICOL; 
            }
            case 8:  {
                 return DOT; 
            }
            case 9:  {
                 return EQUAL; 
            }
            case 10:  {
                 return BAR; 
            }
            case 11:  {
                 return EOF; 
            }
            case 12:  {
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
            int result = _jl_cell16();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
            }
            case 1:  {
                 comment(); return; 
            }
            case 2:  {
                 throw error("Unterminated comment"); 
            }
            case 3:  {
                 newline(); comment(); return; 
            }
            case 4:  {
                 comment(); return; 
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
            int result = _jl_cell23();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
            }
            case 1:  {
                final char c = getSubLexemeChar(startPos + 1);
                 stringBuffer.append(forBackslash(c));
			  string(); return; 
			
            }
            case 2:  {
                final char c = getSubLexemeChar(startPos + 1);
                 stringBuffer.append('\\').append(c);
			  string(); return; 
			
            }
            case 3:  {
                 throw error("Unterminated escape sequence in string literal"); 
            }
            case 4:  {
                 throw error("Unterminated string literal"); 
            }
            case 5:  {
                 stringBuffer.append(getLexeme()); 
			  string(); return; 
			
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
            int result = _jl_cell31();
            endToken();
            switch (result) {
            case 0:  {
                 ++braceDepth; return action(); 
            }
            case 1:  {
                 --braceDepth;
			  if (braceDepth == 0) return getLexemeStart().offset - 1;
			  return action();
			
            }
            case 2:  {
                 stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  return action();
			
            }
            case 3:  {
                 skipChar(); return action(); 
            }
            case 4:  {
                 comment(); return action(); 
            }
            case 5:  {
                 return action(); 
            }
            case 6:  {
                 throw error("Unterminated action"); 
            }
            case 7:  {
                 newline(); return action(); 
            }
            case 8:  {
                 return action(); 
            }
            case 9:  {
                 return action(); 
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
            int result = _jl_cell43();
            endToken();
            switch (result) {
            case 0:  {
                 ++parenDepth; return arguments(); 
            }
            case 1:  {
                 --parenDepth;
			  if (parenDepth == 0) return getLexemeStart().offset - 1;
			  return arguments();
			
            }
            case 2:  {
                 stringBuffer.setLength(0);
			  string();
			  stringBuffer.setLength(0);
			  return arguments();
			
            }
            case 3:  {
                 skipChar(); return arguments(); 
            }
            case 4:  {
                 comment(); return arguments(); 
            }
            case 5:  {
                 return arguments(); 
            }
            case 6:  {
                 throw error("Unterminated arguments"); 
            }
            case 7:  {
                 newline(); return arguments(); 
            }
            case 8:  {
                 return arguments(); 
            }
            case 9:  {
                 return arguments(); 
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
            int result = _jl_cell55();
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
                return _jl_cell13();
            }
            // 0x000a
            case 10: {
                return _jl_cell11();
            }
            // 0x000d
            case 13: {
                return _jl_cell12();
            }
            // 0x0028
            case 40: {
                return _jl_cell8();
            }
            // 0x002e
            case 46: {
                return _jl_cell5();
            }
            // 0x002f
            case 47: {
                return _jl_cell10();
            }
            default:  {
                return _jl_cell1();
            }
            }
        } else  {
            if (_jl_char <= 94) {
                switch (_jl_char) {
                // 0x003b
                case 59: {
                    return _jl_cell6();
                }
                // [0x003c0x003e-0x0040\[-\^]
                case 60:
                case 62:
                case 63:
                case 64:
                case 91:
                case 92:
                case 93:
                case 94: {
                    return _jl_cell1();
                }
                // 0x003d
                case 61: {
                    return _jl_cell4();
                }
                default:  {
                    return _jl_cell7();
                }
                }
            } else  {
                if (_jl_char <= 122) {
                    switch (_jl_char) {
                    // 0x0060
                    case 96: {
                        return _jl_cell1();
                    }
                    default:  {
                        return _jl_cell7();
                    }
                    }
                } else  {
                    switch (_jl_char) {
                    // 0x007b
                    case 123: {
                        return _jl_cell9();
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
        return 12;
    }
    
    private int _jl_cell2() {
        return 11;
    }
    
    private int _jl_cell3() {
        return 10;
    }
    
    private int _jl_cell4() {
        return 9;
    }
    
    private int _jl_cell5() {
        return 8;
    }
    
    private int _jl_cell6() {
        return 7;
    }
    
    private int _jl_cell7() {
        while (true) {
            mark(6);
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
    
    private int _jl_cell8() {
        return 5;
    }
    
    private int _jl_cell9() {
        return 4;
    }
    
    private int _jl_cell10() {
        mark(12);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002a
        case 42: {
            return _jl_cell15();
        }
        // 0x002f
        case 47: {
            return _jl_cell14();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell11() {
        return 1;
    }
    
    private int _jl_cell12() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell11();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell13() {
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
    
    private int _jl_cell14() {
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
    
    private int _jl_cell15() {
        return 2;
    }
    
    private int _jl_cell16() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell18();
        }
        // 0x000d
        case 13: {
            return _jl_cell19();
        }
        // 0x002a
        case 42: {
            return _jl_cell21();
        }
        // EOF
        case 65535: {
            return _jl_cell20();
        }
        default:  {
            return _jl_cell17();
        }
        }
    }
    
    private int _jl_cell17() {
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
    
    private int _jl_cell18() {
        return 3;
    }
    
    private int _jl_cell19() {
        mark(3);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell18();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell20() {
        return 2;
    }
    
    private int _jl_cell21() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002f
        case 47: {
            return _jl_cell22();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell22() {
        return 0;
    }
    
    private int _jl_cell23() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x0022
        case 34: {
            return _jl_cell24();
        }
        // \\
        case 92: {
            return _jl_cell27();
        }
        // EOF
        case 65535: {
            return _jl_cell26();
        }
        default:  {
            return _jl_cell25();
        }
        }
    }
    
    private int _jl_cell24() {
        return 0;
    }
    
    private int _jl_cell25() {
        while (true) {
            mark(5);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x0022\\EOF]
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
    
    private int _jl_cell26() {
        return 4;
    }
    
    private int _jl_cell27() {
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
            return _jl_cell30();
        }
        // EOF
        case 65535: {
            return _jl_cell28();
        }
        default:  {
            return _jl_cell29();
        }
        }
    }
    
    private int _jl_cell28() {
        return 3;
    }
    
    private int _jl_cell29() {
        return 2;
    }
    
    private int _jl_cell30() {
        return 1;
    }
    
    private int _jl_cell31() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell38();
        }
        // 0x000d
        case 13: {
            return _jl_cell39();
        }
        // 0x0022
        case 34: {
            return _jl_cell34();
        }
        // 0x0027
        case 39: {
            return _jl_cell33();
        }
        // 0x002f
        case 47: {
            return _jl_cell32();
        }
        // 0x007b
        case 123: {
            return _jl_cell36();
        }
        // 0x007d
        case 125: {
            return _jl_cell35();
        }
        // EOF
        case 65535: {
            return _jl_cell40();
        }
        default:  {
            return _jl_cell37();
        }
        }
    }
    
    private int _jl_cell32() {
        mark(9);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002a
        case 42: {
            return _jl_cell41();
        }
        // 0x002f
        case 47: {
            return _jl_cell42();
        }
        default:  {
            return rewind();
        }
        }
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
        return 0;
    }
    
    private int _jl_cell37() {
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
    
    private int _jl_cell38() {
        return 7;
    }
    
    private int _jl_cell39() {
        mark(7);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell38();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell40() {
        return 6;
    }
    
    private int _jl_cell41() {
        return 4;
    }
    
    private int _jl_cell42() {
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
    
    private int _jl_cell43() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell49();
        }
        // 0x000d
        case 13: {
            return _jl_cell50();
        }
        // 0x0022
        case 34: {
            return _jl_cell45();
        }
        // 0x0027
        case 39: {
            return _jl_cell44();
        }
        // 0x0028
        case 40: {
            return _jl_cell47();
        }
        // 0x0029
        case 41: {
            return _jl_cell46();
        }
        // 0x002f
        case 47: {
            return _jl_cell52();
        }
        // EOF
        case 65535: {
            return _jl_cell51();
        }
        default:  {
            return _jl_cell48();
        }
        }
    }
    
    private int _jl_cell44() {
        return 3;
    }
    
    private int _jl_cell45() {
        return 2;
    }
    
    private int _jl_cell46() {
        return 1;
    }
    
    private int _jl_cell47() {
        return 0;
    }
    
    private int _jl_cell48() {
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
    
    private int _jl_cell49() {
        return 7;
    }
    
    private int _jl_cell50() {
        mark(7);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell49();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell51() {
        return 6;
    }
    
    private int _jl_cell52() {
        mark(9);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x002a
        case 42: {
            return _jl_cell53();
        }
        // 0x002f
        case 47: {
            return _jl_cell54();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell53() {
        return 4;
    }
    
    private int _jl_cell54() {
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
    
    private int _jl_cell55() {
        mark(2);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0x0027EOF]
        case 39:
        case 65535: {
            return rewind();
        }
        // \\
        case 92: {
            return _jl_cell56();
        }
        default:  {
            return _jl_cell57();
        }
        }
    }
    
    private int _jl_cell56() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // EOF
        case 65535: {
            return rewind();
        }
        default:  {
            return _jl_cell59();
        }
        }
    }
    
    private int _jl_cell57() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x0027
        case 39: {
            return _jl_cell58();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell58() {
        return 0;
    }
    
    private int _jl_cell59() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x0027
        case 39: {
            return _jl_cell60();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell60() {
        return 1;
    }
    
     
    
}
