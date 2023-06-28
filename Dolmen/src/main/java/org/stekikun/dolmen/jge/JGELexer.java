package org.stekikun.dolmen.jge;
import java.util.Stack;
import static org.stekikun.dolmen.jge.JGEParser.Token.*;
import org.stekikun.dolmen.jge.JGEParser.Token;
import org.stekikun.dolmen.syntax.PExtent;

/**
 * Lexer generated by Dolmen 1.0.0
 */
public final class JGELexer extends org.stekikun.dolmen.codegen.LexBuffer {
    
    
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

    private char fromOctalCode(String code) {
        return (char)(Integer.parseInt(code, 8));
    }
    
    private char fromHexCode(String code) {
        return (char)(Integer.parseInt(code, 16));
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
    public JGELexer(String inputname, java.io.Reader reader) {
        super("1.0.0", inputname, reader);
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
              errLocs.push(start);
			  Position p = getLexemeEnd();
			  PExtent ext = action(new PExtent.Builder(filename, p.offset, p.line, p.column()));
              startLoc = start;
              errLocs.pop();
			  return ACTION(ext);
			
            }
            case 5:  {
                 parenDepth = 1;
              Position start = getLexemeStart();
              errLocs.push(start);
			  Position p = getLexemeEnd();
			  PExtent ext = arguments(new PExtent.Builder(filename, p.offset, p.line, p.column()));
			  startLoc = start;
			  errLocs.pop();
			  return ARGUMENTS(ext);
			
            }
            case 6:  {
                 Position start = getLexemeStart();
			  errLocs.push(start); 
			  stringBuffer.setLength(0);
			  string(true);
			  startLoc = start;
			  errLocs.pop();
			  return MLSTRING(stringBuffer.toString());
			
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
                 return LANGLE; 
            }
            case 11:  {
                 return RANGLE; 
            }
            case 12:  {
                 return COMMA; 
            }
            case 13:  {
                 return SEMICOL; 
            }
            case 14:  {
                 return DOT; 
            }
            case 15:  {
                 return STAR; 
            }
            case 16:  {
                 return EQUAL; 
            }
            case 17:  {
                 return BAR; 
            }
            case 18:  {
                 return EOF; 
            }
            case 19:  {
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
            int result = _jl_cell23();
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
    private void string(boolean multi) {
        string:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell30();
            endToken();
            switch (result) {
            case 0:  {
                 return; 
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
     * Entry point for rule escapeSequence
     */
    private  char  escapeSequence() {
        escapeSequence:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell37();
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
    private PExtent action(PExtent.Builder builder) {
        action:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell50();
            endToken();
            switch (result) {
            case 0:  {
                 ++braceDepth; continue action; 
            }
            case 1:  {
                 --braceDepth;
			  if (braceDepth == 0)
			  	return builder.build(getLexemeStart().offset - 1);
			  continue action;
			
            }
            case 2:  {
                final String hole_name = getSubLexeme(startPos + 1, curPos);
                 Position p = getLexemeStart();
			  builder.addHole(p.offset, hole_name, p.line, p.column()); 
			  continue action;
			
            }
            case 3:  {
                 errLocs.push(getLexemeStart());
			  stringBuffer.setLength(0);
			  string(false);	// Java string literals are single-line
			  stringBuffer.setLength(0);
			  errLocs.pop();
			  continue action;
			
            }
            case 4:  {
                 errLocs.push(getLexemeStart());
			  character();
			  errLocs.pop();
			  continue action;
			
            }
            case 5:  {
                 comment(); continue action; 
            }
            case 6:  {
                 continue action; 
            }
            case 7:  {
                 throw error("Unterminated action"); 
            }
            case 8:  {
                 newline(); continue action; 
            }
            case 9:  {
                 continue action; 
            }
            case 10:  {
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
    private PExtent arguments(PExtent.Builder builder) {
        arguments:
        while (true) {
            // Initialize lexer for this automaton
            startToken();
            int result = _jl_cell64();
            endToken();
            switch (result) {
            case 0:  {
                 ++parenDepth; continue arguments; 
            }
            case 1:  {
                 --parenDepth;
			  if (parenDepth == 0)
			  	return builder.build(getLexemeStart().offset - 1);
			  continue arguments;
			
            }
            case 2:  {
                final String hole_name = getSubLexeme(startPos + 1, curPos);
                 Position p = getLexemeStart();
			  builder.addHole(p.offset, hole_name, p.line, p.column()); 
			  continue arguments;
			
            }
            case 3:  {
                 errLocs.push(getLexemeStart());
			  stringBuffer.setLength(0);
			  string(false);	// Java string literals are single-line
			  stringBuffer.setLength(0);
			  errLocs.pop();
			  continue arguments;
			
            }
            case 4:  {
                 errLocs.push(getLexemeStart());
			  character();
			  errLocs.pop();
			  continue arguments;
			
            }
            case 5:  {
                 comment(); continue arguments; 
            }
            case 6:  {
                 continue arguments; 
            }
            case 7:  {
                 throw error("Unterminated arguments"); 
            }
            case 8:  {
                 newline(); continue arguments; 
            }
            case 9:  {
                 continue arguments; 
            }
            case 10:  {
                 continue arguments; 
            }
            default:
                break arguments;
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
            int result = _jl_cell78();
            endToken();
            switch (result) {
            case 0:  {
                 throw error("Invalid character literal"); 
            }
            case 1:  {
                final char c = getSubLexemeChar(startPos);
                 characterClose(); return c; 
            }
            case 2:  {
                 errLocs.push(getLexemeStart());
				  char c = escapeSequence();
				  errLocs.pop();
				  characterClose();
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
            int result = _jl_cell84();
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
    
    private int _jl_cell0() {
        final char _jl_char = getNextChar();
        if (_jl_char <= 46) {
            switch (_jl_char) {
            // [0x0009 0x000c 0x0020]
            case 9:
            case 12:
            case 32: {
                return _jl_cell20();
            }
            // 0x000a
            case 10: {
                return 1;
            }
            // 0x000d
            case 13: {
                return _jl_cell19();
            }
            // "
            case 34: {
                return 6;
            }
            // (
            case 40: {
                return 5;
            }
            // *
            case 42: {
                return 15;
            }
            // ,
            case 44: {
                return 12;
            }
            // .
            case 46: {
                return 14;
            }
            default:  {
                return 19;
            }
            }
        } else  {
            if (_jl_char <= 91) {
                if (_jl_char <= 60) {
                    switch (_jl_char) {
                    // /
                    case 47: {
                        return _jl_cell17();
                    }
                    // ;
                    case 59: {
                        return 13;
                    }
                    // <
                    case 60: {
                        return 10;
                    }
                    default:  {
                        return 19;
                    }
                    }
                } else  {
                    switch (_jl_char) {
                    // 0x003d
                    case 61: {
                        return 16;
                    }
                    // >
                    case 62: {
                        return 11;
                    }
                    // [?-@]
                    case 63:
                    case 64: {
                        return 19;
                    }
                    // \[
                    case 91: {
                        return 8;
                    }
                    default:  {
                        return _jl_cell13();
                    }
                    }
                }
            } else  {
                if (_jl_char <= 96) {
                    switch (_jl_char) {
                    // \]
                    case 93: {
                        return 9;
                    }
                    // \_
                    case 95: {
                        return _jl_cell13();
                    }
                    default:  {
                        return 19;
                    }
                    }
                } else  {
                    if (_jl_char <= 123) {
                        switch (_jl_char) {
                        // {
                        case 123: {
                            return 4;
                        }
                        default:  {
                            return _jl_cell13();
                        }
                        }
                    } else  {
                        switch (_jl_char) {
                        // |
                        case 124: {
                            return 17;
                        }
                        // EOF
                        case 65535: {
                            return 18;
                        }
                        default:  {
                            return 19;
                        }
                        }
                    }
                }
            }
        }
    }
    
    private int _jl_cell13() {
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
    
    private int _jl_cell17() {
        mark(19);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // *
        case 42: {
            return 2;
        }
        // /
        case 47: {
            return _jl_cell21();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell19() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 1;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell20() {
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
    
    private int _jl_cell21() {
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
    
    private int _jl_cell23() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 3;
        }
        // 0x000d
        case 13: {
            return _jl_cell28();
        }
        // *
        case 42: {
            return _jl_cell25();
        }
        // EOF
        case 65535: {
            return 2;
        }
        default:  {
            return _jl_cell26();
        }
        }
    }
    
    private int _jl_cell25() {
        mark(1);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // /
        case 47: {
            return 0;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell26() {
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
    
    private int _jl_cell28() {
        mark(3);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 3;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell30() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 2;
        }
        // 0x000d
        case 13: {
            return _jl_cell34();
        }
        // "
        case 34: {
            return 0;
        }
        // \\
        case 92: {
            return 1;
        }
        // EOF
        case 65535: {
            return 3;
        }
        default:  {
            return _jl_cell31();
        }
        }
    }
    
    private int _jl_cell31() {
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
    
    private int _jl_cell34() {
        mark(2);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 2;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell37() {
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
            return 0;
        }
        // [0-3]
        case 48:
        case 49:
        case 50:
        case 51: {
            return _jl_cell43();
        }
        // [4-7]
        case 52:
        case 53:
        case 54:
        case 55: {
            return _jl_cell42();
        }
        // u
        case 117: {
            return _jl_cell41();
        }
        // EOF
        case 65535: {
            return 5;
        }
        default:  {
            return 4;
        }
        }
    }
    
    private int _jl_cell41() {
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
                return _jl_cell46();
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
    
    private int _jl_cell42() {
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
            return 1;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell43() {
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
            return _jl_cell44();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell44() {
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
            return 1;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell46() {
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
            return _jl_cell47();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell47() {
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
            return _jl_cell48();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell48() {
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
            return 2;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell50() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 8;
        }
        // 0x000d
        case 13: {
            return _jl_cell53();
        }
        // "
        case 34: {
            return 3;
        }
        // #
        case 35: {
            return _jl_cell58();
        }
        // '
        case 39: {
            return 4;
        }
        // /
        case 47: {
            return _jl_cell55();
        }
        // {
        case 123: {
            return 0;
        }
        // }
        case 125: {
            return 1;
        }
        // EOF
        case 65535: {
            return 7;
        }
        default:  {
            return _jl_cell51();
        }
        }
    }
    
    private int _jl_cell51() {
        while (true) {
            mark(9);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d "-# ' / { } EOF]
            case 10:
            case 13:
            case 34:
            case 35:
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
    
    private int _jl_cell53() {
        mark(8);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 8;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell55() {
        mark(10);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // *
        case 42: {
            return 5;
        }
        // /
        case 47: {
            return _jl_cell62();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell58() {
        mark(10);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [a-z]
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
            return _jl_cell61();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell61() {
        while (true) {
            mark(2);
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
    
    private int _jl_cell62() {
        while (true) {
            mark(6);
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
    
    private int _jl_cell64() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 8;
        }
        // 0x000d
        case 13: {
            return _jl_cell73();
        }
        // "
        case 34: {
            return 3;
        }
        // #
        case 35: {
            return _jl_cell68();
        }
        // '
        case 39: {
            return 4;
        }
        // (
        case 40: {
            return 0;
        }
        // )
        case 41: {
            return 1;
        }
        // /
        case 47: {
            return _jl_cell65();
        }
        // EOF
        case 65535: {
            return 7;
        }
        default:  {
            return _jl_cell71();
        }
        }
    }
    
    private int _jl_cell65() {
        mark(10);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // *
        case 42: {
            return 5;
        }
        // /
        case 47: {
            return _jl_cell76();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell68() {
        mark(10);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // [a-z]
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
            return _jl_cell75();
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell71() {
        while (true) {
            mark(9);
            final char _jl_char = getNextChar();
            switch (_jl_char) {
            // [0x000a 0x000d "-# '-) / EOF]
            case 10:
            case 13:
            case 34:
            case 35:
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
    
    private int _jl_cell73() {
        mark(8);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 8;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell75() {
        while (true) {
            mark(2);
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
    
    private int _jl_cell76() {
        while (true) {
            mark(6);
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
    
    private int _jl_cell78() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 3;
        }
        // 0x000d
        case 13: {
            return _jl_cell80();
        }
        // '
        case 39: {
            return 0;
        }
        // \\
        case 92: {
            return 2;
        }
        // EOF
        case 65535: {
            return 3;
        }
        default:  {
            return 1;
        }
        }
    }
    
    private int _jl_cell80() {
        mark(3);
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // 0x000a
        case 10: {
            return 3;
        }
        default:  {
            return rewind();
        }
        }
    }
    
    private int _jl_cell84() {
        final char _jl_char = getNextChar();
        switch (_jl_char) {
        // '
        case 39: {
            return 0;
        }
        // EOF
        case 65535: {
            return 1;
        }
        default:  {
            return 1;
        }
        }
    }
    
     
    
}