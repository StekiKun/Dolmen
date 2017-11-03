package syntax;

import java.util.ArrayList;
import java.util.Collections;

/**
 * An instance of {@link Reporter} can be used to
 * collect many problem {@link IReport reports} together.
 * 
 * @author Stéphane Lescuyer
 */
public final class Reporter {

	private final ArrayList<IReport> reports;
	
	/**
	 * Creates a fresh empty reporter
	 */
	public Reporter() {
		this.reports = new ArrayList<IReport>();
	}
	
	/**
	 * Adds the given {@code report} to the reporter
	 * 
	 * @param report
	 */
	public void add(IReport report) {
		reports.add(report);
	}
	
	/**
	 * @return a read-only view of the reports
	 * 	collected in this reporter so far
	 */
	public Iterable<IReport> getReports() {
		return Collections.unmodifiableList(reports);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("================================\n");
		buf.append(reports.size()).append(" problems reported");
		int i = 1;
		for (IReport r : reports) {
			buf.append("\n");
			buf.append("- Problem ").append(i++).append(":\n");
			buf.append(r.display());
		}
		return buf.toString();
	}
}
