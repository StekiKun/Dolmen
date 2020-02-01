package org.stekikun.dolmen.syntax;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.codegen.SourceMapping.Origin;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.unparam.Expansion;
import org.stekikun.dolmen.unparam.Grammar;

/**
 * Instances of this class describe ranges of characters
 * in files, and are used to link parsed entities with
 * their concrete representation in the original sources.
 * <p>
 * These are called <i>extents</i> and in their general form,
 * they can be constructed by the instantiation of 
 * {@linkplain PExtent parameterized extents} where holes
 * have been replaced with extents taken from other parts
 * of the file, or from other files even. Such extents
 * are called <i>composite</i> and arise during the
 * {@linkplain Expansion expansion} phase from a
 * {@linkplain PGrammar parametric grammar} into a
 * {@linkplain Grammar ground grammar}.
 * 
 * @see Extent	a non-composite implementation of {@link CExtent}
 * 
 * @author Stéphane Lescuyer
 */
public abstract class CExtent {

	/**
	 * @return the absolute filename where this extent should be interpreted
	 */
	public abstract String filename();
	
	/**
	 * @return the starting character offset of the extent (0-based)
	 */
	public abstract int startPos();
	/**
	 * @return the offset of the last character in the extent (0-based)
	 */
	public abstract int endPos();
	
	/**
	 * @return the line where the extent starts (1-based)
	 */
	public abstract int startLine();

	/**
	 * @return the column where the extent starts, i.e. the offset
	 * of the starting character from the beginning of the line
	 * (0-based)
	 */
	public abstract int startCol();

	/**
	 * @return the length of this extent
	 */
	public abstract int length();
	
	/**
	 * The <i>real</i> length of an extent is the actual length
	 * of its representation once all place-holders have been
	 * replaced.
	 * <p>
	 * It can be different from {@link #length()}, the length of
	 * the extent as it appeared uninstantiated in the original
	 * source.
	 * 
	 * @return the real length of this extent
	 * @see #find()
	 */
	public abstract int realLength();
	
	/**
	 * The string must be of length {@link #realLength()}.
	 * 
	 * @return the string portion described by this extent
	 */
	public abstract String find();
	
	/**
	 * Finding the origin in a composite extent amounts to finding
	 * the innermost extent, composite or not, whose instantiation
	 * covers the given region.
	 * 
	 * @param offset	<b>relative</b> offset from the start of this extent
	 * @param length
	 * @return the origin of the region described by the relative 
	 *  {@code offset} and {@code length}
	 * 
	 * @see Origin
	 * @throws IllegalArgumentException if this extent does
	 * 	not cover the whole region described by {@code offset} 
	 *  and {@code length}
	 */
	public abstract Origin findOrigin(int offset, int length);
	
	/**
	 * @param extent
	 * @param children	the replacements for {@code extent}'s {@link PExtent#holes}, in order
	 * @param ruleName	the ground rule which produced this composite extent
	 * @return a composite extent representing the instantiation of
	 * 	the parameterized extent {@code extent} with the composite
	 *  extents given in {@code children}
	 */
	public static CExtent of(PExtent extent, List<CExtent> children, String ruleName) {
		return new Composite(extent, children, ruleName);
	}
	
