package syntax;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

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
	 * A lexer rule entry is a seequence of regular expressions
	 * associated to semantic actions. A rule has a
	 * {@link #name name} and may have some some 
	 * {@link #args arguments}. The regular expressions
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
		/** The name of this entry */
		public final String name;
		/** Whether shortest match should be used */
		public final boolean shortest;
		/** The list of formal arguments for this rule */
		public final List<String> args;
		/** 
		 * The various clauses for this entry, associated to
		 * the locations for the corresponding semantic actions.
		 * <b>Iteration order in {@link #clauses} is relevant
		 * as priority between same-length matches goes to the
		 * first matching rule in this map.</b>
		 */
		public final Map<Regular, Location> clauses;
		
		/**
		 * @param name
		 * @param shortest
		 * @param args
		 * @param clauses
		 * 
		 * Builds a new lexer entry with the provided arguments.
		 * Beware that order in {@code clauses} is relevant.
		 */
		public Entry(String name, boolean shortest,
				List<String> args, Map<Regular, Location> clauses) {
			this.name = name;
			this.shortest = shortest;
			this.args = args;
			this.clauses = clauses;
		}
		
		StringBuilder append(StringBuilder buf, String kword) {
			buf.append(kword).append(" ").append(name);
			args.forEach(arg -> buf.append(" " + arg));
			buf.append(" = ").append(shortest ? "shortest" : "parse");
			clauses.forEach((reg, act) -> {
				buf.append("\n| ").append(reg);
				buf.append(" {").append(act.find()).append(" }");
			});
			return buf;
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			append(buf, "rule");
			@SuppressWarnings("null")
			@NonNull String res = buf.toString();
			return res;
		}
	}

	/** The location of this lexer's header */
	public final Location header;
	/** The list of entrypoints */
	public final List<Entry> entryPoints;
	/** The location of this lexer's footer */
	public final Location footer;
	
	/**
	 * @param header
	 * @param entryPoints
	 * @param footer
	 * 
	 * Builds a lexer with the provided data
	 */
	public Lexer(Location header, List<Entry> entryPoints, Location footer) {
		this.header = header;
		this.entryPoints = entryPoints;
		this.footer = footer;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("{\n").append(header.find()).append("}\n");
		boolean first = true;
		for (Entry entry : entryPoints) {
			buf.append("\n");
			entry.append(buf, first ? "rule" : "and");
			first = false;
		}
		buf.append("{\n").append(footer.find()).append("}\n");
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}

}