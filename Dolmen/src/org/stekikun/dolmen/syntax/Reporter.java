package org.stekikun.dolmen.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.stekikun.dolmen.syntax.IReport.Severity;

/**
 * An instance of {@link Reporter} can be used to
 * collect many problem {@linkplain IReport reports} together.
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
	public List<IReport> getReports() {
		return Collections.unmodifiableList(reports);
	}
	
	/**
	 * @return whether at least one of the collected reports 
	 * 	has the severity {@link Severity#ERROR}
	 */
	public boolean hasErrors() {
		return reports.stream().anyMatch(r -> r.getSeverity() == Severity.ERROR);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("================================\n");
		int sz = reports.size();
		if (sz == 0)
			buf.append("No problems reported");
		else if (sz == 1)
			buf.append(sz).append(" problem reported");
		else
			buf.append(sz).append(" problems reported");
		int i = 1;
		for (IReport r : reports) {
			buf.append("\n");
			buf.append("- Problem ").append(i++).append(":\n");
			buf.append(r.display());
		}
		return buf.toString();
	}
}
