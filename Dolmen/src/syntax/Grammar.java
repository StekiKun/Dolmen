package syntax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A lexer description is a set of {@link GrammarRule grammar rules}
 * along with arbitrary header and footer sections. The rules come
 * in no particular order, as every rule can theoretically be
 * used as an entry point to the generated parser. When only one
 * entry point really makes sense, it is conventionally the first
 * rule.
 * <p>
 * The rules use a set of {@link #tokenDecls terminals} which
 * are provided before the header section in the source file.
 * Each terminal can optionally be associated to a value of
 * some Java type at run-time.
 * <p>
 * Rule names are identifiers which start with a lower-case letter.
 * Terminal names are identifiers all in upper case.
 * 
 * @author Stéphane Lescuyer
 */
public final class Grammar {

	/**
	 * A token declaration describes the token
	 * {@link #name name}, which is the name
	 * used in grammar rules to denote that terminal,
	 * and the potential {@link #valueType value type}
	 * associated to these tokens.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TokenDecl {
		/** The name of this token */
		public final String name;
		/**
		 * If non-null, the location of the type of Java values
		 * associated to this token at run-time
		 */
		public final @Nullable Location valueType;
		
		/**
		 * Builds the token declaration with the given
		 * name and value type
		 * @param name
		 * @param valueType
		 */
		public TokenDecl(String name, @Nullable Location valueType) {
			if (name.chars().anyMatch(ch -> Character.isLowerCase(ch)))
				throw new IllegalArgumentException("Token name should not contain lower case");

			this.name = name;
			this.valueType = valueType;
		}
		
		/**
		 * @return {@code true} iff tokens of this type bear
		 * 	a semantic value at run-time
		 */
		public boolean isValued() {
			return valueType != null;
		}
		
		@Override
		public String toString() {
			@Nullable Location valueType_ = valueType;
			return "token " +
					(valueType_ == null ? "" : "{" + valueType_.find() + "} ") +
					name;
		}
	}
	
	/** The Java imports to be added to the generated parser */
	public final List<@NonNull String> imports;
	/** The declarations for all terminals of this grammar */
	public final List<@NonNull TokenDecl> tokenDecls;
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
	 * @param tokenDecls
	 * @param header
	 * @param rules
	 * @param footer
	 */
	private Grammar(List<String> imports, List<TokenDecl> tokenDecls, 
			Location header, Map<String, GrammarRule> rules, Location footer) {
		this.imports = imports;
		this.tokenDecls = tokenDecls;
		this.header = header;
		this.rules = rules;
		this.footer = footer;
	}
	
	/**
	 * @param name
	 * @return the rule with the given {@code name} in this grammar
	 * @throws IllegalArgumentException if no such rule exists
	 */
	public GrammarRule rule(String name) {
		@Nullable GrammarRule res = rules.get(name);
		if (res == null)
			throw new IllegalArgumentException("Rule " + name + " does not exist in this grammar: " + this);
		return res;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		imports.forEach(imp -> buf.append(imp).append("\n"));
		tokenDecls.forEach(token ->
			buf.append("\n").append(token));
		buf.append("\n{").append(header.find()).append("}");
		rules.forEach((entry, rule) -> 
			{ buf.append("\n"); rule.append(buf); });
		buf.append("\n{").append(footer.find()).append("}");
		return buf.toString();
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
		private final List<TokenDecl> tokenDecls;
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
			this.tokenDecls = new ArrayList<>();
			this.header = header;
			this.footer = footer;
			this.rules = new LinkedHashMap<>();
		}
		
		/**
		 * @param decl
		 * @return the new state of this builder, with the
		 *  given token declaration added to the grammar
		 */
		public Builder addToken(TokenDecl decl) {
			String key = decl.name;
			for (TokenDecl tdecl : tokenDecls) {
				if (key.equals(tdecl.name))
					throw new IllegalArgumentException("Cannot have two tokens with the same name");
			}
			this.tokenDecls.add(decl);
			return this;
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
		 * @throws IllegalArgumentException if the described grammar
		 * 	is not well-formed
		 */
		public Grammar build() {
			@Nullable String report = sanityCheck(tokenDecls, rules);
			if (report != null)
				throw new IllegalArgumentException(
					"Ill-formed grammar description: " + report);
				
			return new Grammar(imports, tokenDecls, header, rules, footer);
		}
		
		/**
		 * Performs well-formedness checks on the given grammar description
		 * 
		 * @param tokenDecls
		 * @param rules
		 * @return {@code null} if all checks passed, or a description
		 * 	of a well-formedness issue otherwise
		 */
		private @Nullable String sanityCheck(
			List<TokenDecl> tokenDecls, Map<String, GrammarRule> rules) {
			// Prepare sets of declared tokens and non-terminals
			Set<String> tokens = new HashSet<String>();
			Set<String> valuedTokens = new HashSet<String>();
			for (TokenDecl token : tokenDecls) {
				tokens.add(token.name);
				if (token.isValued())
					valuedTokens.add(token.name);
			}
			Set<String> nonterms = rules.keySet();
			Set<String> argnterms = new HashSet<String>();
			Set<String> voidnterms = new HashSet<String>();
			for (GrammarRule rule : rules.values()) {
				if (rule.args != null)
					argnterms.add(rule.name);
				if (rule.returnType.find().trim().equals("void"))
					voidnterms.add(rule.name);
			}
			// Go through every rule and check every item
			// used makes some sense
			for (GrammarRule rule : rules.values()) {
				int i = 0;
				for (Production prod : rule.productions) {
					++i;
					for (Production.Actual actual : prod.actuals()) {
						final int j = i;
						Function<String, String> report =
							msg -> scMessage(rule, j, actual, msg);
						final String name = actual.item;
						if (actual.isTerminal()) {
							if (!tokens.contains(name))
								return report.apply(name + " is not a defined token");
							if (actual.isBound() &&
								!valuedTokens.contains(name))
								return report.apply(name + " is not bound to any value");
						} else {
							if (!nonterms.contains(name))
								return report.apply(name + " is not a defined non-terminal");
							if (actual.args != null &&
								!argnterms.contains(name))
								return report.apply(name + " does not expect parameters");
							// Not complete of course but better than nothing
							if (actual.isBound() &&
								voidnterms.contains(name))
								return report.apply(name + " is bound to a void-rule");
						}
					}
				}
			}
			return null;
		}
		
		private static String scMessage(
			GrammarRule rule, int i, Production.Actual actual, String msg) {
			return String.format("In rule %s, production %d, item %s: %s",
				rule.name, i, actual, msg);
		}
	}
}
