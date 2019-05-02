package org.stekikun.dolmen.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.syntax.IReport.Severity;

/**
 * A lexer definition is a set of {@linkplain Entry lexer rules}
 * along with arbitrary header and footer sections. The rules
 * come in no particular order, as every rule can theoretically
 * be used as an entry point to the generated lexer. When only
 * one entry point really makes sense, it is conventionally
 * the first rule.
 * 
 * @author Stéphane Lescuyer
 */
public final class Lexer {

	/**
	 * A clause associates a {@linkplain Regular regular expression}
	 * to a {@linkplain Extent Java semantic action}. Each lexer
	 * {@link Entry} is made of a number of clauses, and a clause's
	 * semantic action can be taken when the input matches the
	 * clause's regular expression.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public final static class Clause {
		/** The regular expression of this clause */
		public final Located<Regular> regular;
		/** The Java action which can be executed when this clauses matches */
		public final Extent action;
		
		/**
		 * @param regular
		 * @param action
		 */
		public Clause(Located<Regular> regular, Extent action) {
			this.regular = regular;
			this.action = action;
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("| ").append(regular.val);
			buf.append(" {").append(action.find()).append("}");
			return buf.toString();
		}
	}
	
	/**
	 * A lexer rule entry is a sequence of regular expressions
	 * associated to semantic actions. A rule has a
	 * {@linkplain #name name}, a {@linkplain #returnType return type} 
	 * and may have some {@linkplain #args arguments}. The regular expressions
	 * that form the rule are called the {@linkplain #clauses clauses}
	 * and are normally interpreted with the <i>longest match 
	 * priority</i>, unless {@linkplain #shortest specified otherwise}.
	 * 
	 * <p>
	 * NB: In state-based lexer generators like JLex or JavaCC,
	 * 	the equivalent of a lexer rule entry is a lexer state along
	 * 	with all the rules that apply to this state. The equivalent
	 * 	of changing lexer state when matching a regular expression
	 * 	is to call the corresponding rule in the semantic action.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public final static class Entry {
		/** The visibility of this entry point */
		public final boolean visibility;
		/** The name of this entry */
		public final Located<String> name;
		/** Whether shortest match should be used */
		public final boolean shortest;
		/** The formal arguments for this rule */
		public final @Nullable Extent args;
		/** The return type of semantic actions for this entry */
		public final Extent returnType;
		
		/** 
		 * The various clauses for this entry, associated to
		 * the extents for the corresponding semantic actions.
		 * <b>Iteration order in {@link #clauses} is relevant
		 * as priority between same-length matches goes to the
		 * first matching rule in this map.</b>
		 */
		public final List<Clause> clauses;
		
		/**
		 * @param visibility
		 * @param name
		 * @param returnType
		 * @param shortest
		 * @param args
		 * @param clauses
		 * 
		 * Builds a new lexer entry with the provided arguments.
		 * Beware that order in {@code clauses} is relevant.
		 */
		public Entry(boolean visibility, 
				Located<String> name, Extent returnType, boolean shortest,
				@Nullable Extent args, List<Clause> clauses) {
			this.visibility = visibility;
			this.name = name;
			this.returnType = returnType;
			this.shortest = shortest;
			this.args = args;
			this.clauses = clauses;
		}
		
		StringBuilder append(StringBuilder buf, String kword) {
			buf.append(kword).append(" ").append(name.val);
			Extent args_ = args;
			if (args_ == null) buf.append("()");
			else buf.append("(").append(args_.find()).append(")");
			buf.append(" : ").append(returnType.find());
			buf.append(" = ").append(shortest ? "shortest" : "parse");
			clauses.forEach(clause -> {
				buf.append("\n").append(clause.toString());
			});
			return buf;
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			append(buf, "rule");
			return buf.toString();
		}
		
		/**
		 * A builder class for {@linkplain Lexer.Entry lexer entries},
		 * which lets one add clauses incrementally
		 * 
		 * @author Stéphane Lescuyer
		 */
		public static final class Builder {
			private final boolean visibility;
			private final Located<String> name;
			private boolean shortest;
			private final @Nullable Extent args;
			private final Extent returnType;
			private final List<Clause> clauses;

			/**
			 * Constructs a fresh builder with longest-match rule
			 * and an empty set of clauses
			 * 
			 * @param visibility
			 * @param name
			 * @param returnType
			 * @param args
			 */
			public Builder(boolean visibility, 
					Located<String> name, Extent returnType, @Nullable Extent args) {
				this.visibility = visibility;
				this.name = name;
				this.shortest = false;
				this.args = args;
				this.returnType = returnType;
				this.clauses = new ArrayList<>();
			}
			
			/**
			 * Use the shortest-match rule for this entry
			 * @return the receiver
			 */
			public Builder setShortest() {
				shortest = true;
				return this;
			}
			
			/**
			 * Adds the clause formed by the given regular expression
			 * and semantic action
			 * @param regular
			 * @param loc
			 * @return the receiver
			 */
			public Builder add(Located<Regular> regular, Extent loc) {
				clauses.add(new Clause(regular, loc));
				return this;
			}
			
			/**
			 * Adds the clause formed by the given regular expression
			 * and inlined semantic action
			 * @param regular
			 * @param inlined
			 * @return the receiver
			 */
			public Builder add(Regular regular, String inlined) {
				return add(Located.dummy(regular), Extent.inlined(inlined));
			}
			
			/**
			 * @return a new lexer entry based on this builder
			 */
			public Entry build() {
				if (clauses.isEmpty())
					throw new IllegalArgumentException(
						"There must be at least one clause in every lexer entry");
				return new Entry(visibility, name, returnType, shortest,
						args, new ArrayList<>(clauses));
			}
		}
	}

	/** The configuration options specified in this grammar */
	public final List<@NonNull Option> options;
	/** The Java imports to be added to the generated lexer */
	public final List<@NonNull Located<String>> imports;
	/** The extent of this lexer's class header */
	public final Extent header;
	/** 
	 * The named auxiliary regular expressions defined in this lexer
	 * NB: These expressions are already inlined in {@code entryPoints}
	 * 	during the parsing so they are only stored in the lexer
	 *  description for UI feedback purposes. 
	 */
	public final Map<Located<String>, Regular> regulars;
	/** The list of entrypoints */
	public final List<@NonNull Entry> entryPoints;
	/** The extent of this lexer's footer */
	public final Extent footer;
	
	/**
	 * @param options
	 * @param imports
	 * @param header
	 * @param regulars
	 * @param entryPoints
	 * @param footer
	 * 
	 * Builds a lexer with the provided data
	 */
	 private Lexer(List<Option> options, List<Located<String>> imports, 
			Extent header, Map<Located<String>, Regular> regulars, 
			List<Entry> entryPoints, Extent footer) {
		this.options = options;
		this.imports = imports;
		this.header = header;
		this.regulars = regulars;
		this.entryPoints = entryPoints;
		this.footer = footer;
		if (this.entryPoints.isEmpty())
			throw new IllegalArgumentException(
				"There must be at least one entry in a lexer description");
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		imports.forEach(imp -> System.out.println(imp.val));
		buf.append("{\n").append(header.find()).append("\n}");
		boolean first = true;
		for (Entry entry : entryPoints) {
			buf.append("\n");
			entry.append(buf, first ? "rule" : "and");
			first = false;
		}
		buf.append("\n{\n").append(footer.find()).append("\n}");
		return buf.toString();
	}

	/**
	 * Exception raised by the lexer {@linkplain Lexer.Builder builder} class
	 * when trying to construct an ill-formed lexer description.
	 * <p>
	 * The exception contains the {@linkplain #reports problems reported} during
	 * the lexer construction.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static class IllFormedException extends RuntimeException {
		private static final long serialVersionUID = -5811064298772984965L;
		
		/**
		 * The problems reported during the lexer construction,
		 * and which led to this exception
		 */
		public final List<@NonNull IReport> reports;
		
		/**
		 * @param message
		 * @param reports
		 */
		public IllFormedException(String message, List<@NonNull IReport> reports) {
			super(message);
			this.reports = reports;
		}
	}

	/**
	 * A builder class for {@link Lexer}, where entries can be
	 * added incrementally, and which takes care of collecting
	 * problem reports along the way
	 * 
	 * @author Stéphane Lescuyer
	 * @see #addEntry(Entry)
	 */
	public static final class Builder {
		private final List<Option> options;
		private final List<Located<String>> imports;
		private final Extent header;
		private final Map<Located<String>, Regular> regulars;
		private final List<Entry> entryPoints;
		private final Extent footer;
		
		/** Problems reported when building this lexer */
		public final Reporter reporter;
		
		/**
		 * Returns a new builder with the given options, imports, 
		 * header, auxiliary regular expressions and footer
		 * @param options
		 * @param imports
		 * @param header
		 * @param regulars
		 * @param footer
		 */
		public Builder(List<Option> options, List<Located<String>> imports, 
				Extent header, Map<Located<String>, Regular> regulars, Extent footer) {
			this.options = options;
			this.imports = imports;
			this.header = header;
			this.regulars = regulars;
			this.entryPoints = new ArrayList<>();
			this.footer = footer;
			this.reporter = new Reporter();
		}
		
		/**
		 * @param entry
		 * @return the new state of this builder, with the
		 *  given entry rule added to the lexer
		 */
		public Builder addEntry(Entry entry) {
			String key = entry.name.val;
			for (Entry entryPoint : entryPoints) {
				if (key.equals(entryPoint.name.val)) {
					reporter.add(Reports.duplicateEntryDeclaration(entry.name));
					return this;
				}
			}
			this.entryPoints.add(entry);
			return this;
		}
		
		/**
		 * @param entries
		 * @return the new state of this builder, with all entries
		 * 	added to the lexer
		 */
		public Builder addEntries(@NonNull Entry... entries) {
			for (Entry entry : entries)
				addEntry(entry);
			return this;
		}
		
		/**
		 * @return a lexer description from this builder
		 * @throws IllFormedException if the described lexer
		 * 	is not well-formed
		 */
		public Lexer build() {
			if (reporter.hasErrors())
				throw new IllFormedException(
					"Errors were found when trying to build this lexer (aborting):\n" + reporter,
					reporter.getReports());
				
			return new Lexer(options, imports, header, regulars, entryPoints, footer);
		}
		
	}

	/**
	 * @param options
	 * @param imports
	 * @param header
	 * @param regulars
	 * @param entryPoints
	 * @param footer
	 * @return a lexer description built out of the given data, using
	 * 	the {@linkplain Lexer.Builder builder} class to detect potential problems
	 * @throws IllFormedException if the description is ill-formed
	 */
	public static Lexer of(
			List<Option> options, List<Located<String>> imports, 
			Extent header, Map<Located<String>, Regular> regulars,
			Iterable<Entry> entryPoints, Extent footer) {
		 Lexer.Builder builder = new Lexer.Builder(options, imports, header, regulars, footer);
		 for (Entry entry : entryPoints)
			 builder.addEntry(entry);
		 return builder.build();
	}
	
	/**
	 * Static utility class to build the various problem reports
	 * that can arise in building an instance of {@link Lexer}
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class Reports {
		
		static IReport duplicateEntryDeclaration(Located<String> entry) {
			String msg = String.format("Entry rule \"%s\" is already declared", entry.val);
			return IReport.of(msg, Severity.ERROR, entry);
		}

	}
}