package syntax;

/**
 * Instances of this class describe ranges of characters
 * in files, and are used to link parsed entities with
 * their concrete representation in the original sources.
 * 
 * @author Stéphane Lescuyer
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
}
