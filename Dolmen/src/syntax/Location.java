package syntax;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Instances of this class describe ranges of characters
 * in files, and are used to link parsed entities with
 * their concrete representation in the original sources.
 * 
 * @author St�phane Lescuyer
 */
public final class Location {

	/**
	 * The absolute filename where the location should be interpreted
	 */
	public final String filename;
	
	/**
	 * The starting character offset of the location (0-based)
	 */
	public final int startPos;
	/**
	 * The offset of the last character in the location (0-based)
	 */
	public final int endPos;
	
	/**
	 * The line where the location starts (1-based)
	 */
	public final int startLine;
	/**
	 * The column where the location start, i.e. the offset
	 * of the starting character from the beginning of the line
	 * (0-based)
	 */
	public final int startCol;
	
	/**
	 * Returns a new location described by the given arguments
	 * 
	 * @param filename
	 * @param startPos
	 * @param endPos
	 * @param startLine
	 * @param startCol
	 */
	public Location(String filename,
		int startPos, int endPos, int startLine, int startCol) {
		this.filename = filename;
		this.startPos = startPos;
		this.endPos = endPos;
		this.startLine = startLine;
		this.startCol = startCol;
	}
	
	/**
	 * A <i>dummy</i> location for convenience
	 */
	public static final Location DUMMY =
		new Location("", -1, -1, -1, -1);

	/**
	 * @return the length, in characters, of the described location
	 */
	public int length() {
		return endPos - startPos + 1;
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
		if (!(obj instanceof Location)) return false;
		Location loc = (Location) obj;
		if (startPos != loc.startPos) return false;
		if (endPos != loc.endPos) return false;
		if (startLine != loc.startLine) return false;
		if (startCol != loc.startCol) return false;
		if (!filename.equals(loc.filename)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "Location [filename=" + filename + ", startPos=" + startPos 
				+ ", endPos=" + endPos + ", startLine="
				+ startLine + ", startCol=" + startCol + "]";
	}

}
