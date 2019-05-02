package org.stekikun.dolmen.codegen;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.codegen.LexBuffer.Position;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.syntax.CExtent;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.syntax.PExtent;
import org.stekikun.dolmen.syntax.PExtent.Hole;
import org.stekikun.dolmen.unparam.Expansion;

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
	public static final class Mapping {
		/**
		 * Character offset of the region being mapped
		 */
		public final int offset;
		/**
		 * Length in characters of the region being mapped
		 */
		public final int length;
		/**
		 * Original position of the mapped region
		 */
		public final Position origin;
		
		/**
		 * The composite extent describing the origin of
		 * the region in source, or {@code null} if the
		 * source region was just an exact reproduction
		 * of the mapped range
		 */
		private final @Nullable CExtent extent;
		
		Mapping(int offset, int length, Position origin, 
				@Nullable CExtent extent) {
			this.offset = offset;
			this.length = length;
			this.origin = origin;
			this.extent = extent;
			if (extent != null && extent.startPos() != origin.offset)
				throw new IllegalArgumentException("Mismatched offsets: origin=" + origin.offset
					+ ", extent=" + extent.startPos());
			if (extent != null && extent.realLength() != length)
				throw new IllegalArgumentException("Mismatched lengths: mapping=" + length 
					+ ", extent=" + extent.realLength());
		}
		
		@Override
		public String toString() {
			return String.format(
				"[Range at %d, length %d%s: %s]",
				offset, length, extent == null ? "" : ", composite", origin);
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
			return (toffset >= offset && toffset + tlength <= offset + length);
		}
		
		Origin findOrigin(int toffset, int tlength) {
			if (!maps(toffset, tlength))
				throw new IllegalArgumentException();
			// If there is no instantiation, the origin is straightforward
			@Nullable CExtent extent_ = extent; 
			if (extent_ == null)
				return new Origin(
					toffset - offset + origin.offset, tlength, null, Maps.empty());
			// Otherwise, we try and find the innermost extent (composite or not)
			// containing the whole range. It exists because at worst it is 
			// {@code extent} itself.
			return extent_.findOrigin(toffset - offset, tlength);
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
	 * length at position {@code origin} in the original source.
	 * <p>
	 * If the mapped region stems from expanding and instantiating
	 * the source region, the structure of the instantiation is
	 * given by {@code extent}. Otherwise {@code extent} must be 
	 * {@code null}.
	 * 
	 * @param offset
	 * @param length
	 * @param origin
	 * @param extent
	 */
	public void add(int offset, int length, 
			Position origin, @Nullable CExtent extent) {
		insert(new Mapping(offset, length, origin, extent));
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
	 * Iterates on all known mappings and 
	 * applies {@code consumer} on each one
	 * 
	 * @param consumer
	 */
	public void forEach(Consumer<? super Mapping> consumer) {
		mappings.forEach(consumer);
	}
	
	/**
	 * An instance of this class describes the origin of some region
	 * of generated code in terms of the source region from which it
	 * originated. In its simpler form, both regions have the same 
	 * {@link #length} and same contents, and the origin is characterized
	 * by its {@link #offset} in the source file.
	 * <p>
	 * It is also possible that the generated code be the result of
	 * {@link Expansion} and {@linkplain PExtent#instantiate instantiation}
	 * of placeholders in parameterized semantic actions. In that case,
	 * the source region may contain placeholders and be of a different
	 * length than the mapped region. In that case the textual replacements
	 * to perform to obtain the contents of the region in generated code
	 * is given by {@link #replacements}. The rule in the ground grammar
	 * which produced these instantiated actions is given in {@link #ruleName}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public final static class Origin {
		/** The starting offset of the source region */
		public final int offset;
		/** The length of the source region */
		public final int length;
		
		/** 
		 * The name of the (possibly instantiated) rule which 
		 * encloses this origin, if any, or {@code null} otherwise
		 */
		public final @Nullable String ruleName;
		/**
		 * The textual replacements for potential <i>holes</i> in the source region
		 */
		public final Map<String, String> replacements;
		
		/**
		 * Constructs an origin description from the given parameters
		 * @param offset	offset of the origin in source file (0-based)
		 * @param length	length of the origin in source file
		 * @param ruleName	name of the (ground) rule which encloses this origin
		 * @param replacements	textual replacements for holes
		 */
		public Origin(int offset, int length, 
				@Nullable String ruleName, Map<String, String> replacements) {
			this.offset = offset;
			this.length = length;
			this.ruleName = ruleName;
			this.replacements = replacements;
		}
		
		@Override
		public int hashCode() {
			int res = offset;
			res = 31 * res + length;
			// Ignoring rule name on purpose, 
			// as for replacements they are implied by location and rule name
			return res;
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof Origin)) return false;
			Origin origin = (Origin) o;
			if (offset != origin.offset) return false;
			if (length != origin.length) return false;
			// Ignoring rule name on purpose, 
			// as for replacements they are implied by location and rule name
			return true;
		}
		
		@Override
		public String toString() {
			String res = String.format("[%d, %d+%d]", offset, offset, length);
			if (replacements.isEmpty()) return res;
			res += "{ with " + replacements.toString() + "}";
			return res;
		}
	}
	
	/**
	 * Tries to map the region described by {@code offset}
	 * and {@code length} to the original source via the
	 * mappings known in this instance.
	 * <p>
	 * The returned origin does not necessarily match the
	 * given region's contents exactly, as the latter may
	 * be the result of instantiating {@linkplain Hole holes}
	 * in semantic actions.
	 * 
	 * @see Origin
	 * 
	 * @param offset
	 * @param length
	 * @return the {@linkplain Origin origin} in source where
	 * 	the desired range stemmed from, or {@code null} if
	 *  the described range is not entirely mapped in any of
	 *  these source mappings
	 */
	public @Nullable Origin map(int offset, int length) {
		for (Mapping m : mappings) {
			if (m.maps(offset, length))
				return m.findOrigin(offset, length);
		}
		return null;	// no match
	}
}