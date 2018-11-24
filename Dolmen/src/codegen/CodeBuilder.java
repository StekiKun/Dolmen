package codegen;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

import codegen.LexBuffer.Position;
import syntax.CExtent;
import syntax.Extent;

/**
 * An instance of this utility class manages a string
 * buffer with a current <i>indentation level</i>, which
 * allows for convenient declarative generation of 
 * user-readable code.
 * <p>
 * It can also be used to track the origins of some of the
 * regions of the generated code, typically in order to keep
 * track of pieces of code that are copied <i>verbatim</i> 
 * from some other source file into the code builder
 * (see {@link #withTracker(String, int)}).
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
		if (!cond) return this;
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
		return buf.toString();
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
	
	//=====================================
	//     Code tracking functionality
	//	(not as default behaviour for now)
	//=====================================
	
	private @Nullable SourceMapping smap = null;
	private int trackingBase = -1;
	private int lastTrackedOffset = -1;
	private @Nullable Position lastTracked = null;
	
	/**
	 * Configures this {@link CodeBuilder} to be ready
	 * to start tracking some regions of the emitted code.
	 * <b>Should only be called once: extra calls will be ignored.</b>
	 * <p>
	 * Once this instance is configured, the various methods that
	 * take advantage of region tracking can be used:
	 * <ul>
	 * <li> {@link #getSourceMapping()}
	 * <li> {@link #startTrackedRange(Position)}
	 * <li> {@link #endTrackedRange(CExtent)}
	 * <li> {@link #emitTracked(CExtent)}
	 * <li> {@link #emitTrackedIf(boolean, CExtent)}
	 * </ul>
	 * <p>
	 * The tracked regions will refer to their positions in the
	 * emitted code <b>relative to</b> the current amount of code
	 * already emitted when this method is called, plus the 
	 * given {@code offset}.
	 * 
	 * @param generated	the name describing the code being generated
	 * @param offset	a non-negative offset describing where this code
	 * 					is going to be emitted in the actual generated file
	 */
	public void withTracker(String generated, int offset) {
		if (offset < 0)
			throw new IllegalArgumentException("Negative offset " + offset + " is not allowed");
		if (this.smap != null) {
			System.out.println("Ignoring extra call to #withTracker(" + generated + ")");
			return;
		}
		this.smap = new SourceMapping(generated);
		this.trackingBase = buf.length() - offset;
	}
	
	/**
	 * @return the current source mappings representing
	 * 	the regions that have already been tracked
	 * @throws IllegalArgumentException if tracking has
	 * 	not been enabled with {@link #withTracker(String, int)}
	 */
	public SourceMapping getSourceMapping() {
		@Nullable SourceMapping smap_ = smap;
		if (smap_ == null)
			throw new IllegalArgumentException("No tracking was configured");
		return smap_;
	}
	
	/**
	 * Starts tracking a new region from the current position
	 * in the emitted code. The region will be mapped with the
	 * corresponding region at the given position {@code pos}
	 * in the source file.
	 * <p>
	 * <b>Only one region can be tracked at any given time.</b>
	 * Successive calls to {@link #startTrackedRange(Position)} 
	 * without any calls to {@link #endTrackedRange(CExtent)} will be
	 * ignored.
	 * 
	 * @see #endTrackedRange(CExtent)
	 * @param pos
	 * @throws IllegalArgumentException if tracking has
	 * 	not been enabled with {@link #withTracker(String, int)}
	 */
	public CodeBuilder startTrackedRange(Position pos) {
		getSourceMapping();
		if (lastTracked != null) {
			System.out.println("Already tracking some unclosed range: " + 
					"ignoring call to #startTrackedRange(" + pos + ")");
			return this;
		}
		lastTrackedOffset = buf.length();
		lastTracked = pos;
		return this;
	}
	
	/**
	 * Closes the currently tracked region and maps it to
	 * the source position given in the corresponding call
	 * to {@link #startTrackedRange(Position)}. It will now
	 * be registered into the {@link #getSourceMapping() source mappings}.
	 * <p>
	 * <i>If no region was being tracked, this call is ignored.</i>
	 * 
	 * @param extent	if non-{@code null}, describes how the emitted
	 * 					contents were obtained by instantiating placeholders
	 * 					in the corresponding source region
	 * 
	 * @throws IllegalArgumentException if tracking has
	 * 	not been enabled with {@link #withTracker(String, int)}
	 */
	public CodeBuilder endTrackedRange(@Nullable CExtent extent) {
		SourceMapping smap_ = getSourceMapping();
		@Nullable Position lastTracked_ = lastTracked;
		if (lastTracked_ == null) {
			System.out.println("No range being tracked: ignoring call to #endTrackedRange");
			return this;
		}
		smap_.add(lastTrackedOffset - trackingBase,
					buf.length() - lastTrackedOffset, lastTracked_, extent);
		lastTrackedOffset = -1;
		lastTracked = null;
		return this;
	}
	
	/**
	 * Convenient helper which emits the content of some
	 * verbatim piece of code described by the given extent,
	 * and maps it to the extent's original position. 
	 * <b>There should be no currently tracked region or
	 *  the results will be unexpected.</b>
	 * 
	 * @param extent
	 * 
	 * @throws IllegalArgumentException if tracking has
	 * 	not been enabled with {@link #withTracker(String, int)}
	 */
	public CodeBuilder emitTracked(CExtent extent) {
		getSourceMapping();
		
		startTrackedRange(new LexBuffer.Position(extent.filename(),
			extent.startPos(), extent.startLine(), extent.startPos() - extent.startCol()));
		emit(extent.find());
		// No need to record a simple linear extent
		endTrackedRange(extent instanceof Extent ? null : extent);
		
		return this;
	}
	
	/**
	 * Same as {@link #emitTracked(CExtent)} but is only
	 * performed if the given condition holds. Convenient
	 * for conditional output based on preferences/debug/etc.
	 * 
	 * @param cond
	 * @param extent
	 */
	public CodeBuilder emitTrackedIf(boolean cond, CExtent extent) {
		if (!cond) return this;
		return emitTracked(extent);
	}
}