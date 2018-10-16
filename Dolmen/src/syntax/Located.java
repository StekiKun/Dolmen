package syntax;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import codegen.LexBuffer.Position;

/**
 * This class allows wrapping values of some type
 * with a start and end {@linkplain Position positions}
 * as provided by the lexing engine. It only supports
 * wrapping non-null values and the start and end
 * positions must always specify the same 
 * {@linkplain Position#filename source}.
 * <p>
 * <b>Warning:</b> Equality test on located values is just
 * forwarded to the values themselves; in other words, the actual
 * positions are ignored when comparing instances of {@link Located}.
 * Typically, this means one can use located values as keys in a map
 * in an intermediate AST as if they were not located, but must be wary
 * of potential duplicate keys at different spots during the parsing 
 * phase.
 * <p>
 * {@link Located#toString()} is implemented in such a way that
 * pairs of start/end positions are written in the following
 * format:
 * <pre>
 * (`start source`[`start offset`, `start line`+`start column offset`]..
 *  `end source`[`end offset`, `end line`+`end column offset`])
 * </pre
 * where line offsets are 1-indexed and absolute offsets and column offsets
 * are 0-indexed.
 * 
 * @author Stéphane Lescuyer
 *
 * @param <T>
 */
public final class Located<@NonNull T> {
	/** The value whose location is stored in this instance */
	public final T val;
	/** The start position of the value in the input file */
	public final Position start;
	/** The end position of the value in the input file */
	public final Position end;
		
	private Located(T val, Position start, Position end) {
		if (!start.filename.equals(end.filename))
			throw new IllegalArgumentException("Cannot create location with"
					+ "start and end positions from different sources: "
					+ "start=" + start + ", end=" + end);
		this.val = val;
		this.start = start;
		this.end = end;
	}
	
	@Override
	public int hashCode() {
		return val.hashCode();
	}
	
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (!(o instanceof Located<?>))
			return false;
		Located<?> loc = (Located<?>) o;
		return val.equals(loc.val);
	}
	
	private static StringBuilder appendPos(StringBuilder buf, Position pos) {
		int n = pos.filename.lastIndexOf('/');
		String file = n == -1 ? pos.filename : pos.filename.substring(n + 1);
		buf.append(file).append("[");
		buf.append(pos.line).append(",").append(pos.bol).append("+").append(pos.offset - pos.bol);
		buf.append("]");
		return buf;
	}

	private static String locToString(Position start, Position end) {
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		appendPos(buf, start).append("..");
		appendPos(buf, end).append(")");
		return buf.toString();
	}

	@Override
	public @NonNull String toString() {
		return val.toString() + " " + locToString(start, end);
	}
	
	/**
	 * @return the length of this location
	 */
	public int length() {
		return end.offset - start.offset;
	}
	
	/**
	 * @param val
	 * @param start
	 * @param end
	 * @return the given value {@code val} wrapped with the given 
	 * 	{@code start} and {@code end} positions
	 */
	public static <@NonNull T> Located<T> of(T val, Position start, Position end) {
		return new Located<T>(val, start, end);
	}
	
	private static final Position DUMMY_POS = new Position("<DUMMY>");
	/**
	 * @param val
	 * @return the given value with a dummy start and end position, located
	 * 	at the beginning of an imaginary file called "&lt;DUMMY&gt;"
	 */
	public static <@NonNull T> Located<T> dummy(T val) {
		return new Located<T>(val, DUMMY_POS, DUMMY_POS);
	}
	
	/**
	 * @param val
	 * @param loc
	 * @return the valule {@code val} wrapped with the positions
	 * 	of {@code loc}
	 */
	public static <@NonNull T, @NonNull U> Located<T> like(T val, Located<U> loc) {
		return new Located<T>(val, loc.start, loc.end);
	}
}