package syntax;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A lexer definition is a set of {@link Entry lexer rules}
 * along with arbitrary header and footer sections. The rules
 * come in no particular order, as every rule can theoretically
 * be used as an entry point to the generated lexer. When only
 * one entry point really makes sense, it is conventionnally
 * the first rule.
 * 
 * @author Stéphane Lescuyer
 */
public final class Lexer {

	/**
	 * A lexer rule entry is a sequence of regular expressions
	 * associated to semantic actions. A rule has a
	 * {@link #name name}, a {@link #returnType return type} and may 
	 * have some {@link #args arguments}. The regular expressions
	 * that form the rule are called the {@link #clauses clauses}
	 * are normally interpreted with the <i>longest match 
	 * priority</i>, unless {@link #shortest specified otherwise}.
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
		public final String name;
		/** Whether shortest match should be used */
		public final boolean shortest;
		/** The formal arguments for this rule */
		public final @Nullable Location args;
		/** The return type of semantic actions for this entry */
		public final Location returnType;
		
		/** 
		 * The various clauses for this entry, associated to
		 * the locations for the corresponding semantic actions.
		 * <b>Iteration order in {@link #clauses} is relevant
		 * as priority between same-length matches goes to the
		 * first matching rule in this map.</b>
		 */
		public final Map<@NonNull Regular, @NonNull Location> clauses;
		
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
				String name, Location returnType, boolean shortest,
				@Nullable Location args, Map<Regular, Location> clauses) {
			this.visibility = visibility;
			this.name = name;
			this.returnType = returnType;
			this.shortest = shortest;
			this.args = args;
			this.clauses = clauses;
		}
		
		StringBuilder append(StringBuilder buf, String kword) {
			buf.append(kword).append(" ").append(name);
			Location args_ = args;
			if (args_ == null) buf.append("()");
			else buf.append("(").append(args_.find()).append(")");
			buf.append(" : ").append(returnType.find());
			buf.append(" = ").append(shortest ? "shortest" : "parse");
			clauses.forEach((reg, act) -> {
				buf.append("\n| ").append(reg);
				buf.append(" {").append(act.find()).append("}");
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
		 * A builder class for {@link Lexer.Entry lexer entries},
		 * which lets one add clauses incrementally
		 * 
		 * @author Stéphane Lescuyer
		 */
		public static final class Builder {
			private final boolean visibility;
			private final String name;
			private boolean shortest;
			private final @Nullable Location args;
			private final Location returnType;
			private final Map<@NonNull Regular, @NonNull Location> clauses;

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
					String name, Location returnType, @Nullable Location args) {
				this.visibility = visibility;
				this.name = name;
				this.shortest = false;
				this.args = args;
				this.returnType = returnType;
				this.clauses = new LinkedHashMap<>();
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
			public Builder add(Regular regular, Location loc) {
				clauses.put(regular, loc);
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
				clauses.put(regular, Location.inlined(inlined));
				return this;
			}
			
			/**
			 * @return a new lexer entry based on this builder
			 */
			public Entry build() {
				return new Entry(visibility, name, returnType, shortest,
						args, new LinkedHashMap<>(clauses));
			}
		}
	}

	/** The Java imports to be added to the generated lexer */
	public final List<@NonNull String> imports;
	/** The location of this lexer's class header */
	public final Location header;
	/** The list of entrypoints */
	public final List<@NonNull Entry> entryPoints;
	/** The location of this lexer's footer */
	public final Location footer;
	
	/**
	 * @param imports
	 * @param header
	 * @param entryPoints
	 * @param footer
	 * 
	 * Builds a lexer with the provided data
	 */
	public Lexer(List<String> imports, 
			Location header, List<Entry> entryPoints, Location footer) {
		this.imports = imports;
		this.header = header;
		this.entryPoints = entryPoints;
		this.footer = footer;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		imports.forEach(imp -> System.out.println(imp));
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

}