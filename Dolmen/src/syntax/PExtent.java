package syntax;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import common.Lists;

/**
 * Instances of this class describe ranges of characters
 * in files, and are used to link parsed entities with
 * their concrete representation in the original sources.
 * <p>
 * The differences with {@link Extent} is that instances of
 * {@link PExtent} can contain special markers which are not
 * Java code and represent special parameters which can be
 * instantiated by typically other extents. These special
 * markers are called <i>holes</i> and are introduced by
 * a specific "magic" character.
 * <p>
 * An extent's {@linkplain #holes holes} are stored in order
 * of appearance in the extent, and must never overlap. This
 * is guaranteed by the associated {@link Builder} class.
 * 
 * @author Stéphane Lescuyer
 */
public class PExtent extends Extent {

	/**
	 * A {@link Hole} represents a special part of an {@link PExtent extent}
	 * which is bound to some {@link #name} and can be instantiated later
	 * on. A hole is characterized by its {@link #offset} relative to 
	 * its extent's {@linkplain Extent#startPos starting offset}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Hole {
		/**
		 * The offset of this hole in the enclosing extent
		 */
		public final int offset;
		
		/**
		 * The name to which this hole is bound, and through which
		 * the hole can be filled
		 */
		public final String name;
		
		/**
		 * Returns a new hole from the given parameters
		 * 
		 * @param offset
		 * @param name
		 */
		public Hole(int offset, String name) {
			this.offset = offset;
			this.name = name;
		}
		
		/**
		 * @return the length of the hole in the original
		 * 	extent, including the magic character introducing
		 *  the hole 
		 */
		public int length() {
			return name.length() + 1;
		}
		
		/**
		 * @return the end offset of the hole, i.e. the offset
		 * 	of the hole's last character in the original extent
		 */
		public int endOffset() {
			return offset + name.length();
		}
		
		@Override
		public int hashCode() {
			return 31 * offset + name.hashCode();
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof Hole)) return false;
			Hole hole = (Hole) o;
			if (offset != hole.offset) return false;
			if (!name.equals(hole.name)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			return String.format("{%s@+%d}", name, offset);
		}
	}
	
	/**
	 * The (possibly empty) list of holes in this parameterized extent.
	 * <p>
	 * The list must be <i>strictly ordered</i> in the sense that all
	 * holes appear in increasing order of {@linkplain Hole#offset offset},
	 * and that all the ranges described by the holes are disjoint.
	 */
	public final List<Hole> holes;
	
	/**
	 * Returns a new parameterized extent described by the given arguments
	 * 
	 * @param filename
	 * @param startPos
	 * @param endPos
	 * @param startLine
	 * @param startCol
	 * @param holes
	 */
	private PExtent(String filename,
		int startPos, int endPos, int startLine, int startCol,
		List<Hole> holes) {
		super(filename, startPos, endPos, startLine, startCol);
		this.holes = holes;
	}
	
	/**
	 * A <i>dummy</i> extent for convenience, with no holes
	 */
	public static final PExtent DUMMY =
		new PExtent("", -1, -1, -1, -1, Lists.empty());

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + holes.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof PExtent)) return false;
		PExtent ext = (PExtent) obj;
		if (!super.equals(ext)) return false;
		if (!holes.equals(ext.holes)) return false;
		return true;
	}

	@Override
	public String toString() {
		if (this == DUMMY) return "PExtent DUMMY";
		return "PExtent [filename=" + filename + ", startPos=" + startPos 
				+ ", endPos=" + endPos + ", startLine="
				+ startLine + ", startCol=" + startCol 
				+ ", holes=" + holes + "]";
	}

	/**
	 * @return the string portion described by this extent, 
	 *  <i>with the holes as they were</i>
	 */
	@Override
	public String find() {
		return super.find();
	}
	
	/**
	 * @param extent
	 * @return the given extent as a {@link PExtent}, with no holes
	 */
	public static PExtent ofExtent(Extent extent) {
		return new PExtent(extent.filename, extent.startPos, extent.endPos, 
			extent.startLine, extent.startCol, Lists.empty());
	}
	
	/**
	 * This is equivalent to {@code ofExtent(Extent.inlined(contents))}.
	 * 
	 * @param contents
	 * @return a parameterized extent with the given {@code contents},
	 * 	which must not contain any holes
	 */
	public static PExtent inlined(String contents) {
		return ofExtent(Extent.inlined(contents));
	}
	
	/**
	 * Builder class for {@linkplain PExtent parameterized extends}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Builder {
		private final String filename;
		private final int startPos;
		private final int startLine;
		private final int startCol;
		
		// The next source position at which a hole could legally be found,
		// the character after the last position of the last hole (or the start
		// of the extent if no holes have been added yet)
		private int curPos;
		private final List<Hole> holes;
		
		/**
		 * Starts building a parameterized extent from the given source position 
		 * 
		 * @param filename
		 * @param startPos
		 * @param startLine
		 * @param startCol
		 */
		public Builder(String filename, int startPos, int startLine, int startCol) {
			this.filename = filename;
			this.startPos =  startPos;
			this.startLine = startLine;
			this.startCol = startCol;
			
			this.curPos = startPos;
			this.holes = new ArrayList<>(4);
		}
		
		/**
		 * Adds a new hole at <i>source position</i> {@code sourcePos} and
		 * linked with the parameter {@code name}
		 * 
		 * @param sourcePos
		 * @param name
		 * @return this builder
		 */
		public Builder addHole(int sourcePos, String name) {
			int offset = sourcePos - startPos;
			Hole hole = new Hole(offset, name);
			if (sourcePos < curPos) {
				if (holes.isEmpty())
					throw new IllegalArgumentException("Cannot add hole " + hole + " before start of extent");
				else
					throw new IllegalArgumentException("Cannot add hole " + hole + 
						" which would overlap with " + holes.get(holes.size() - 1));
			}
			holes.add(hole);
			curPos = startPos + hole.endOffset();
			return this;
		}
		
		/**
		 * Closes the extent at the given source position {@code endPos}
		 * and returns it. This {@link Builder} instance must not be used
		 * anymore after this method has been called.
		 * 
		 * @param endPos
		 * @return the extent described by this builder and ending at {@code endPos}
		 */
		public PExtent build(int endPos) {
			if (endPos < curPos - 1)
				throw new IllegalArgumentException("Extent end offset " + 
						endPos + " is not large enough for hole " + holes.get(holes.size() - 1));
			return new PExtent(filename, startPos, endPos, startLine, startCol, holes);
		}
	}
}
