package automaton;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import automaton.DFA.Cell;
import automaton.DFA.GotoAction;
import automaton.DFA.MemAction;
import automaton.DFA.Perform;
import automaton.DFA.Remember;
import automaton.DFA.Shift;
import automaton.DFA.TransActions;
import common.CSet;
import syntax.Extent;
import syntax.IReport;
import syntax.IReport.Severity;
import syntax.Lexer;
import syntax.Reporter;
import tagged.TLexerEntry.Finisher;

/**
 * An instance of {@link Automata} gathers all the
 * deterministic finite automata implementing the
 * various rules in a lexer definition.
 * <p>
 * The automata are described by the reunion of 
 * their {@link #automataCells cells} and by a
 * {@link #automataEntries description} of each 
 * sub-automaton associated to a lexer entry, providing
 * its initial state, memory size for tag handling,
 * initializer and finisher actions, etc.
 * 
 * @author Stéphane Lescuyer
 * @see Determinize#lexer(syntax.Lexer, boolean)
 */
public final class Automata {

	/**
	 * Represents automaton information for one lexer
	 * entry. Provides the {@link #memSize memory size} 
	 * required to execute the automaton, the
	 * {@link #initialState initial state number} and
	 * the {@link #initializer associated actions}, 
	 * and the {@link #finishers finishers} associated
	 * to the semantic actions.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Entry {
		/** Whether this rule is public or not */
		public final boolean visibility;
		/** The name of this rule */
		public final String name;
		/** The return type of this rule */
		public final Extent returnType;
		/** The formal arguments for this rule */
		public final @Nullable Extent args;
		/** The number of memory cells required */
		public final int memSize;
		
		/** The number of the initial state for this rule */
		public final int initialState;
		/** The initial memory actions */
		public final List<@NonNull MemAction> initializer;
		
		/** 
		 * The list of finishers associated to 
		 * semantic actions for this rule
		 */
		public final List<@NonNull Finisher> finishers;
		
		/**
		 * Builds an automaton entry from the given arguments
		 * 
		 * @param name
		 * @param returnType
		 * @param args
		 * @param memSize
		 * @param initialState
		 * @param initializer
		 * @param finishers
		 */
		public Entry(boolean visibility, String name, Extent returnType,
				@Nullable Extent args, int memSize, int initialState, 
				List<MemAction> initializer, List<Finisher> finishers) {
			this.visibility = visibility;
			this.name = name;
			this.returnType = returnType;
			this.args = args;
			this.memSize = memSize;
			this.initialState = initialState;
			this.initializer = initializer;
			this.finishers = finishers;
		}
		
