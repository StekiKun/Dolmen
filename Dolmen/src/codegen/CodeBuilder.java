package codegen;

import java.io.IOException;

import common.Nulls;

/**
 * An instance of this utility class manages a string
 * buffer with a current <i>indentation level</i>, which
 * allows for convenient declarative generation of 
 * user-readable code.
 * 
 * @author Stéphane Lescuyer
 */
public final class CodeBuilder {
	/**
	 * Amount of indentation per level
	 */
	public final static int PER_LEVEL = 4;
	
	// TODO make that an Appendable instead
	// 	so we can write directly in an output stream
	//	for instance. But that means dealing with
	//	IO exceptions everywhere.
	
	private final StringBuilder buf;
	
	/** 
	 * Current amount of indentation 
	 * (including the {@link #PER_LEVEL} factor) 
	 */
	private int indentLevel;
	
	/**
	 * Returns a fresh code builder starting with
	 * the given indentation level, and using a
	 * fresh instance of {@link StringBuilder} as
	 * the internal target of emitted code
	 * 
	 * @param level
	 */
	public CodeBuilder(int level) {
		this.buf = new StringBuilder(8092);
		this.indentLevel = level * PER_LEVEL;
	}
	
	/**
	 * @return the current indentation level
	 * (not the number of spaces, but the number of
	 * 	nested blocks, starting at 0)
	 */
	public int getCurrentLevel() {
		return indentLevel / PER_LEVEL;
	}

	/**
	 * Increments the indentation level by 1
	 * (and the indentation amount by {@link #PER_LEVEL})
	 */
	public CodeBuilder incrIndent() {
		indentLevel += PER_LEVEL;
		return this;
	}

	/**
	 * Starts a new line with the current indentation amount
	 */
	public CodeBuilder newline() {
		buf.append("\n");
		for (int i = 0; i < indentLevel; ++i)
			buf.append(' ');
		return this;
	}
	
	/**
	 * Decrements the indentation level by 1
	 * (and the indentation amount by {@link #PER_LEVEL}
	 */
	public CodeBuilder decrIndent() {
		indentLevel -= PER_LEVEL;
		return this;
	}
	
	/**
	 * Appends the given code, which should not
	 * contain newline characters
	 * 
	 * @param code
	 */
	public CodeBuilder emit(String code) {
		buf.append(code);
		return this;
	}
	
	/**
	 * Appends the given line of code and emits a terminal
	 * {@link #newline()}
	 * 
	 * @param line	should not contain newline characters
	 */
	public CodeBuilder emitln(String line) {
		emit(line);
		newline();
		return this;
	}

	/**
	 * Appends the given code, which should not
	 * contain newline characters, provided the condition
	 * {@code cond} holds
	 * 
	 * @param cond
	 * @param code
	 */
	public CodeBuilder emitIf(boolean cond, String code) {
		if (!cond) return this;
		buf.append(code);
		return this;
	}
	
	/**
	 * Appends the given line of code and emits a terminal
	 * {@link #newline()}, provided the condition
	 * {@code cond} holds
	 * 
	 * @param cond
	 * @param line	should not contain newline characters
	 */
	public CodeBuilder emitlnIf(boolean cond, String line) {
		emit(line);
		newline();
		return this;
	}

	
	/**
	 * Opens a new curly-braced block with incremented
	 * indentation, i.e. is equivalent to:
	 * <pre>
	 * 	emit(" {");
	 *  incrIndent();
	 *  newline();
	 * </pre>
	 */
	public CodeBuilder openBlock() {
		emit(" {");
		incrIndent();
		newline();
		return this;
	}

	/**
	 * Closes a curly-braced block with decremented
	 * indentation and without breaking the line after
	 * the closing brace, i.e. is equivalent to:
	 * <pre>
	 *  decrIndent();
	 * 	newline();
	 *  emit("}");
	 * </pre>
	 * It is useful when closing several blocks in a row
	 * to avoid empty lines.	
	 */
	public CodeBuilder closeBlock0() {
		decrIndent();
		newline();
		emit("}");
		return this;
	}

	/**
	 * Closes a curly-braced block with decremented
	 * indentation, i.e. is equivalent to:
	 * <pre>
	 *  decrIndent();
	 *  newline();
	 * 	emit("}");
	 *  newline();
	 * </pre>
	 */
	public CodeBuilder closeBlock() {
		closeBlock0();
		newline();
		return this;
	}
	
	/**
	 * Returns the current contents of this code builder buffer
	 */
	public String contents() {
		return Nulls.ok(buf.toString());
	}
	
	/**
	 * Prints all code emitted thus far in this
	 * code builder into the provided {@code appendable} 
	 * 
	 * @param appendable
	 * @throws IOException
	 */
	public void print(Appendable appendable) throws IOException {
		appendable.append(buf.toString());
	}
}