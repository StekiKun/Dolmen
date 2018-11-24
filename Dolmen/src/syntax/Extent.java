package syntax;

import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

import codegen.SourceMapping.Origin;
import common.Maps;

/**
 * Instances of this class describe ranges of characters
 * in files, and are used to link parsed entities with
 * their concrete representation in the original sources.
 * 
 * @author Stéphane Lescuyer
 */
public class Extent extends CExtent {

	/**
	 * The absolute filename where this extent should be interpreted
	 */
	public final String filename;
	
	/**
	 * The starting character offset of the extent (0-based)
	 */
	public final int startPos;
	/**
	 * The offset of the last character in the extent (0-based)
	 */
	public final int endPos;
	
	/**
	 * The line where the extent starts (1-based)
	 */
	public final int startLine;
	/**
	 * The column where the extent starts, i.e. the offset
	 * of the starting character from the beginning of the line
	 * (0-based)
	 */
	public final int startCol;
	
	/**
	 * Returns a new extent described by the given arguments
	 * 
	 * @param filename
	 * @param startPos
	 * @param endPos
	 * @param startLine
	 * @param startCol
	 */
	public Extent(String filename,
		int startPos, int endPos, int startLine, int startCol) {
		this.filename = filename;
		this.startPos = startPos;
		this.endPos = endPos;
		this.startLine = startLine;
		this.startCol = startCol;
	}
	
	/**
	 * A <i>dummy</i> extent for convenience
	 */
	public static final Extent DUMMY =
		new Extent("", -1, -1, -1, -1);

	@Override
	public String filename() {
		return filename;
	}
	
	@Override
	public int startPos() {
		return startPos;
	}

	@Override
	public int endPos() {
		return endPos;
	}
	
	@Override
	public int startLine() {
		return startLine;
	}
	
	@Override
	public int startCol() {
		return startCol;
	}
	
	/**
	 * @return the length, in characters, of the described extent
	 */
	@Override
	public int length() {
		return endPos - startPos + 1;
	}
	
	@Override
	public int realLength() {
		return length();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endPos;
		result = prime * result + filename.hashCode();
		result = prime * result + startCol;
		result = prime * result + startLine;
		result = prime * result + startPos;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Extent)) return false;
		Extent ext = (Extent) obj;
		if (startPos != ext.startPos) return false;
		if (endPos != ext.endPos) return false;
		if (startLine != ext.startLine) return false;
		if (startCol != ext.startCol) return false;
		if (!filename.equals(ext.filename)) return false;
		return true;
	}

	@Override
	public String toString() {
		if (this == DUMMY) return "Extent DUMMY";
		return "Extent [filename=" + filename + ", startPos=" + startPos 
				+ ", endPos=" + endPos + ", startLine="
				+ startLine + ", startCol=" + startCol + "]";
	}

	/**
	 * @return the string portion described by this extent
	 */
	@Override
	public String find() {
		if (this == DUMMY) return "";

		try (FileReader reader = new FileReader(filename)) {
			reader.skip(startPos);
			int length = endPos - startPos + 1;
			char[] buf = new char[length];
			for (int i = 0; i < length; ++i)
				buf[i] = (char) reader.read();
			return new String(buf);
		} catch (IOException e) {
			return "<Could not open file " + filename + ">";
		}
	}
	
	@Override
	public Origin findOrigin(int offset, int length) {
		if (this == DUMMY) throw new IllegalArgumentException();
		if (offset + length > length())
			throw new IllegalArgumentException();
		return new Origin(offset + startPos, length, Maps.empty());
	}
	
	private static final class Inlined extends Extent {
		/** The inlined contents that this extent represents */
		private final String contents;
		
		Inlined(String contents) {
			super("<inlined>", -1, -1, -1, -1);
			this.contents = contents;
		}
		
		@Override
		public int length() {
			return contents.length();
		}
		
		@Override
		public int hashCode() {
			return contents.hashCode();
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (o == null) return false;
			if (this.getClass() != o.getClass()) return false;
			Inlined inl = (Inlined) o;
			if (!contents.equals(inl.contents))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "Extent INLINED";
		}
		
		@Override
		public String find() {
			return contents;
		}
	}
	/**
	 * @param contents
	 * @return an inlined extent with the given {@code contents}
	 */
	public static Extent inlined(String contents) {
		return new Inlined(contents);
	}
}
