package jl;
import static jl.JLToken.*;
/**
 * Lexer generated by Dolmen 
 */
@org.eclipse.jdt.annotation.NonNullByDefault({})
public final class JLLexerGenerated extends codegen.LexBuffer {
    
    
    private final StringBuilder stringBuffer = new StringBuilder();
    private int braceDepth = 0;
    
    private char forBackslash(char c) {
        switch (c) {
        case 'n': return '\012';
        case 'r': return '\015';
        case 'b': return '\010';
        case 't': return '\011';
        default: return c;
        }
    }
    
    private jl.JLToken identOrKeyword(String id) {
        if (id.equals("rule")) return RULE;
        else if (id.equals("shortest")) return SHORTEST;
        else if (id.equals("eof")) return EOF;
        else if (id.equals("as")) return AS;
        else if (id.equals("orelse")) return ORELSE;
        else if (id.equals("import")) return IMPORT;
        else if (id.equals("static")) return STATIC;
        else if (id.equals("public")) return PUBLIC;
        else if (id.equals("private")) return PRIVATE;
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
    public JLLexerGenerated(String inputname, java.io.Reader reader) {
        super(inputname, reader);
    }
    
    /**
     * Entry point for rule main
     */
    public jl.JLToken main() {
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
            stringBuffer.setLength(0);
string();
@SuppressWarnings("null")
jl.JLToken res = LSTRING(stringBuffer.toString());
return res;
        }
        case 5:  {
            braceDepth = 1;
Position p = getLexemeEnd();
int endOffset = action();
syntax.Location loc = new syntax.Location(
    filename, p.offset, endOffset, p.line, p.column());
return ACTION(loc);
        }
        case 6:  {
            return UNDERSCORE;
        }
        case 7:  {
            return identOrKeyword(getLexeme());
        }
        case 8:  {
            final char c = getSubLexemeChar(startPos + 1);
            return LCHAR(c);
        }
        case 9:  {
            final char c = getSubLexemeChar(startPos + 2);
            return LCHAR(forBackslash(c));
        }
        case 10:  {
            return EQUAL;
        }
        case 11:  {
            return OR;
        }
        case 12:  {
            return LBRACKET;
        }
        case 13:  {
            return RBRACKET;
        }
        case 14:  {
            return STAR;
        }
        case 15:  {
            return MAYBE;
        }
        case 16:  {
            return PLUS;
        }
        case 17:  {
            return LPAREN;
        }
        case 18:  {
            return RPAREN;
        }
        case 19:  {
            return CARET;
        }
        case 20:  {
            return DASH;
        }
        case 21:  {
            return HASH;
        }
        case 22:  {
            return DOT;
        }
        case 23:  {
            return SEMICOL;
        }
        case 24:  {
            return END;
        }
        case 25:  {
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
        int result = _jl_cell34();
        endToken();
        switch (result) {
        case 0:  {
            return;
        }
        case 1:  {
            comment(); return;
        }
        case 2:  {
            stringBuffer.setLength(0);
string();
stringBuffer.setLength(0);comment(); return;

        }
        case 3:  {
            skipChar(); comment(); return;
        }
        case 4:  {
            throw error("Unterminated comment");
        }
        case 5:  {
            newline(); comment(); return;
        }
        case 6:  {
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
        int result = _jl_cell43();
        endToken();
        switch (result) {
        case 0:  {
            return;
        }
        case 1:  {
            final char c = getSubLexemeChar(startPos + 1);
            stringBuffer.append(forBackslash(c)); string(); return;
        }
        case 2:  {
            final char c = getSubLexemeChar(startPos + 1);
            stringBuffer.append('\\').append(c); string(); return;
        }
        case 3:  {
            throw error("Unterminated string");
        }
        case 4:  {
            stringBuffer.append(getLexeme()); string(); return;
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
        int result = _jl_cell50();
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
string();stringBuffer.setLength(0);
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
     * Entry point for rule skipChar
     */
    private void skipChar() {
        // Initialize lexer for this automaton
        startToken();
        int result = _jl_cell62();
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
        // 0x0028
        case 40: {
            return _jl_cell9();
        }
        // 0x002a
        case 42: {
            return _jl_cell12();
        }
        // \]
        case 93: {
            return _jl_cell13();
        }
        // 0x003d
        case 61: {
            return _jl_cell16();
        }
        // \^
        case 94: {
            return _jl_cell7();
        }
        // 0x000a
        case 10: {
            return _jl_cell23();
        }
        // 0x0029
        case 41: {
            return _jl_cell8();
        }
        // \[
        case 91: {
            return _jl_cell14();
        }
        // 0xffff
        case 65535: {
            return _jl_cell2();
        }
        // 0x007b
        case 123: {
            return _jl_cell20();
        }
        // 0x003b
        case 59: {
            return _jl_cell3();
        }
        // 0x007c
        case 124: {
            return _jl_cell15();
        }
        // 0x003f
        case 63: {
            return _jl_cell11();
        }
        // \-
        case 45: {
            return _jl_cell6();
        }
        // 0x002e
        case 46: {
            return _jl_cell4();
        }
        // [0x00090x000c0x0020]
        case 9:
        case 12:
        case 32: {
            return _jl_cell25();
        }
        // 0x000d
        case 13: {
            return _jl_cell24();
        }
        // 0x002f
        case 47: {
            return _jl_cell22();
        }
        // 0x0023
        case 35: {
            return _jl_cell5();
        }
        // [A-Za-z]
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
            return _jl_cell18();
        }
        // \_
        case 95: {
            return _jl_cell19();
        }
        // 0x0027
        case 39: {
            return _jl_cell17();
        }
        // 0x0022
        case 34: {
            return _jl_cell21();
        }
        // 0x002b
        case 43: {
            return _jl_cell10();
        }
        default:  {
            return _jl_cell1();
        }
        }
        
    }
    
    private int _jl_cell1() {
        return 25;
    }
    
    private int _jl_cell2() {
        return 24;
    }
    
    private int _jl_cell3() {
        return 23;
    }
    
    private int _jl_cell4() {
        return 22;
    }
    
    private int _jl_cell5() {
        return 21;
    }
    
    private int _jl_cell6() {
        return 20;
    }
    
    private int _jl_cell7() {
        return 19;
    }
    
    private int _jl_cell8() {
        return 18;
    }
    
    private int _jl_cell9() {
        return 17;
    }
    
    private int _jl_cell10() {
        return 16;
    }
    
    private int _jl_cell11() {
        return 15;
    }
    
    private int _jl_cell12() {
        return 14;
    }
    
    private int _jl_cell13() {
        return 13;
    }
    
    private int _jl_cell14() {
        return 12;
    }
    
    private int _jl_cell15() {
        return 11;
    }
    
    private int _jl_cell16() {
        return 10;
    }
    
    private int _jl_cell17() {
        mark(25);
        switch (getNextChar()) {
        // \\
        case 92: {
            return _jl_cell29();
        }
        // 0xffff
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell30();
        }
        }
        
    }
    
    private int _jl_cell18() {
        mark(7);
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
            return _jl_cell18();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell19() {
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
            return _jl_cell18();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell20() {
        return 5;
    }
    
    private int _jl_cell21() {
        return 4;
    }
    
    private int _jl_cell22() {
        mark(25);
        switch (getNextChar()) {
        // 0x002f
        case 47: {
            return _jl_cell26();
        }
        // 0x002a
        case 42: {
            return _jl_cell27();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell23() {
        return 1;
    }
    
    private int _jl_cell24() {
        mark(1);
        switch (getNextChar()) {
        // 0x000a
        case 10: {
            return _jl_cell23();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell25() {
        mark(0);
        switch (getNextChar()) {
        // [0x00090x000c0x0020]
        case 9:
        case 12:
        case 32: {
            return _jl_cell25();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell26() {
        switch (getNextChar()) {
        // [0x000a0x000d0xffff]
        case 10:
        case 13:
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell28();
        }
        }
        
    }
    
    private int _jl_cell27() {
        return 2;
    }
    
    private int _jl_cell28() {
        mark(3);
        switch (getNextChar()) {
        // [0x000a0x000d0xffff]
        case 10:
        case 13:
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell28();
        }
        }
        
    }
    
    private int _jl_cell29() {
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
            return _jl_cell32();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell30() {
        switch (getNextChar()) {
        // 0x0027
        case 39: {
            return _jl_cell31();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell31() {
        return 8;
    }
    
    private int _jl_cell32() {
        switch (getNextChar()) {
        // 0x0027
        case 39: {
            return _jl_cell33();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell33() {
        return 9;
    }
    
    private int _jl_cell34() {
        switch (getNextChar()) {
        // 0x0022
        case 34: {
            return _jl_cell39();
        }
        // 0xffff
        case 65535: {
            return _jl_cell37();
        }
        // 0x000a
        case 10: {
            return _jl_cell35();
        }
        // 0x002a
        case 42: {
            return _jl_cell40();
        }
        // 0x000d
        case 13: {
            return _jl_cell36();
        }
        // 0x0027
        case 39: {
            return _jl_cell38();
        }
        default:  {
            return _jl_cell41();
        }
        }
        
    }
    
    private int _jl_cell35() {
        return 5;
    }
    
    private int _jl_cell36() {
        mark(5);
        switch (getNextChar()) {
        // 0x000a
        case 10: {
            return _jl_cell35();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell37() {
        return 4;
    }
    
    private int _jl_cell38() {
        return 3;
    }
    
    private int _jl_cell39() {
        return 2;
    }
    
    private int _jl_cell40() {
        mark(1);
        switch (getNextChar()) {
        // 0x002f
        case 47: {
            return _jl_cell42();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell41() {
        mark(6);
        switch (getNextChar()) {
        // [0x000a0x000d0x00220x00270x002a0xffff]
        case 10:
        case 13:
        case 34:
        case 39:
        case 42:
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell41();
        }
        }
        
    }
    
    private int _jl_cell42() {
        return 0;
    }
    
    private int _jl_cell43() {
        switch (getNextChar()) {
        // 0xffff
        case 65535: {
            return _jl_cell45();
        }
        // \\
        case 92: {
            return _jl_cell46();
        }
        // 0x0022
        case 34: {
            return _jl_cell47();
        }
        default:  {
            return _jl_cell44();
        }
        }
        
    }
    
    private int _jl_cell44() {
        mark(4);
        switch (getNextChar()) {
        // [0x0022\\0xffff]
        case 34:
        case 92:
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell44();
        }
        }
        
    }
    
    private int _jl_cell45() {
        return 3;
    }
    
    private int _jl_cell46() {
        switch (getNextChar()) {
        // 0xffff
        case 65535: {
            return rewind();
            
        }
        // [0x00200x00220x0027\\bnrt]
        case 32:
        case 34:
        case 39:
        case 92:
        case 98:
        case 110:
        case 114:
        case 116: {
            return _jl_cell49();
        }
        default:  {
            return _jl_cell48();
        }
        }
        
    }
    
    private int _jl_cell47() {
        return 0;
    }
    
    private int _jl_cell48() {
        return 2;
    }
    
    private int _jl_cell49() {
        return 1;
    }
    
    private int _jl_cell50() {
        switch (getNextChar()) {
        // 0xffff
        case 65535: {
            return _jl_cell58();
        }
        // 0x002f
        case 47: {
            return _jl_cell59();
        }
        // 0x007d
        case 125: {
            return _jl_cell53();
        }
        // 0x007b
        case 123: {
            return _jl_cell54();
        }
        // 0x0027
        case 39: {
            return _jl_cell51();
        }
        // 0x000a
        case 10: {
            return _jl_cell56();
        }
        // 0x0022
        case 34: {
            return _jl_cell52();
        }
        // 0x000d
        case 13: {
            return _jl_cell57();
        }
        default:  {
            return _jl_cell55();
        }
        }
        
    }
    
    private int _jl_cell51() {
        return 3;
    }
    
    private int _jl_cell52() {
        return 2;
    }
    
    private int _jl_cell53() {
        return 1;
    }
    
    private int _jl_cell54() {
        return 0;
    }
    
    private int _jl_cell55() {
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
            return _jl_cell55();
        }
        }
        
    }
    
    private int _jl_cell56() {
        return 7;
    }
    
    private int _jl_cell57() {
        mark(7);
        switch (getNextChar()) {
        // 0x000a
        case 10: {
            return _jl_cell56();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell58() {
        return 6;
    }
    
    private int _jl_cell59() {
        switch (getNextChar()) {
        // 0x002a
        case 42: {
            return _jl_cell60();
        }
        // 0x002f
        case 47: {
            return _jl_cell61();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell60() {
        return 4;
    }
    
    private int _jl_cell61() {
        mark(5);
        switch (getNextChar()) {
        // [0x000a0x000d0xffff]
        case 10:
        case 13:
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell61();
        }
        }
        
    }
    
    private int _jl_cell62() {
        mark(2);
        switch (getNextChar()) {
        // \\
        case 92: {
            return _jl_cell63();
        }
        // [0x00270xffff]
        case 39:
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell64();
        }
        }
        
    }
    
    private int _jl_cell63() {
        switch (getNextChar()) {
        // 0xffff
        case 65535: {
            return rewind();
            
        }
        default:  {
            return _jl_cell66();
        }
        }
        
    }
    
    private int _jl_cell64() {
        switch (getNextChar()) {
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
        return 0;
    }
    
    private int _jl_cell66() {
        switch (getNextChar()) {
        // 0x0027
        case 39: {
            return _jl_cell67();
        }
        default:  {
            return rewind();
            
        }
        }
        
    }
    
    private int _jl_cell67() {
        return 1;
    }
    
}
