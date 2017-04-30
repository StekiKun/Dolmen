package codegen;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Instances of buffers used by generated lexers.
 * <p>
 * Lexical analysers actually extend this class to inherit
 * a buffer with markers for token positions, final states,
 * and methods which are used by the generated automata,
 * as well as methods that can be used in semantic actions.
 * 
 * @author Stéphane Lescuyer
 */
public class LexBuffer {

	/**
	 * Instances of this class describe a <i>position</i> in some input
	 * (most frequently a file, but could be a string or any char sequence).
	 * A position is given by the {@link #filename description} of the input,
	 * the {@link #offset absolute position} in said input, the {@link #line}
	 * number in the input where this position occurs, and the 
	 * {@link #bol offset of said line} in the input. The column number can
	 * be retrieved via {@link #column()}.
	 * <p>
	 * {@link LexBuffer} uses {@link Position} to register the positions in 
	 * the current input of the last lexeme start and the current position (which
	 * happens to be the last lexeme <i>end</i> when in a semantic action).
	 * {@link LexBuffer} do not manage line numbers by themselves, only absolute
	 * character offsets, so that updating {@link #line} and {@link #bol} is
	 * the responsiblity of the lexer's semantic actions.
	 * See {@link LexBuffer#newline()}.
	 * <p>
	 * Instances of this class are <i>immutable</i> so the lexer must create
	 * new ones when updating them, but a parser using this lexer or semantic
	 * actions can safely use positions without having to copy them defensively.
	 * 
	 * @author Stéphane Lescuyer
	 * @see LexBuffer#newline
	 * @see LexBuffer#getLexemeStart
	 * @see LexBuffer#getLexemeEnd
	 */
	public static final class Position {
		/** 
		 * The filename that this position relates to, or a
		 * description of the input if not a regular file 
		 */
		public final String filename;
		/** The character offset of this position, starting at 0 */
		public final int offset;
		/** The line of this position, starting at 1 */
		public final int line;
		/** The offset of the beginning of the line of this position */
		public final int bol;
		
		/**
		 * Returns the initial position in the given file
		 * @param filename
		 */
		public Position(String filename) {
			this(filename, 0, 1, 0);
		}
		
		/**
		 * Builds a position from the given parameters
		 * @param filename
		 * @param offset
		 * @param line
		 * @param bol
		 */
		public Position(String filename, int offset, int line, int bol) {
			this.filename = filename;
			this.offset = offset;
			this.line = line;
			this.bol = bol;
		}
		
		/**
		 * @return the column offset (1-based) of this position
		 */
		public int column() {
			return offset - bol + 1;
		}
		
		@Override
		public @NonNull String toString() {
			@SuppressWarnings("null")
			@NonNull String res = String.format(
				"[file=%s, char=%d, line=%d, col=%d]",
				filename, offset, line, column());
			return res;
		}
	}
	
