package jg;
import static jg.JGParserGenerated.Token.*;
import jg.JGParserGenerated.Token;
/**
 * Lexer generated by Dolmen 
 */
@org.eclipse.jdt.annotation.NonNullByDefault({})
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
	
	private LexicalError error(String msg) {
		Position p = getLexemeStart();
		String res = String.format("%s (line %d, col %d)",
			msg, p.line, p.column());
		return new LexicalError(res);
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
			  Position p = getLexemeEnd();
			  int endOffset = action();
			  syntax.Location loc = new syntax.Location(
			  	filename, p.offset, endOffset, p.line, p.column());
			  return ACTION(loc);
			
        }
        case 5:  {
             parenDepth = 1;
			  Position p = getLexemeEnd();
			  int endOffset = arguments();
			  syntax.Location loc = new syntax.Location(
			    filename, p.offset, endOffset, p.line, p.column());
			  return ARGUMENTS(loc);
			
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
            throw new LexicalError("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule comment
     */
    private void comment() {
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
            throw new LexicalError("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule string
     */
    private void string() {
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
             throw error("Unterminated string literal"); 
        }
        case 4:  {
             stringBuffer.append(getLexeme()); 
			  string(); return; 
			
        }
        default:
            throw new LexicalError("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule action
     */
    private int action() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell30();
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
        default:
            throw new LexicalError("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule arguments
     */
    private int arguments() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell42();
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
        default:
            throw new LexicalError("Empty token");
        }
        
    }
    
    /**
     * Entry point for rule skipChar
     */
    private void skipChar() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell54();
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
            throw new LexicalError("Empty token");
        }
        
    }
    
    private int _jl_cell0() {
        switch (getNextChar()) {
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
        // 0x003b
        case 59: {
            return _jl_cell6();
        }
        // 0x003d
        case 61: {
            return _jl_cell4();
        }
        // [A-Z\_a-z]
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
            return _jl_cell7();
        }
        // 0x007b
        case 123: {
            return _jl_cell9();
        }
        // 0x007c
        case 124: {
            return _jl_cell3();
        }
        // 0xffff
        case 65535: {
            return _jl_cell2();
        }
        default:  {
            return _jl_cell1();
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
            switch (getNextChar()) {
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
        switch (getNextChar()) {
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
        switch (getNextChar()) {
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
    
    private int _jl_cell14() {
        while (true) {
            mark(3);
            switch (getNextChar()) {
            // [0x000a0x000d0xffff]
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
        switch (getNextChar()) {
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
        // 0xffff
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
            switch (getNextChar()) {
            // [0x000a0x000d0x002a0xffff]
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
        switch (getNextChar()) {
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
        switch (getNextChar()) {
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
        switch (getNextChar()) {
        // 0x0022
        case 34: {
            return _jl_cell24();
        }
        // \\
        case 92: {
            return _jl_cell27();
        }
        // 0xffff
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
            mark(4);
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
    
    private int _jl_cell26() {
        return 3;
    }
    
    private int _jl_cell27() {
        switch (getNextChar()) {
        // [0x00200x00220x0027\\bnrt]
        case 32:
        case 34:
        case 39:
        case 92:
        case 98:
        case 110:
        case 114:
        case 116: {
            return _jl_cell29();
        }
        // 0xffff
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell28();
        }
        }
        
    }
    
    private int _jl_cell28() {
        return 2;
    }
    
    private int _jl_cell29() {
        return 1;
    }
    
    private int _jl_cell30() {
        switch (getNextChar()) {
        // 0x000a
        case 10: {
            return _jl_cell37();
        }
        // 0x000d
        case 13: {
            return _jl_cell38();
        }
        // 0x0022
        case 34: {
            return _jl_cell33();
        }
        // 0x0027
        case 39: {
            return _jl_cell32();
        }
        // 0x002f
        case 47: {
            return _jl_cell31();
        }
        // 0x007b
        case 123: {
            return _jl_cell35();
        }
        // 0x007d
        case 125: {
            return _jl_cell34();
        }
        // 0xffff
        case 65535: {
            return _jl_cell39();
        }
        default:  {
            return _jl_cell36();
        }
        }
        
    }
    
    private int _jl_cell31() {
        switch (getNextChar()) {
        // 0x002a
        case 42: {
            return _jl_cell41();
        }
        // 0x002f
        case 47: {
            return _jl_cell40();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell32() {
        return 3;
    }
    
    private int _jl_cell33() {
        return 2;
    }
    
    private int _jl_cell34() {
        return 1;
    }
    
    private int _jl_cell35() {
        return 0;
    }
    
    private int _jl_cell36() {
        while (true) {
            mark(8);
            switch (getNextChar()) {
            // [0x000a0x000d0x00220x00270x002f0x007b0x007d0xffff]
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
    
    private int _jl_cell37() {
        return 7;
    }
    
    private int _jl_cell38() {
        mark(7);
        switch (getNextChar()) {
        // 0x000a
        case 10: {
            return _jl_cell37();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell39() {
        return 6;
    }
    
    private int _jl_cell40() {
        while (true) {
            mark(5);
            switch (getNextChar()) {
            // [0x000a0x000d0xffff]
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
    
    private int _jl_cell41() {
        return 4;
    }
    
    private int _jl_cell42() {
        switch (getNextChar()) {
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
            return _jl_cell43();
        }
        // 0xffff
        case 65535: {
            return _jl_cell51();
        }
        default:  {
            return _jl_cell48();
        }
        }
        
    }
    
    private int _jl_cell43() {
        switch (getNextChar()) {
        // 0x002a
        case 42: {
            return _jl_cell53();
        }
        // 0x002f
        case 47: {
            return _jl_cell52();
        }
        default:  {
            return rewind();
            
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
            switch (getNextChar()) {
            // [0x000a0x000d0x00220x0027-0x00290x002f0xffff]
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
        switch (getNextChar()) {
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
        while (true) {
            mark(5);
            switch (getNextChar()) {
            // [0x000a0x000d0xffff]
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
    
    private int _jl_cell53() {
        return 4;
    }
    
    private int _jl_cell54() {
        mark(2);
        switch (getNextChar()) {
        // [0x00270xffff]
        case 39:
        case 65535: {
            return rewind();
            
        }
        // \\
        case 92: {
            return _jl_cell55();
        }
        default:  {
            return _jl_cell56();
        }
        }
        
    }
    
    private int _jl_cell55() {
        switch (getNextChar()) {
        // 0xffff
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell58();
        }
        }
        
    }
    
    private int _jl_cell56() {
        switch (getNextChar()) {
        // 0x0027
        case 39: {
            return _jl_cell57();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell57() {
        return 0;
    }
    
    private int _jl_cell58() {
        switch (getNextChar()) {
        // 0x0027
        case 39: {
            return _jl_cell59();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell59() {
        return 1;
    }
    
     
    
}
