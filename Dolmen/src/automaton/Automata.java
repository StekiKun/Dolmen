package automaton;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import automaton.DFA.MemAction;
import syntax.Location;
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
		public final Location returnType;
		/** The formal arguments for this rule */
		public final @Nullable Location args;
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
		public Entry(boolean visibility, String name, Location returnType,
				@Nullable Location args, int memSize, int initialState, 
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
			Location args_ = args;
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
			@SuppressWarnings("null")
			@NonNull String res = buf.toString();
			return res;			
		}
	}

	/** The imports to be added to the generated lexer */
	public final List<@NonNull String> imports;
	/** The location of this lexer's header */
	public final Location header;
	/** The list of automata entries, one for each lexer rule */
	public final List<@NonNull Entry> automataEntries;
	/** The cells of the various automata implementing the rules */
	public final DFA.@NonNull Cell[] automataCells;
	/** The location of this lexer's footer */
	public final Location footer;

	/**
	 * Builds the automata from the given entries and cells
	 * 
	 * @param imports
	 * @param header
	 * @param footer
	 * @param automataEntries
	 * @param automataCells
	 */
	public Automata(List<String> imports, Location header, Location footer,
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
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}
	
}
