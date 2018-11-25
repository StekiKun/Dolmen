package syntax;

import syntax.PExtent.Hole;

/**
 * Common interface for reports of problems found during 
 * the generation of lexical analyzers or parsers
 * 
 * @see #of(String, Severity, CExtent)
 * @see #of(String, Severity, PExtent, PExtent.Hole)
 * @see #of(String, Severity, Located)
 * 
 * @author Stéphane Lescuyer
 */
public interface IReport {

	/**
	 * Enum describing the severity of the reported problem
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static enum Severity {
		/** Severity of errors */
		ERROR("Error"),
		/** Severity of warnings */
		WARNING("Warning"),
		/** Severity of information logs */
		LOG("Log");
		
		/** User-friendly name for the severity */
		public final String name;
		
		private Severity(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	/**
	 * @return a user-friendly message describing the report
	 */
	public String getMessage();
	
	/**
	 * @return the path name of the file where the problem 
	 * 	should be reported
	 */
	public String getFilename();
	
	/**
	 * @return the absolute offset of the range where the
	 * 	problem should be reported (0-based)
	 */
	public int getOffset();

	/**
	 * @return the line number where the
	 * 	problem should be reported (1-based)
	 */
	public int getLine();
	
	/**
	 * @return the column number where the
	 * 	problem should be reported (0-based)
	 */
	public int getColumn();
	
	/**
	 * @return the length of the range where the
	 * 	problem should be reported, in characters
	 */
	public int getLength();
	
	/**
	 * @return the {@link Severity severity} of the problem
	 */
	public Severity getSeverity();
	
	/**
	 * A default implementation for {@link IReport}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static class Impl implements IReport {
		private final String message;
		private final String filename;
		private final int startPos;
		private final int endPos;
		private final int line;
		private final int column;
		private final Severity severity;
		
		private Impl(String message, Severity severity, CExtent extent) {
			this.message = message;
			this.severity = severity;
			this.filename = extent.filename();
			this.startPos = extent.startPos();
			this.endPos = extent.endPos() + 1;	// extent are inclusives
			this.line = extent.startLine();
			this.column = extent.startCol();
		}

		private Impl(String message, Severity severity, PExtent extent, Hole hole) {
			this.message = message;
			this.severity = severity;
			this.filename = extent.filename;
			this.startPos = extent.startPos + hole.offset;
			this.endPos = extent.startPos + hole.endOffset() + 1; // holes are inclusives
			this.line = hole.startLine;
			this.column = hole.startCol;
		}
		
		private Impl(String message, Severity severity, Located<?> loc) {
			this.message = message;
			this.severity = severity;
			this.filename = loc.start.filename;
			this.startPos = loc.start.offset;
			this.endPos = loc.end.offset;	// locations are exclusives
			this.line = loc.start.line;
			this.column = loc.start.column() - 1;
		}
		
		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public String getFilename() {
			return filename;
		}

		@Override
		public int getOffset() {
			return startPos;
		}

		@Override
		public int getLine() {
			return line;
		}

		@Override
		public int getColumn() {
			return column;
		}

		@Override
		public int getLength() {
			return endPos - startPos;
		}

		@Override
		public Severity getSeverity() {
			return severity;
		}
	}
	
	/**
	 * @param message
	 * @param severity
	 * @param extent
	 * @return a report with the given message and severity, whose location
	 * 	is the one of {@code extent}
	 */
	public static IReport of(String message, Severity severity, CExtent extent) {
		return new Impl(message, severity, extent);
	}

	/**
	 * @param message
	 * @param severity
	 * @param extent
	 * @param hole
	 * @return a report with the given message and severity, whose location
	 * 	is the one of {@code hole} in {@code extent}
	 */
	public static IReport of(String message, Severity severity, PExtent extent, Hole hole) {
		return new Impl(message, severity, extent, hole);
	}
	
	/**
	 * @param message
	 * @param severity
	 * @param loc
	 * @return a report with the given message and severity, at the location
	 * 	of the object {@code loc}
	 */
	public static IReport of(String message, Severity severity, Located<?> loc) {
		return new Impl(message, severity, loc) {
			@Override
			public String display() {
				if (loc.start.line == loc.end.line) return super.display();
				// Try and display a multi-line error if needed
				StringBuilder buf = new StringBuilder();
				buf.append("File \"").append(getFilename()).append("\", ");
				buf.append("lines ").append(getLine()).append("-")
					.append(loc.end.line).append(", ");
				buf.append("characters ").append(getColumn()).append("-")
					.append(loc.end.column() - 1).append(":\n");
				buf.append(getSeverity()).append(": ").append(getMessage());
				return buf.toString();
			}
		};
	}
	
	/**
	 * The default implementation displays the severity, the message, 
	 * and the source location of the reported problem, in an Emacs-like
	 * fashion.
	 * <p>
	 * It can be overridden by implementers.
	 * 
	 * @return a string fully describing the report
	 */
	public default String display() {
		StringBuilder buf = new StringBuilder();
		buf.append("File \"").append(getFilename()).append("\", ");
		buf.append("line ").append(getLine()).append(", ");
		buf.append("characters ").append(getColumn()).append("-")
			.append(getColumn()+getLength()).append(":\n");
		buf.append(getSeverity()).append(": ").append(getMessage());
		return buf.toString();
	}
}
