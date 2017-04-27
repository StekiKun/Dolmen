package syntax;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A lexer description is a set of {@link GrammarRule grammar rules}
 * along with arbitrary header and footer sections. The rules come
 * in no particular order, as every rule can theoretically be
 * used as an entry point to the generated parser. When only one
 * entry point really makes sense, it is conventionnally the first
 * rule.
 * 
 * @author Stéphane Lescuyer
 */
public final class Grammar {

	/** The Java imports to be added to the generated parser */
	public final List<@NonNull String> imports;
	// TODO: add token declarations
	/** The location of this parser class' header */
	public final Location header;
	/** The map of all grammar rules in the parser, indexed by their name */
	public final Map<@NonNull String, @NonNull GrammarRule> rules;
	/** The location of this parser class' footer */
	public final Location footer;
	
	/**
	 * Builds a grammar description from the given parameters
	 * 
	 * @param imports
	 * @param header
	 * @param rules
	 * @param footer
	 */
	public Grammar(List<String> imports, Location header,
			Map<String, GrammarRule> rules, Location footer) {
		this.imports = imports;
		this.header = header;
		this.rules = rules;
		this.footer = footer;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		imports.forEach(imp -> buf.append(imp).append("\n"));
		buf.append("{").append(header.find()).append("}");
		rules.forEach((entry, rule) -> 
			{ buf.append("\n"); rule.append(buf); });
		buf.append("{").append(footer.find()).append("}");
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}
	
	/**
	 * A builder class for {@link Grammar}, where rules can be
	 * added incrementally
	 * 
	 * @author Stéphane Lescuyer
	 * @see #addRule(GrammarRule)
	 */
	public static final class Builder {
		private final List<String> imports;
		private final Location header;
		private final Location footer;
		private final Map<String, GrammarRule> rules;
		
		/**
		 * Returns a new builder with the given imports, header and footer
		 * @param imports
		 * @param header
		 * @param footer
		 */
		public Builder(List<String> imports, Location header, Location footer) {
			this.imports = imports;
			this.header = header;
			this.footer = footer;
			this.rules = new HashMap<>();
		}
		
		/**
		 * @param rule
		 * @return the new state of this builder, with the
		 * 	given {@code rule} added to the set of rules
		 */
		public Builder addRule(GrammarRule rule) {
			String key = rule.name;
			if (rules.containsKey(key))
				throw new IllegalArgumentException("Cannot have two rules with the same name");
			this.rules.put(key, rule);
			return this;
		}
		
		/**
		 * @return a grammar description from this builder
		 */
		public Grammar build() {
			return new Grammar(imports, header, rules, footer);
		}
	}
}