		StringBuilder append(StringBuilder buf) {
			buf.append(name);
			Extent args_ = args;
			if (args_ == null) buf.append("()");
			else buf.append("(").append(args_.find()).append(")");
			buf.append("\n memSize = ").append(memSize);
			buf.append("\n initial = ").append(initialState);
			buf.append("\n initializer = ").append(initializer);
			buf.append("\n finishers = ");
			finishers.forEach(fi -> buf.append("\n   ").append(fi));
			return buf;
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			append(buf);
			return buf.toString();			
		}
	}

	/** The imports to be added to the generated lexer */
	public final List<@NonNull String> imports;
	/** The location of this lexer's header */
	public final Extent header;
	/** The list of automata entries, one for each lexer rule */
	public final List<@NonNull Entry> automataEntries;
	/** The cells of the various automata implementing the rules */
	public final DFA.@NonNull Cell[] automataCells;
	/** The location of this lexer's footer */
	public final Extent footer;

	/**
	 * Builds the automata from the given entries and cells
	 * 
	 * @param imports
	 * @param header
	 * @param footer
	 * @param automataEntries
	 * @param automataCells
	 */
	public Automata(List<String> imports, Extent header, Extent footer,
		List<Entry> automataEntries, DFA.@NonNull Cell[] automataCells) {
		this.imports = imports;
		this.header = header;
		this.footer = footer;
		this.automataEntries = automataEntries;
		this.automataCells = automataCells;
		if (!sanityCheck())
			throw new IllegalArgumentException("Sanity check failed");
	}

	private boolean sanityCheck() {
		for (DFA.Cell cell : automataCells)
			if (!cell.sanityCheck()) return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		int num = 0;
		for (Entry entry : automataEntries) {
			if (num > 0) buf.append("\n");
			buf.append("Entry ").append(num).append(": ");
			buf.append(entry);
			++num;
		}
		buf.append("\nCells (").append(automataCells.length).append("):");
		for (int i = 0; i < automataCells.length; ++i) {
			buf.append("\n ").append(i).append(": ");
			buf.append(automataCells[i]);
		}
		return buf.toString();
	}
	
	/**
	 * Checks the automata which must correspond to the lexer description {@code lexer}
	 * for any problems and reports all of them in {@code reporter}.
	 * <p>
	 * The following issues can be reported:
	 * <ul>
	 * <li> clauses which are never used, i.e. those for which the semantic action
	 *      is never performed;
	 * <li> entries which can produce empty token exceptions.
	 * </ul>
	 * 
	 * @param lexer
	 * @return the (potentially empty) list of reports for the problems found
	 * 	in this {@link Automata} instance
	 */
	public List<@NonNull IReport> findProblems(Lexer lexer) {
		Reporter reporter = new Reporter();
		for (Lexer.Entry entry : lexer.entryPoints) {
			Optional<Entry> aentry = automataEntries.stream()
				.filter(e -> e.name.equals(entry.name.val))
				.findAny();
			if (!aentry.isPresent())
				throw new IllegalStateException("Lexer entry " + entry.name.val
					+ " has no corresponding entry in automaton: " + this.toString());
			findProblemsInEntry(reporter, entry, aentry.get().initialState);
		}
		return reporter.getReports();
	}
	
	/**
	 * Checks the automaton entry whose initial state is {@code initialState}
	 * and must correspond to the syntactic lexer entry {@code entry} for issues,
	 * and report all of them in {@code reporter}.
	 * <p>
	 * The following issues can be reported:
	 * <ul>
	 * <li> clauses which are never used, i.e. those for which the semantic action
	 *      is never performed;
	 * <li> entries which can produce empty token exceptions.
	 * </ul>
	 * 
	 * @param reporter
	 * @param entry
	 * @param initialState
	 */
	private void findProblemsInEntry(Reporter reporter, 
			Lexer.Entry entry, int initialState) {
		// First look for potential token errors
		findEmptyTokenStates(reporter, entry, initialState);
		
		Set<Integer> visited = new HashSet<>();
		Stack<Integer> todo = new Stack<>();
		todo.push(initialState);
		// We also track which semantic actions were found in final states
		boolean reachable[] = new boolean[entry.clauses.size()];
		Arrays.fill(reachable, false);
		// Visit all states reachable from [initialState]
		while (!todo.isEmpty()) {
			int s = todo.pop();
			
			if (!visited.add(s)) continue;
			
			// Handle the corresponding cell: if final, record
			// the corresponding action as reachable ; if not a
			// sink state record all possible successors
			Cell cell = automataCells[s];
			switch (cell.getKind()) {
			case PERFORM: {
				final Perform perform = (Perform) cell;
				reachable[perform.action] = true;
				break;
			}
			case SHIFT: {
				final Shift shift = (Shift) cell;
				if (shift.remember != Remember.NOTHING)
					reachable[shift.remember.action] = true;
				
				for (TransActions trans : shift.transTable.values()) {
					if (trans.gotoAction == GotoAction.BACKTRACK) continue;
					int target = trans.gotoAction.target;
					if (!visited.contains(target))
						todo.push(target);
				}
				break;
			}
			}
		}
		
		// Now report all unreachable actions in this lexer entry
		int i = 0;
		for (Map.Entry<syntax.@NonNull Regular, @NonNull Extent> clause : 
			entry.clauses.entrySet()) {
			if (!reachable[i]) {
				String msg = String.format(
					"This clause is never used (entry %s, clause #%d)", entry.name.val, i);
				reporter.add(
					IReport.of(msg, Severity.WARNING, clause.getValue()));
			}
			++i;
		}
		
	}

	/**
	 * Finds the set of states which can be reached from {@code initialState}
	 * without encountering any final state, along with "witness" strings, i.e.
	 * sequence of characters which show how these states can be reached from
	 * the initial state. If any of these states has a {@link GotoAction#BACKTRACK}
	 * transition, a report is filed into {@code reporter} which gives examples
	 * of unmatched input sequences.
	 * 
	 * @param reporter
	 * @param entry
	 * @param initialState
	 */
	private void findEmptyTokenStates(Reporter reporter,
			Lexer.Entry entry, int initialState) {
		Map<Integer, String> emptyTokenStates = new HashMap<>();
		Stack<Map.Entry<Integer, String>> todo = new Stack<>(); 
		todo.push(new AbstractMap.SimpleEntry<>(initialState, ""));
		TreeSet<String> emptyTokenWitnesses = new TreeSet<>();
		
		while (!todo.isEmpty()) {
			final Map.Entry<Integer, String> e = todo.pop();
			final int s = e.getKey();
			if (emptyTokenStates.containsKey(s)) continue;
			final String witness = e.getValue();
			final Cell cell = automataCells[s];
			
			switch (cell.getKind()) {
			case PERFORM: {
				@SuppressWarnings("unused")
				final Perform perform = (Perform) cell;
				// This cell is a sink, nothing to do
				break;
			}
			case SHIFT: {
				final Shift shift = (Shift) cell;
				if (shift.remember != Remember.NOTHING) continue;
				// This state is not final, if it isn't known already
				// we need to visit its successors.
				// If the transition table can backtrack from there, this
				// state can lead to an empty token error, and we must
				// report it.
				emptyTokenStates.put(s, witness);
				shift.transTable.forEach((cset, trans) -> {
					if (trans.gotoAction == GotoAction.BACKTRACK) {
						char c = CSet.witnesses(cset).iterator().next();
						String witc = witness + " '" + CSet.charToString(c) + "'";
						emptyTokenWitnesses.add(witc);
					}
					else {
						int target = trans.gotoAction.target;
						if (!emptyTokenStates.containsKey(target)) {
							char c = CSet.witnesses(cset).iterator().next();
							String witc = witness + " '" + CSet.charToString(c) + "'";
							todo.push(new AbstractMap.SimpleEntry<>(target, witc));
						}
					}
				});
				break;
			}
			}
		}
		
		// If there are some inputs leading to empty tokens, 
		// report them (or at least the first 10)
		if (emptyTokenWitnesses.isEmpty()) return;
		
		StringBuilder buf = new StringBuilder();
		buf.append("The lexer entry ").append(entry.name.val)
			.append(" cannot recognize all possible input sequences.\n");
		buf.append("Here are examples of input sequences which will result in an empty token error:\n");
		int i = 0;
		for (String wit : emptyTokenWitnesses.descendingSet()) {
			buf.append(" -").append(wit).append("\n");
			++i;
			if (i == 10) {
				buf.append(" ...\n");
				break;
			}
		}
		buf.append("You may want to add '_' or 'orelse' catch-all clauses and provide a better error report.");
		
		reporter.add(IReport.of(buf.toString(), Severity.WARNING, entry.name));
	}

}
