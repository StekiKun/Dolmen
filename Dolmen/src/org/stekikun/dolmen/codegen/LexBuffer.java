package org.stekikun.dolmen.codegen;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Exceptions.DolmenVersionException;

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
	 * A position is given by the {@linkplain #filename description} of the input,
	 * the {@linkplain #offset absolute position} in said input, the {@link #line}
	 * number in the input where this position occurs, and the 
	 * {@linkplain #bol offset of said line} in the input. The column number can
	 * be retrieved via {@link #column()}.
	 * <p>
	 * {@link LexBuffer} uses {@link Position} to register the positions in 
	 * the current input of the last lexeme start and the current position (which
	 * happens to be the last lexeme <i>end</i> when in a semantic action).
	 * {@link LexBuffer}s do not manage line numbers by themselves, only absolute
	 * character offsets, so that updating {@link #line} and {@link #bol} is
	 * the responsibility of the lexer's semantic actions.
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
			return String.format(
				"[file=%s, char=%d, line=%d, col=%d]",
				filename, offset, line, column());
		}
	}
	
    /**
     * Exception which can be raised by generated lexers which
     * extend {@link LexBuffer}, and which is raised also by
     * {@link LexBuffer#getNextChar()} in place of potential
     * {@link IOException}s.
     * 
     * @see LexBuffer#error
     */
    public static final class LexicalError extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        /** The position in input at which the error occurred */
        public final @Nullable Position pos;
        
        /**
         * @param pos	the position in input at which the error occurred
         * @param msg	error message
         */
        public LexicalError(@Nullable Position pos, @Nullable String msg) {
            super(msg + (pos == null ? "" : 
            	String.format(" (%s, at line %d, column %d)", pos.filename, pos.line, pos.column())));
            this.pos = pos;
        }
    }

	/**
	 * A container class describing the lexer state with respect to some input
	 * stream. It is used to save the lexing context when temporary moving to some
	 * new input stream, and contains everything required to be able to later
	 * restart lexing from the position where it was interrupted.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Input {
	    /**
	     * The name of the input, for locations 
	     * (for error reports only, need not be an actual filename) 
	     */
		final String filename;

		/** The character stream to read from this input */
		final java.io.Reader reader;

		/** The character buffer associated to this input */
		final char[] tokenBuf;

	    /** 
	     * The extent of valid chars in {@link #tokenBuf},
	     * i.e. the index of the first non-valid character 
	     */
		final int bufLimit;

		/** Absolute position of the start of the buffer */
		final int absPos;

		/** Whether end-of-file was reached in {@link #reader} */
		final boolean eofReached;

		/** Current buffer input position */
		final int curPos;

		/** Current token position */
		protected Position curLoc;

		Input(LexBuffer lexbuf) {
			this.filename = lexbuf.filename;
			this.reader = lexbuf.reader;
			this.tokenBuf = lexbuf.tokenBuf;
			this.bufLimit = lexbuf.bufLimit;
			this.absPos = lexbuf.absPos;
			this.eofReached = lexbuf.eofReached;
			this.curPos = lexbuf.curPos;
			this.curLoc = lexbuf.curLoc;
		}
	}
	
    /**
     * Constructs a new lexer buffer based on the given character stream.
     *
	 * @param version	the version of Dolmen which generated the subclass
     * @param filename
     * @param reader
	 * @throws DolmenVersionException if {@code version} is not equal to
	 * 	the version of this {@code LexBuffer}
     */
    protected LexBuffer(String version, 
    		@Nullable String filename, java.io.@Nullable Reader reader) {
    	DolmenVersionException.checkLexer(version);
    	if (filename == null || reader == null)
    		throw new IllegalArgumentException();
		// Same as #init(filename, reader), but calling #init will anger
		// the compiler with respect to final or non-nullable fields.
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
	 * Resets this lexer buffer to read from the start of the given 
	 * input source
	 * 
	 * @param filename		name describing the input
	 * @param reader		input character stream
	 */
	private void init(String filename, java.io.Reader reader) {
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
		this.startLoc = hasPositions ? new Position(filename) : DUMMY_POS;
		this.curLoc = startLoc;
	}

	/**
	 * A lazily-allocated stack of {@link Input} objects 
	 * remembering input streams which have been interrupted
	 */
	private @Nullable Stack<Input> inputs = null;

    /**
     * The name of the input, for locations 
     * (for error reports only, need not be an actual filename) 
     */
    protected String filename;
    
    /** The character stream to feed the lexer */
    private java.io.Reader reader;
    
    /** The local character buffer */
    private char[] tokenBuf;

    /** 
     * The extent of valid chars in {@link #tokenBuf},
     * i.e. the index of the first non-valid character 
     */
    private int bufLimit;
    
    /** Absolute position of the start of the buffer */
    @DolmenInternal 
    protected int absPos;

    /** Whether end-of-file was reached in {@link #reader} */
    private boolean eofReached;
    
    /** Buffer input position of the token start */
    @DolmenInternal 
    protected int startPos;
    
    /** Current buffer input position */
    @DolmenInternal 
    protected int curPos;
    
    /** Last action remembered */
    private int lastAction;
    
    /** Position of last action remembered */
    private int lastPos;
    
    /** Memory cells */
    @DolmenInternal 
    protected int memory[];
    
    /**
     * Whether position tracking in {@link #startLoc} and {@link #curLoc}
     * is enabled. It is enabled by default.
     */
    private boolean hasPositions = true;
    
    /** Position of the last token start */
    protected Position startLoc;
    
    /** Current token position */
    protected Position curLoc;

    /**
     * A dummy position used for {@link #startLoc} and {@link #curLoc}
     * when position tracking is {@linkplain #disablePositions() disabled}.
     * <p>
     * It is guaranteed to be different from any valid position.
     */
    protected static final Position DUMMY_POS =
        new Position("<Lexer positions disabled>", -1, -1, -1);
        
    /**
     * @return whether position tracking is enabled in this lexer
     */
    public boolean hasPositions() {
    	return hasPositions;
    }
    
    /***
	 * Disables position tracking in this lexer.
	 * <p>
	 * Should be called first before the lexer is used,
	 * and cannot be re-enabled later.
     */
    public void disablePositions() {
    	if (!hasPositions()) return;
    	this.hasPositions = false;
    	this.startLoc = this.curLoc = DUMMY_POS;
    }
    
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
    @DolmenInternal 
    protected final char getNextChar() {
    	// If there aren't any more valid characters in the buffer
    	if (curPos >= bufLimit)
    		// fetch more characters in the buffer and return one
    		return getMoreChars();
    	// Otherwise simply return the next char in line
    	return tokenBuf[curPos++];
    }
    
    /**
     * Load more characters from the current input into the buffer,
     * if possible.
     * 
     * @return the next available character if the load was
     * 	successful, or 0xFFFF if the end-of-input has been reached.
     */
    private final char getMoreChars() {
		// either we've reached end-of-file or we
		// need to refill
		if (eofReached) return 0xFFFF;
		try {
			refill();
		} catch (IOException e) {
			// re-throw as unchecked lexical error exception
			Position errPos = new Position(startLoc.filename,
		    		absPos + curPos, startLoc.line, startLoc.bol);
			throw new LexicalError(errPos, "IOException: " + e.getLocalizedMessage());
		}
		// NB: refill() can only make bufLen grow,
		// or set eofReached, so it's one recursive call at most
		return getNextChar();    	
    }
    
    /**
     * Starts the matching of a new token
     */
    @DolmenInternal 
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
    @DolmenInternal 
    protected final void mark(int action) {
    	lastAction = action;
    	lastPos = curPos;
    }
    
    /**
     * Resets the current position to the last terminal
     * state encountered
     * @return the recorded semantic action
     */
    @DolmenInternal 
    protected final int rewind() {
    	curPos = lastPos;
    	return lastAction;
    }
    
    /**
     * Ends the matching of the current token
     */
    @DolmenInternal 
    protected final void endToken() {
    	if (!hasPositions) return;
    	startLoc = curLoc;
    	curLoc = new Position(startLoc.filename,
    		absPos + curPos, startLoc.line, startLoc.bol);
    }
    
    /**
     * This function is useful in cases when a lexer's semantic
     * action must act depending on what kind of input follows the
     * current token in the stream (although arguably at that point it's
     * not lexing anymore but parsing!).
     * 
     * @return the next character in the stream (-1 for end-of-input)
     * 	but does not advance the lexer engine
     */
    protected final char peekNextChar() {
    	char c = getNextChar();
    	if (c == 0xFFFF) return c;
    	--curPos;
    	return c;
    }

    /**
     * This function fetches the next characters in the stream
     * and returns them into the given character buffer {@code chars}.
     * It does so without advancing the state of the lexing engine so
     * that on return the lexer can resume from the original position.
     * <p>
     * This function is useful in cases when a lexer's semantic
     * action must act depending on what kind of input follows the
     * current token in the stream (although arguably at that point it's
     * not lexing anymore but parsing!).
     * 
     * @see #peekNextChar()
     * 
     * @return the number of look-ahead characters from the stream
     * 	actually inserted into {@code chars}
     */
    protected final int peekNextChars(char[] chars) {
    	int max = chars.length;
    	int i = 0;
    	for (; i < max; ++i) {
    		char c = getNextChar();
    		if (c == 0xFFFF) break;
    		chars[i] = c;
    	}
    	curPos -= i;
    	return i;
    }
    
	/**
	 * This changes the input stream used by this lexer buffer to the
	 * new source described by {@code filename} and {@code reader}.
	 * <p>
	 * In contrast to {@link #pushInput(String, java.io.Reader)}, this
	 * does not allow resuming the analysis of the former stream once
	 * the new one is complete.	This takes care of closing the input
	 * stream which was in use until that point.
	 * 
	 * @param filename		name of the new input source
	 * @param reader		new input character stream
	 */
	protected final void changeInput(String filename, java.io.Reader reader) {
		if (filename == null || reader == null)
			throw new IllegalArgumentException();
		try {
			this.reader.close();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot close input stream: " + e.getMessage());
		}
		init(filename, reader);
	}
	
	/**
	 * This pushes the current input stream to the internal input stack
	 * and resets the lexer to read from the given {@code reader}.
	 * Subsequent tokens will consume characters from the new stream.
	 * <p>
	 * The syntactic analysis of the former input stream can be resumed
	 * in the exact same position by a subsequent call to {@link #popInput()}. 
	 * 
	 * @param filename		name of the new input source
	 * @param reader		new input character stream
	 */
	protected final void pushInput(String filename, java.io.Reader reader) {
		if (filename == null || reader == null)
			throw new IllegalArgumentException();
		// Push the state of the current input to the input stack
		Stack<Input> linputs = inputs;
		if (linputs == null) {
			linputs = new Stack<>();
			inputs = linputs;
		}
		linputs.add(new Input(this));
		// Reinitialize the current lexer to use the new input
		init(filename, reader);
	}
	
	/**
	 * @return {@code true} if and only if there is more input
	 * 	to fall back to once the current input stream is over,
	 * 	i.e. if {@link #popInput} will succeed
	 */
	protected final boolean hasMoreInput() {
		return (inputs != null && !inputs.isEmpty());
	}
	
	/**
	 * This fetches the input stream which is at the stop of the input
	 * stack. After this call, the lexer will continue reading from that
	 * input in the exact spot where it was interrupted by a call
	 * to {@link #pushInput(String, java.io.Reader)}.
	 * <p>
	 * This takes care of closing the input stream which was in use
	 * until that point.
	 * 
	 * @see #hasMoreInput()
	 * 
	 * @throws IllegalArgumentException when the input stack is empty
	 */
	protected final void popInput() {
		Stack<Input> linputs = inputs;
		if (linputs == null || linputs.isEmpty())
			throw new IllegalArgumentException("No more input streams available");
		try {
			this.reader.close();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot close input stream: " + e.getMessage());
		}
		Input input = linputs.pop();
		this.filename = input.filename;
		this.reader = input.reader;
		this.tokenBuf = input.tokenBuf;
		this.bufLimit = input.bufLimit;
		this.absPos = input.absPos;
		this.eofReached = input.eofReached;
		this.startPos = input.curPos;
		this.curPos = input.curPos;
		this.lastAction = -1;
		this.lastPos = 0;
		this.memory = new int[0];
		this.startLoc = input.curLoc;
		this.curLoc = input.curLoc;
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
     * @return the length of the last matched lexeme
     */
    public final int getLexemeLength() {
    	return curPos - startPos;
    }
    
    /**
     * When successful, this is equivalent to {@code getLexeme().charAt(idx)},
     * but will be more efficient in general since it does not require
     * allocating the lexeme string as {@link #getLexeme()} does.
     * <p>
     * Beware that this method must only be called in the associated semantic
     * action, and <b>before</b> any call to a nested rule, as leaving the
     * action or entering another rule can change the underlying buffer.
     * 
     * @param idx
     * @return the character at index {@code idx} in the last
     * 	matched lexeme
     * @throws LexicalError when {@code idx} is negative or not less
     * 	than the {@link #getLexemeLength() length} of the lexeme
     */
    protected final char getLexemeChar(int idx) {
    	if (idx < 0 || idx >= getLexemeLength())
			throw error("Invalid index  " + idx 
						+ " in lexeme of length " + getLexemeLength());
    	return tokenBuf[startPos + idx];
    }

    /**
     * This method returns the same sequence of characters that
     * {@link #getLexeme()} would, but it is only a view based on the
     * current state of the {@link LexBuffer}. Therefore it can be more
     * efficient than {@link #getLexeme()} when the only requirement is
     * iterating on the characters in sequence, for instance appending
     * the contents to a {@link StringBuilder}.
     * <p>
     * The downside is that the result of {@link #getLexemeChars()} must
     * be used with more care, as it depends on the current state of
     * the buffer. It only makes sense during the associated semantic
     * action, and <b>before</b> any call to a nested entry rule as well.
     * Otherwise, the contents or size of the underlying buffer may
     * have changed and the contents of the returned {@link CharSequence}
     * is undefined.
     * 
     * @return the sequence of characters forming the last matched lexeme
     */
    protected final CharSequence getLexemeChars() {
    	return new LexemeCharSequence(startPos, curPos);
    }
    
    /**
     * An implementation of {@link CharacterSequence} which is backed up
     * by the underlying {@link LexBuffer}. It represents a range inside
     * the current buffer, and can be used to retrieve a lexeme efficiently
     * without actually copying the lexeme's data to a standalone {@link String},
     * for cases where the semantic action does not need more than a
     * {@link CharSequence}.
     * <p>
     * Of course, instances of this class will only make sense for the duration
     * of the associated semantic action, and before any possible call to
     * a nested rule. Otherwise, the underlying data may have been replaced,
     * the buffer may have been resized, etc.
     * 
     * @author Stéphane Lescuyer
     */
    private final class LexemeCharSequence implements CharSequence {
    	// Absolute start offset in the underlying LexBuffer
    	private final int start;
    	// Absolute end offset in the underlying LexBuffer
    	private final int end;
    	// Length of the sequence in chars
    	private final int length;
    	
    	LexemeCharSequence(int start, int end) {
    		this.start = start;
    		this.end = end;
    		this.length = end - start;
    	}

		@Override
		public int length() {
			return length;
		}

		@Override
		public char charAt(int index) {
			if (index < 0 || index >= length)
				throw error("Invalid index " + index
						+ " in character sequence of length " + length);
			return tokenBuf[start + index];
		}

		@Override
		public CharSequence subSequence(int sstart, int send) {
	        if ((sstart < 0) || (send > length) || (sstart > send))
	        	throw new IndexOutOfBoundsException();
	        return new LexemeCharSequence(start + sstart, start + send);
		}
		
		@Override
	    public IntStream chars() {
	        class CharIterator implements PrimitiveIterator.OfInt {
	            int cur = start;

	            @Override public boolean hasNext() {
	                return cur < end;
	            }

	            @Override public int nextInt() {
	                if (hasNext()) {
	                    return tokenBuf[cur++];
	                } else {
	                    throw new NoSuchElementException();
	                }
	            }
	        }

	        return StreamSupport.intStream(
	        	Spliterators.spliterator(new CharIterator(), length, Spliterator.ORDERED),
	            false);
	    }
    }
    
    /**
     * Appends the last matched lexeme to the given buffer {@code buf}.
     * This is equivalent to {@code buf.append(getLexeme())} or
     * {@code buf.append(getLexemeChars())} but will be faster than
     * both.
     * 
     * @param buf	the buffer to append the last lexeme to
     */
    protected void appendLexeme(StringBuilder buf) {
    	buf.append(tokenBuf, startPos, curPos - startPos);
    }
    
    /**
     * @param start
     * @param end
     * @return the substring between positions {@code pos}
     *  and {@code end} (exclusive) in the token buffer
     */
    @DolmenInternal 
    protected final String getSubLexeme(int start, int end) {
    	return new String(tokenBuf, start, end - start);
    }

    /**
     * @param start
     * @param end
     * @return the (optional) substring between positions {@code pos} 
     *  and {@code end} (exclusive) in the token buffer
     */
    @DolmenInternal 
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
    @DolmenInternal 
    protected final char getSubLexemeChar(int pos) {
    	return tokenBuf[pos];
    }
    
    /**
     * @param pos
     * @return the (optional) character at position {@code pos}
     * 	in the token buffer
     */
    @DolmenInternal 
    protected final Optional<Character> getSubLexemeOptChar(int pos) {
    	if (pos < 0)
    		return Optional.empty();
    	return Optional.of(tokenBuf[pos]);
    }
    
    /**
     * Convenience helper which returns a {@link LexicalError}
     * located at the current lexeme start.
     * <p>
     * It is also used by the lexer generator to report empty tokens, 
     * i.e. input which does not match any of the lexer rules. It can
     * be overriden in generated lexers to allow for customized message
     * and position reports.
     * 
     * @param msg
     * @return the exception with the given message and the current
     * 	token start position
     */
    protected LexicalError error(String msg) {
    	return new LexicalError(getLexemeStart(), msg);
    }

    /**
     * Updates the current position to account for a line change.
     * Is not called automatically by the lexer, but can be used
     * in semantic actions when matching a newline character.
     */
    protected final void newline() {
    	if (!hasPositions) return;
    	Position pos = curLoc;
    	curLoc = new Position(pos.filename, pos.offset, pos.line + 1, pos.offset);
    }
    
    /**
     * Same as {@link #savePosition}{@code (supplier, getLexemeStart())}.
     * <p>
     * This is exactly equivalent to the following code:
     * <pre>
     *   Position saved = getLexemeStart();
     *   T res = supplier.get();	// Calling the routine
     *   startLoc = saved;		// Restoring the start position
     * </pre>
     * <p>
     * 
     * @param supplier	a routine to run 
     * @return the value returned by running the given {@code supplier}
     * 
     * @see #savePosition(Supplier, Position)
     * @see #getLexemeStart()
     */
	protected final <T> T saveStart(Supplier<T> supplier) {
		return savePosition(supplier, getLexemeStart());
	}
	
    /**
     * This method runs the given {@code supplier} routine but takes
     * care to restore the current token start position to the 
     * position given as the {@code saved} parameter.
     * <p>
     * This is exactly equivalent to the following code:
     * <pre>
     *   T res = supplier.get();	// Calling the routine
     *   startLoc = saved;		// Restoring the start position
     * </pre>
     * This is useful in semantic actions which have only recognized
     * part of a syntactic construct (typically a complex literal opening
     * delimiter, such as the opening double-quote of a literal string)
     * and which call other rules of the lexer recursively
     * in order to finish analyzing the current construct. In such
     * cases, one may want to return the final resulting token as if
     * its span covered the whole range from the opening construct.
     * Calling nested rules inside a lambda passed to this method
     * is a way to achieve this.
     * 
     * @param supplier	a routine to run
     * @param saved		the starting position to save 
     * @return the value returned by running the given {@code supplier}
     * 
     * @see #saveStart(Supplier)
     */	
	protected final <T> T savePosition(Supplier<T> supplier, Position saved) {
		T res = supplier.get();
		startLoc = saved;
		return res;
	}

    /**
     * Same as {@link #savePosition}{@code (runnable, getLexemeStart())}.
     * <p>
     * This is exactly equivalent to the following code:
     * <pre>
     *   Position saved = getLexemeStart();
     *   supplier.run();	// Calling the routine
     *   startLoc = saved;		// Restoring the start position
     * </pre>
     * <p>
     * 
     * @param runnable 	a routine to run 
     * 
     * @see #savePosition(Runnable, Position)
     * @see #getLexemeStart()
     */
	protected final void saveStart(Runnable runnable) {
		savePosition(runnable, getLexemeStart());
	}
	
    /**
     * This method runs the given {@code runnable} but takes
     * care to restore the current token start position to the 
     * position given as the {@code saved} parameter.
     * <p>
     * This is exactly equivalent to the following code:
     * <pre>
     *   supplier.run();	// Calling the routine
     *   startLoc = saved;		// Restoring the start position
     * </pre>
     * This is useful in semantic actions which have only recognized
     * part of a syntactic construct (typically a complex literal opening
     * delimiter, such as the opening double-quote of a literal string)
     * and which call other rules of the lexer recursively
     * in order to finish analyzing the current construct. In such
     * cases, one may want to return the final resulting token as if
     * its span covered the whole range from the opening construct.
     * Calling nested rules inside a lambda passed to this method
     * is a way to achieve this.
     * 
     * @param runnable 	a routine to run
     * @param saved		the starting position to save 
     * 
     * @see #saveStart(Runnable)
     */	
	protected final void savePosition(Runnable runnable, Position saved) {
		runnable.run();
		startLoc = saved;
	}
}