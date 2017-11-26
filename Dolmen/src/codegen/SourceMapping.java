package codegen;

import java.util.LinkedList;
import java.util.ListIterator;

import org.eclipse.jdt.annotation.NonNull;

import codegen.LexBuffer.Position;
import syntax.Extent;

/**
 * This class stores a <i>mapping</i> from range of positions
 * in a generated file (typically a generated Java lexer or parser)
 * to corresponding positions in a source file (typically a Dolmen
 * lexer or parser description file), or even several source files.
 * <p>
 * The point of this information is to keep track of where semantic
 * actions (see {@link Extent}) end up in generated files, to be
 * able to translate features that rely on positions in generated
 * files (errors, markers, stacktrace information) back to the
 * corresponding positions in the source files if possible.
 * 
 * @author Stéphane Lescuyer
 */
public final class SourceMapping {
	
	/**
	 * A mapping describes a link between the range 
	 * described by {@link #offset} and {@link #length}
	 * in the generated file and the region of the
	 * same length starting at position {@link #origin}
	 * in the source file. 
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Mapping {
		private final int offset;
		private final int length;
		
		private final Position origin;
		
		Mapping(int offset, int length, Position origin) {
			this.offset = offset;
			this.length = length;
			this.origin = origin;
		}
		
		@Override
		public String toString() {
			return String.format(
				"[Range at %d, length %d: %s]",
				offset, length, origin);
		}
		
		/**
		 * @param m
		 * @return whether the mapping {@code m} maps
		 * 	a range which does not lie <b>strictly</b> before {@code this}
		 */
		boolean after(Mapping m) {
			return this.offset >= m.offset;
		}

		/**
		 * @param m
		 * @return whether the mapping {@code m} maps
		 * 	a range which does not lie <b>strictly</b> after {@code this}
		 */
		boolean before(Mapping m) {
			return this.offset <= m.offset;
		}
		
		/**
		 * @param toffset
		 * @param tlength
		 * @return {@code true} if the region described by
		 * 	{@code toffset} and {@code tlength} is entirely covered
		 * 	by this mapping
		 */
		boolean maps(int toffset, int tlength) {
			return (toffset >= offset && toffset + length <= offset + length);
		}
	}
	
	/**
	 * A name describing the input being mapped
	 */
	private final String mapped;
	
	/**
	 * The list of individual mappings, <b>sorted in increasing order
	 * of range offset</b>. It is expected that different mappings in the
	 * same instance of {@link SourceMapping} will be disjoint pair-wise
	 * but it is <b>not mandatory</b>: in case several mappings describe
	 * range that start at the same offset, they will appear in order
	 * of appearance.
	 */
	private final LinkedList<@NonNull Mapping> mappings;
	
	/**
	 * @param generated	a name describing the input being mapped
	 * 
	 * Constructs a fresh and empty {@link SourceMapping}
	 * describing mapping regions of {@code generated} to
	 * regions of one or several original sources
	 */
	public SourceMapping(String generated) {
		this.mappings = new LinkedList<>();
		this.mapped = generated;
	}
	
	/**
	 * Inserts a new mapping {@code m} in {@link #mappings}
	 * whilst preserving the ordering of {@link #mappings}
	 * @param m
	 */
	private void insert(Mapping m) {
		if (mappings.isEmpty()) {
			mappings.add(m);
			return;
		}
		if (m.after(mappings.getLast())) {
			mappings.addLast(m);
			return;
		}
		ListIterator<@NonNull Mapping> it = mappings.listIterator();
		while (it.hasNext()) {
			if (it.next().before(m)) continue;
			it.previous(); break;
		}
		it.add(m);
	}
	
	/**
	 * Adds a new mapping to {@code this}, describing that
	 * the range starting at position {@code offset} and spanning
	 * {@code length} bytes corresponds to the range of the same
	 * length at position {@code origin} in the original source
	 * 
	 * @param offset
	 * @param length
	 * @param origin
	 */
	public void add(int offset, int length, Position origin) {
		insert(new Mapping(offset, length, origin));
	}
	
	@Override
	public String toString() {
		if (mappings.isEmpty()) return "No mappings for " + mapped;
		StringBuilder buf = new StringBuilder();
		buf.append("Mappings for " + mapped + ": ");
		mappings.forEach(m -> buf.append(m.toString()));
		return buf.toString();
	}
	
	/**
	 * Tries to map the region described by {@code offset}
	 * and {@code length} to the original source via the
	 * mappings known in this instance
	 * 
	 * @param offset
	 * @param length
	 * @return the offset in the original source where
	 * 	the desired range can be found, or -1 if the described
	 * 	range is not entirely mapped
	 */
	public int map(int offset, int length) {
		for (Mapping m : mappings) {
			if (m.maps(offset, length)) {
				return offset - m.offset + m.origin.offset;
			}
		}
		return -1;	// no match
	}
}