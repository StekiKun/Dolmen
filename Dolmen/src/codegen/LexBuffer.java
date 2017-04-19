package codegen;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Instances of buffers used by generated lexers.
 * <p>
 * Lexical anlysers actually extend this class to inherit
 * a buffer with markers for token positions, final states,
 * and methods which are used by the generated automata,
 * as well as methods that can be used in semantic actions.
 * 
 * @author Stéphane Lescuyer
 */
public class LexBuffer {

    /**
     * Constructs a new lexer buffer based on the given character stream
     * @param reader
     */
    public LexBuffer(java.io.@Nullable Reader reader) {
    	if (reader == null) throw new IllegalArgumentException();
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
    }
    
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
    protected final char getNextChar() throws IOException {
    	// If there aren't any more valid characters in the buffer
    	if (curPos >= bufLimit) {
    		// either we've reached end-of-file or we
    		// need to refill
    		if (eofReached) return 0xFFFF;
    		refill();
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
    protected final void start() {
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
     * @return the substring between the last started token
     * 	and the current position (exclusive)
     */
    protected final String getLexeme() {
    	return new String(tokenBuf, startPos, curPos - startPos);
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
}