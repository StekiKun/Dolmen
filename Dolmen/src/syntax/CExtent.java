package syntax;

import java.util.List;

/**
 * @WIP
 * Composite extents
 * 
 * @author Stéphane Lescuyer
 */
public abstract class CExtent {

	/**
	 * @retrn the absolute filename where this extent should be interpreted
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
	 * @param extent
	 * @param children	the replacements for {@code extent}'s {@link PExtent#holes}, in order
	 * @return a composite extent representing the instantiation of
	 * 	the parameterized extent {@code extent} with the composite
	 *  extents given in {@code children}
	 */
	public static CExtent of(PExtent extent, List<CExtent> children) {
		return new Composite(extent, children);
	}
	
	/**
	 * TODO
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Composite extends CExtent {
		
		private final PExtent extent;
		private final List<CExtent> children;
		private final String realization;
	
		Composite(PExtent extent, List<CExtent> children) {
			this.extent = extent;
			this.children = children;
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
	}
}