	/**
	 * An actual <i>composite</i> extent made of a parameterized
	 * extent {@link #extent} and the list of composite extents
	 * {@link #children} that must replace the holes in {@link #extent}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Composite extends CExtent {
		// The parameterized extent which was instantiated
		private final PExtent extent;
		// The extents that were substituted in place of 
		// {@code extent}'s holes, in order
		private final List<CExtent> children;
		// The (ground) rule whose generation produced 
		// this composite extent
		private final String ruleName;
		
		// Cached version of this composite's extent actual 
		// final contents, with the holes in {@code extent}
		// substituted for the contents in {@code children}
		private final String realization;
	
		Composite(PExtent extent, List<CExtent> children, String ruleName) {
			this.extent = extent;
			this.children = children;
			this.ruleName = ruleName;
			if (extent.holes.size() != children.size())
				throw new IllegalArgumentException(
						"Size mismatch between extent holes (" + extent.holes.size() +
						") and children (" + children.size() + ")");
			this.realization = realize();
		}
	
		@Override
		public String filename() {
			return extent.filename;
		}
		
		@Override
		public int startPos() {
			return extent.startPos;
		}

		@Override
		public int endPos() {
			return extent.endPos;
		}
		
		@Override
		public int startLine() {
			return extent.startLine;
		}
		
		@Override
		public int startCol() {
			return extent.startCol;
		}

		@Override
		public int length() {
			return extent.length();
		}
		
		@Override
		public int realLength() {
			return realization.length();
		}

		@Override
		public String find() {
			return realization;
		}
	
		private String realize() {
			final String raw = extent.find();
			if (extent.holes.isEmpty()) return raw;

			StringBuilder buf = new StringBuilder();
			int offset = 0;
			int idx = 0;
			for (PExtent.Hole hole : extent.holes) {
				buf.append(raw, offset, hole.offset);
				String replacement = children.get(idx++).find();
				buf.append(replacement);
				offset = hole.endOffset() + 1;
			}
			if (offset < raw.length())
				buf.append(raw, offset, raw.length());
			return buf.toString();
		}
		
		@Override
		public Origin findOrigin(int offset, int length) {
			final int end = offset + length;
			if (end > realLength())
				throw new IllegalArgumentException();
			if (extent.holes.isEmpty())
				return new Origin(offset + startPos(), length, ruleName, Maps.empty());
			boolean inGap = false;
			int lastPOffset = 0;
			int realOffset = 0;
			int idx = 0;
			int shift = 0;
			find_tighter: {
				for (PExtent.Hole hole : extent.holes) {
					final int gap = hole.offset - lastPOffset;
					final int nextPOffset = hole.endOffset() + 1;
					final int realHoleOffset = realOffset + gap;
					final CExtent child = children.get(idx);
					final int nextRealOffset = realHoleOffset + child.realLength();
					// Is the region in the gap to the next hole? If so,
					// we know this extent is the innermost one
					if (offset >= realOffset && end <= realOffset + gap) {
						inGap = true;
						break find_tighter;
					}
					// Is the region in the hole? If so, we can look 
					// recursively in the replacement for the hole/
					if (offset >= realHoleOffset && end < nextRealOffset) {
						return child.findOrigin(offset - realHoleOffset, length);
					}
					// If the region starts before the next gap, we can
					// stop looking
					if (offset < nextRealOffset)
						break find_tighter;
					
					lastPOffset = nextPOffset;
					realOffset = nextRealOffset;
					shift += (child.realLength() - hole.length());
					++idx;
				}
				// Our last chance is the final gap, we should be in it or
				// we would have aborted earlier on.
				if (!(offset >= realOffset && end <= realLength()))
					throw new IllegalStateException();
				inGap = true;
			}
			// If we end up here, this extent is the innermost one
			// containing the whole region. The replacements are only
			// needed if we were not in a gap.
			return new Origin(offset - shift + startPos(), length,
					ruleName, inGap ? Maps.empty() : textualReplacements());
		}
		
		/**
		 * @return a map that binds every hole name in {@link #extent}
		 * 	to the actual textual contents that should replace it
		 *  according to {@link #children}
		 */
		private Map<String, String> textualReplacements() {
			LinkedHashMap<String, String> res = new LinkedHashMap<>();
			int idx = 0;
			for (PExtent.Hole hole : extent.holes) {
				res.put(hole.name, children.get(idx).find());
				idx++;
			}
			return res;
		}
		
		@Override
		public int hashCode() {
			int result = extent.hashCode();
			result = 31 * result + children.hashCode();
			return result;
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof Composite)) return false;
			Composite comp = (Composite) o;
			if (!extent.equals(comp.extent)) return false;
			if (!children.equals(comp.children)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "CExtent [filename=" + filename() + ", startPos=" + startPos() 
					+ ", endPos=" + endPos() + ", startLine="
					+ startLine() + ", startCol=" + startCol() 
					+ ", holes=" + extent.holes 
					+ ", chilren=" + children + "]";
		}
	}
}