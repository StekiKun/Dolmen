package syntax;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A lexer description is a set of {@link GrammarRule grammar rules}
 * along with arbitrary header and footer sections. The rules come
 * in no particular order, as every rule can theoretically be
 * used as an entry point to the generated parser. When only one
 * entry point really makes sense, it is conventionnally the first
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
	public Grammar(List<String> imports, List<TokenDecl> tokenDecls, 
			Location header, Map<String, GrammarRule> rules, Location footer) {
		this.imports = imports;
		this.tokenDecls = tokenDecls;
		this.header = header;
		this.rules = rules;
		this.footer = footer;
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
		 */
		public Grammar build() {
			return new Grammar(imports, tokenDecls, header, rules, footer);
		}
	}
}