    /**
     * Exception which can be raised by generated lexers which
     * extend {@link LexBuffer}, and which is raised also by
     * {@link LexBuffer#getNextChar()} in place of potential
     * {@link IOException}s. 
     */
    public static final class LexicalError extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        /**
         * @param msg	error message
         */
        public LexicalError(@Nullable String msg) {
            super(msg);
        }
    }

	
    /**
     * Constructs a new lexer buffer based on the given character stream
     * @param filename
     * @param reader
     */
    public LexBuffer(@Nullable String filename, java.io.@Nullable Reader reader) {
    	if (filename == null || reader == null)
    		throw new IllegalArgumentException();
    	this.filename = filename;
    	this.reader = reader;
    	this.tokenBuf = new char[1024];
    	this.bufLimit = 0;
    	this.absPos = 0;
    	this.eofReached = false;
    	this.startPos = 0;
    	this.curPos = 0;
    	this.lastAction = -1;
    	this.lastPos = 0;
    	this.memory = new int[0];
    	this.startLoc = new Position(filename);
    	this.curLoc = startLoc;
    }

    /**
     * The name of the input, for locations 
     * (for error reports only, need not be an actual filename) 
     */
    protected final String filename;
    
    /** The character stream to feed the lexer */
    private final java.io.Reader reader;
    
    /** The local character buffer */
    private char[] tokenBuf;

    /** 
     * The extent of valid chars in {@link #tokenBuf},
     * i.e. the index of the first non-valid character 
     */
    private int bufLimit;
    
    /** Absolute position of the start of the buffer */
    protected int absPos;

    /** Whether end-of-file was reached in {@link #reader} */
    private boolean eofReached;
    
    /** Buffer input position of the token start */
    protected int startPos;
    
    /** Current buffer input position */
    protected int curPos;
    
    /** Last action remembered */
    private int lastAction;
    
    /** Position of last action remembered */
    private int lastPos;
    
    /** Memory cells */
    protected int memory[];
    
    /** Position of the last token start */
    protected Position startLoc;
    
    /** Current token position */
    protected Position curLoc;
    
    /**
     * Tries to refill the token buffer from the character
     * stream. This may grow and realloc the token buffer 
     * if necessary, or move valid chars in the token buffer,
     * thus some shifting of positions can be involved as well.
     * <p>
     * This method <i>blocks</i> for input iff {@link #reader}
     * does.
     * @throws IOException 
     */
    private void refill() throws IOException {
    	// How much space at the end
    	int space = tokenBuf.length - bufLimit;
    	if (space >= 32) { 
    		// If enough space simply read as far as possible
    		// without overflowing the buffer, no shifting required
    		int read = reader.read(tokenBuf, bufLimit, space);
    		if (read == -1) {
    			eofReached = true;
    			read = 0;
    		}
    		bufLimit += read;
    		return;
    	}
    	// If not enough space, we'll have to either:
    	//  - flush the valid part of the buffer to the left,
    	//    to make space for new characters
    	//  - grow the buffer (in which case we also flush,
    	//	  while we're at it)
    	// We try to only grow the buffer it it looks like
    	// we are reaching a token whose length is not too far
    	// from the buffer's capacity.
    	if (startPos >= 128) {	// 1/8th of the buffer
    		System.arraycopy(tokenBuf, startPos,
    			tokenBuf, 0, bufLimit - startPos);
    	}
    	else {
    		char[] grownBuf = new char[2 * tokenBuf.length];
    		System.arraycopy(tokenBuf, startPos,
    			grownBuf, 0, bufLimit - startPos);
    		tokenBuf = grownBuf;
    	}
    	// Shifting positions
    	int shift = startPos;
    	absPos += shift;
    	startPos = 0;
    	curPos -= shift;
    	bufLimit -= shift;
    	lastPos -= shift;
    	for (int i = 0; i < memory.length; ++i) {
    		int v = memory[i];
    		if (v >= 0) {	// must be >= startPos before shift
    			v -= shift;
    			if (v < 0) throw new IllegalStateException();
    			memory[i] = v;
    		}
    	}
    }
    
    /**
     * @return the next character in buffer,
     * 	with the special value 0xFFFF used to denote end-of-input
     */
    protected final char getNextChar() {
    	// If there aren't any more valid characters in the buffer
    	if (curPos >= bufLimit) {
    		// either we've reached end-of-file or we
    		// need to refill
    		if (eofReached) return 0xFFFF;
    		try {
				refill();
			} catch (IOException e) {
				// re-throw as unchecked lexical error exception
				throw new LexicalError("IOException: " + e.getLocalizedMessage());
			}
    		// NB: refill() can only make bufLen grow,
    		// or set eofReached, so it's one recursive call at most
    		return getNextChar();
    	}
    	// Otherwise simply return the next char in line
    	return tokenBuf[curPos++];
    }
    
    /**
     * Starts the matching of a new token
     */
    protected final void startToken() {
    	startPos = curPos;
    	lastPos = curPos;
    	lastAction = -1;
    }
    
    /**
     * Marks the current position as the last terminal
     * state encountered
     * @param action	associated semantic action index
     */
    protected final void mark(int action) {
    	lastAction = action;
    	lastPos = curPos;
    }
    
    /**
     * Resets the current position to the last terminal
     * state encountered
     * @return the recorded semantic action
     */
    protected final int rewind() {
    	curPos = lastPos;
    	return lastAction;
    }
    
    /**
     * Ends the matching of the current token
     */
    protected final void endToken() {
    	startLoc = curLoc;
    	curLoc = new Position(startLoc.filename,
    		absPos + curPos, startLoc.line, startLoc.bol);
    }

    /**
     * @return the substring between the last started token
     * 	and the current position (exclusive)
     */
    protected final String getLexeme() {
    	return new String(tokenBuf, startPos, curPos - startPos);
    }
    
    /**
     * @return the position of the last lexeme start
     */
    public final Position getLexemeStart() {
    	return startLoc;
    }
    
    /**
     * @return the position of the last lexeme end
     */
    public final Position getLexemeEnd() {
    	return curLoc;
    }
    
    /**
     * @param start
     * @param end
     * @return the substring between positions {@code pos}
     *  and {@code end} (exclusive) in the token buffer
     */
    protected final String getSubLexeme(int start, int end) {
    	return new String(tokenBuf, start, end - start);
    }

    /**
     * @param start
     * @param end
     * @return the (optional) substring between positions {@code pos} 
     *  and {@code end} (exclusive) in the token buffer
     */
    @SuppressWarnings("null")
	protected final Optional<String> getSubLexemeOpt(int start, int end) {
    	if (start < 0)
    		return Optional.empty();
    	return Optional.of(new String(tokenBuf, start, end - start));
    }
    
    /**
     * @param pos
     * @return the character at position {@code pos}
     * 	in the token buffer
     */
    protected final char getSubLexemeChar(int pos) {
    	return tokenBuf[pos];
    }
    
    /**
     * @param pos
     * @return the (optional) character at position {@code pos}
     * 	in the token buffer
     */
    @SuppressWarnings("null")
    protected final Optional<Character> getSubLexemeOptChar(int pos) {
    	if (pos < 0)
    		return Optional.empty();
    	return Optional.of(tokenBuf[pos]);
    }
    
    /**
     * Updates the current position to account for a line change.
     * Is not called automatically by the lexer, but can be used
     * in semantic actions when matching a newline character.
     */
    protected final void newline() {
    	Position pos = curLoc;
    	curLoc = new Position(pos.filename, pos.offset, pos.line + 1, pos.offset);
    }
}