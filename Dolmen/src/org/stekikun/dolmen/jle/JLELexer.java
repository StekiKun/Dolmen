package org.stekikun.dolmen.jle;
import java.util.Stack;
import static org.stekikun.dolmen.jle.JLEParser.Token.*;
import org.stekikun.dolmen.jle.JLEParser.Token;
import org.stekikun.dolmen.syntax.Extent;

/**
 * Lexer generated by Dolmen 1.0.0
 */
public final class JLELexer extends org.stekikun.dolmen.codegen.LexBuffer {
    
    
    private final StringBuilder stringBuffer = new StringBuilder();
	private int braceDepth = 0;
    
    private char forBackslash(char c) {
        switch (c) {
        case 'n': return '\012';
        case 'r': return '\015';
        case 'b': return '\010';
        case 't': return '\011';
        case 'f': return '\014';
        default: return c;
        }
    }
    
    private char fromOctalCode(String code) {
        return (char)(Integer.parseInt(code, 8));    }
    
    private char fromHexCode(String code) {
        return (char)(Integer.parseInt(code, 16));    }
    
    private Token identOrKeyword(String id) {
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
    
    // We override the default behaviour of #error(String) to conveniently
    // report errors at the beginning of a saved position instead of the current
    // lexeme start.
    
    private Stack<Position> errLocs = new Stack<>();
    
    @Override
    protected LexicalError error(String msg) {
    	Position err = errLocs.isEmpty() ? getLexemeStart() : errLocs.peek();
    	return new LexicalError(err, msg);
    } 

    
    /**
     * Returns a fresh lexer based on the given character stream
     * @param inputname
     * @param reader
     */
    public JLELexer(String inputname, java.io.Reader reader) {
        super(inputname, reader);
    }
    
    /**
     * Entry point for rule main
     */
    public  Token  main() {
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
                 Position stringStart = getLexemeStart();
				  errLocs.push(stringStart);
				  stringBuffer.setLength(0);
				  int eline = string(true);
				  startLoc = stringStart;
				  Token res = stringStart.line == eline ? 
				 	LSTRING(stringBuffer.toString()) :
				 	MLSTRING(stringBuffer.toString());
				  errLocs.pop();
				  return res;
				
            }
            case 5:  {
                 braceDepth = 1;
				  Position start = getLexemeStart();
				  errLocs.push(start);
				  Position p = getLexemeEnd();
				  int endOffset = action();
				  Extent ext = new Extent(
				  	filename, p.offset, endOffset, p.line, p.column());
				  startLoc = start;
				  errLocs.pop();
				  return ACTION(ext);
				
            }
            case 6:  {
                 return UNDERSCORE; 
            }
            case 7:  {
                 return identOrKeyword(getLexeme()); 
            }
            case 8:  {
                 return INTEGER(Integer.parseInt(getLexeme())); 
            }
            case 9:  {
                 Position start = getLexemeStart();
				  errLocs.push(start);
				  char c = character();
				  characterClose();
				  startLoc = start;
				  errLocs.pop();
				  return LCHAR(c);
				 
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
                 return LANGLE; 
            }
            case 24:  {
                 return RANGLE; 
            }
            case 25:  {
                 return COMMA; 
            }
            case 26:  {
                 return SEMICOL; 
            }
            case 27:  {
                 return END; 
            }
            case 28:  {
                final char c = getSubLexemeChar(startPos);
                 throw error("Unexpected character: " + c); 
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
    private  void  comment() {
        comment:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell33();
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
    private  int  string(boolean multi) {
        string:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell40();
            endToken();
            switch (result) {
            case 0:  {
                 return getLexemeEnd().line; 
            }
            case 1:  {
                 errLocs.push(getLexemeStart());
						  char c = escapeSequence();
						  stringBuffer.append(c);
						  errLocs.pop();
						  continue string;
						
            }
            case 2:  {
                 if (!multi)
							throw error("String literal in Java action not properly closed");
						  newline();
						  stringBuffer.append(getLexeme());
						  continue string; 
            }
            case 3:  {
                 throw error("Unterminated string literal"); 
            }
            case 4:  {
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
     * Entry point for rule character
     */
    private  char  character() {
        character:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell47();
            endToken();
            switch (result) {
            case 0:  {
                 throw error("Invalid character literal"); 
            }
            case 1:  {
                final char c = getSubLexemeChar(startPos);
                 return c; 
            }
            case 2:  {
                 errLocs.push(getLexemeStart());
				  char c = escapeSequence();
				  errLocs.pop();
				  return c;
				
            }
            case 3:  {
                 throw error("Unterminated character literal"); 
            }
            default:
                break character;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule characterClose
     */
    private  void  characterClose() {
        characterClose:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell53();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
            }
            case 1:  {
                 throw error("Unterminated character literal"); 
            }
            default:
                break characterClose;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule escapeSequence
     */
    private  char  escapeSequence() {
        escapeSequence:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell56();
            endToken();
            switch (result) {
            case 0:  {
                final char c = getSubLexemeChar(startPos);
                 return forBackslash(c); 
            }
            case 1:  {
                final String code = getSubLexeme(startPos, curPos);
                 return fromOctalCode(code); 
            }
            case 2:  {
                final String code = getSubLexeme(curPos + (-4), curPos);
                 return fromHexCode(code); 
            }
            case 3:  {
                 throw error("Invalid Unicode escape sequence: " +
					"expected four hexadecimal digits after \\" + getLexeme()); 
            }
            case 4:  {
                 throw error("Invalid escape sequence: " + 
					"only \\\\, \\\', \\\", \\n, \\t, \\b, \\f, \\r are supported"); 
            }
            case 5:  {
                 throw error("Unterminated escape sequence"); 
            }
            default:
                break escapeSequence;
            }
        }
        throw error("Empty token");
    }
    
    /**
     * Entry point for rule action
     */
    private  int  action() {
        action:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell69();
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
                 errLocs.push(getLexemeStart());
		      stringBuffer.setLength(0);
			  string(false);		// Java string literals are single-line
			  stringBuffer.setLength(0);
			  errLocs.pop();
			  continue action;
			
            }
            case 3:  {
                 errLocs.push(getLexemeStart());
			  character();
			  characterClose();
			  errLocs.pop();
			  continue action;
			
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
    
    private int _jl_cell0() {
        final char _jl_char = getNextChar();
        if (_jl_char <= 47) {
            switch (_jl_char) {
            // [0x0009 0x000c 0x0020]
            case 9:
            case 12:
            case 32: {
                return _jl_cell30();
            }
            // 0x000a
            case 10: {
                return _jl_cell28();
            }
            // 0x000d
            case 13: {
                return _jl_cell29();
            }
            // "
            case 34: {
                return _jl_cell26();
            }
            // #
            case 35: {
                return _jl_cell8();
            }
            // '
            case 39: {
                return _jl_cell20();
            }
            // (
            case 40: {
                return _jl_cell12();
            }
            // )
            case 41: {
                return _jl_cell11();
            }
            // *
            case 42: {
                return _jl_cell15();
            }
            // +
            case 43: {
                return _jl_cell13();
            }
            // ,
            case 44: {
                return _jl_cell4();
            }
            // \-
            case 45: {
                return _jl_cell9();
            }
            // .
            case 46: {
                return _jl_cell7();
            }
            // /
            case 47: {
                return _jl_cell27();
            }
            default:  {
                return _jl_cell1();
            }
            }
        } else  {
            if (_jl_char <= 90) {
                if (_jl_char <= 60) {
                    switch (_jl_char) {
                    // 0
                    case 48: {
                        return _jl_cell22();
                    }
                    // :
                    case 58: {
                        return _jl_cell1();
                    }
                    // ;
                    case 59: {
                        return _jl_cell3();
                    }
                    // <
                    case 60: {
                        return _jl_cell6();
                    }
                    default:  {
                        return _jl_cell21();
                    }
                    }
                } else  {
                    switch (_jl_char) {
                    // 0x003d
                    case 61: {
                        return _jl_cell19();
                    }
                    // >
                    case 62: {
                        return _jl_cell5();
                    }
                    // ?
                    case 63: {
                        return _jl_cell14();
                    }
                    // @
                    case 64: {
                        return _jl_cell1();
                    }
                    default:  {
                        return _jl_cell23();
                    }
                    }
                }
            } else  {
                if (_jl_char <= 95) {
                    switch (_jl_char) {
                    // \\
                    case 92: {
                        return _jl_cell1();
                    }
                    // \]
                    case 93: {
                        return _jl_cell16();
                    }
                    // \^
                    case 94: {
                        return _jl_cell10();
                    }
                    // \_
                    case 95: {
                        return _jl_cell24();
                    }
                    default:  {
                        return _jl_cell17();
                    }
                    }
                } else  {
                    if (_jl_char <= 123) {
                        switch (_jl_char) {
                        // 0x0060
                        case 96: {
                            return _jl_cell1();
                        }
                        // {
                        case 123: {
                            return _jl_cell25();
                        }
                        default:  {
                            return _jl_cell23();
                        }
                        }
                    } else  {
                        switch (_jl_char) {
                        // |
                        case 124: {
                            return _jl_cell18();
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
    }
    
    private int _jl_cell1() {
        return 28;
    }
    
    private int _jl_cell2() {
        return 27;
    }
    
    private int _jl_cell3() {
        return 26;
    }
    
    private int _jl_cell4() {
        return 25;
    }
    
    private int _jl_cell5() {
        return 24;
    }
    
    private int _jl_cell6() {
        return 23;
    }
    
    private int _jl_cell7() {
        return 22;
    }
    
    private int _jl_cell8() {
        return 21;
    }
    
    private int _jl_cell9() {
        return 20;
    }
    
    private int _jl_cell10() {
        return 19;
    }
    
    private int _jl_cell11() {
        return 18;
    }
    
    private int _jl_cell12() {
        return 17;
    }
    
    private int _jl_cell13() {
        return 16;
    }
    
    private int _jl_cell14() {
        return 15;
    }
    
    private int _jl_cell15() {
        return 14;
    }
    
    private int _jl_cell16() {
        return 13;
    }
    
    private int _jl_cell17() {
        return 12;
    }
    
    private int _jl_cell18() {
        return 11;
    }
    
    private int _jl_cell19() {
        return 10;
    }
    
    private int _jl_cell20() {
        return 9;
    }
    
    private int _jl_cell21() {
        while (true) {
            mark(8);
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
    
    private int _jl_cell22() {
        return 8;
    }
    
    private int _jl_cell23() {
        while (true) {
            mark(7);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0-9 A-Z \_ a-z]
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
    
    private int _jl_cell24() {
        mark(6);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-9 A-Z \_ a-z]
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
            return _jl_cell23();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell25() {
        return 5;
    }
    
    private int _jl_cell26() {
        return 4;
    }
    
    private int _jl_cell27() {
        mark(28);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // *
        case 42: {
            return _jl_cell32();
        }
        // /
        case 47: {
            return _jl_cell31();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell28() {
        return 1;
    }
    
    private int _jl_cell29() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell28();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell30() {
        while (true) {
            mark(0);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x0009 0x000c 0x0020]
            case 9:
            case 12:
            case 32: {
                continue;
            }
            default:  {
                return rewind();
            }
            }
        }
    }
    
    private int _jl_cell31() {
        while (true) {
            mark(3);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d EOF]
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
    
    private int _jl_cell32() {
        return 2;
    }
    
    private int _jl_cell33() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell35();
        }
        // 0x000d
        case 13: {
            return _jl_cell36();
        }
        // *
        case 42: {
            return _jl_cell38();
        }
        // EOF
        case 65535: {
            return _jl_cell37();
        }
        default:  {
            return _jl_cell34();
        }
        }
    }
    
    private int _jl_cell34() {
        while (true) {
            mark(4);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d * EOF]
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
    
    private int _jl_cell35() {
        return 3;
    }
    
    private int _jl_cell36() {
        mark(3);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
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
        return 2;
    }
    
    private int _jl_cell38() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // /
        case 47: {
            return _jl_cell39();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell39() {
        return 0;
    }
    
    private int _jl_cell40() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell43();
        }
        // 0x000d
        case 13: {
            return _jl_cell44();
        }
        // "
        case 34: {
            return _jl_cell46();
        }
        // \\
        case 92: {
            return _jl_cell45();
        }
        // EOF
        case 65535: {
            return _jl_cell42();
        }
        default:  {
            return _jl_cell41();
        }
        }
    }
    
    private int _jl_cell41() {
        while (true) {
            mark(4);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d " \\ EOF]
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
    
    private int _jl_cell42() {
        return 3;
    }
    
    private int _jl_cell43() {
        return 2;
    }
    
    private int _jl_cell44() {
        mark(2);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell43();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell45() {
        return 1;
    }
    
    private int _jl_cell46() {
        return 0;
    }
    
    private int _jl_cell47() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell48();
        }
        // 0x000d
        case 13: {
            return _jl_cell49();
        }
        // '
        case 39: {
            return _jl_cell52();
        }
        // \\
        case 92: {
            return _jl_cell50();
        }
        // EOF
        case 65535: {
            return _jl_cell48();
        }
        default:  {
            return _jl_cell51();
        }
        }
    }
    
    private int _jl_cell48() {
        return 3;
    }
    
    private int _jl_cell49() {
        mark(3);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell48();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell50() {
        return 2;
    }
    
    private int _jl_cell51() {
        return 1;
    }
    
    private int _jl_cell52() {
        return 0;
    }
    
    private int _jl_cell53() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // '
        case 39: {
            return _jl_cell55();
        }
        default:  {
            return _jl_cell54();
        }
        }
    }
    
    private int _jl_cell54() {
        return 1;
    }
    
    private int _jl_cell55() {
        return 0;
    }
    
    private int _jl_cell56() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [" ' \\ b f n r t]
        case 34:
        case 39:
        case 92:
        case 98:
        case 102:
        case 110:
        case 114:
        case 116: {
            return _jl_cell61();
        }
        // [0-3]
        case 48:
        case 49:
        case 50:
        case 51: {
            return _jl_cell60();
        }
        // [4-7]
        case 52:
        case 53:
        case 54:
        case 55: {
            return _jl_cell59();
        }
        // u
        case 117: {
            return _jl_cell62();
        }
        // EOF
        case 65535: {
            return _jl_cell57();
        }
        default:  {
            return _jl_cell58();
        }
        }
    }
    
    private int _jl_cell57() {
        return 5;
    }
    
    private int _jl_cell58() {
        return 4;
    }
    
    private int _jl_cell59() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-7]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55: {
            return _jl_cell68();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell60() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-7]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55: {
            return _jl_cell67();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell61() {
        return 0;
    }
    
    private int _jl_cell62() {
        while (true) {
            mark(3);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0-9 A-F a-f]
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
                return _jl_cell63();
            }
            // u
            case 117: {
                continue;
            }
            default:  {
                return rewind();
            }
            }
        }
    }
    
    private int _jl_cell63() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-9 A-F a-f]
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
            return _jl_cell64();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell64() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-9 A-F a-f]
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
            return _jl_cell65();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell65() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-9 A-F a-f]
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
            return _jl_cell66();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell66() {
        return 2;
    }
    
    private int _jl_cell67() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [0-7]
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55: {
            return _jl_cell68();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell68() {
        return 1;
    }
    
    private int _jl_cell69() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell78();
        }
        // 0x000d
        case 13: {
            return _jl_cell77();
        }
        // "
        case 34: {
            return _jl_cell72();
        }
        // '
        case 39: {
            return _jl_cell71();
        }
        // /
        case 47: {
            return _jl_cell76();
        }
        // {
        case 123: {
            return _jl_cell74();
        }
        // }
        case 125: {
            return _jl_cell73();
        }
        // EOF
        case 65535: {
            return _jl_cell70();
        }
        default:  {
            return _jl_cell75();
        }
        }
    }
    
    private int _jl_cell70() {
        return 6;
    }
    
    private int _jl_cell71() {
        return 3;
    }
    
    private int _jl_cell72() {
        return 2;
    }
    
    private int _jl_cell73() {
        return 1;
    }
    
    private int _jl_cell74() {
        return 0;
    }
    
    private int _jl_cell75() {
        while (true) {
            mark(9);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d " ' / { } EOF]
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
    
    private int _jl_cell76() {
        mark(8);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // *
        case 42: {
            return _jl_cell80();
        }
        // /
        case 47: {
            return _jl_cell79();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell77() {
        mark(7);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return _jl_cell78();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell78() {
        return 7;
    }
    
    private int _jl_cell79() {
        while (true) {
            mark(5);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d EOF]
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
    
    private int _jl_cell80() {
        return 4;
    }
    
     
    
}
