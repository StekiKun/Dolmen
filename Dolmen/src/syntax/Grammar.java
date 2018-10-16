package syntax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Nulls;
import syntax.IReport.Severity;

/**
 * A lexer description is a set of {@linkplain GrammarRule grammar rules}
 * along with arbitrary header and footer sections. The rules come
 * in no particular order, as every rule can theoretically be
 * used as an entry point to the generated parser. When only one
 * entry point really makes sense, it is conventionally the first
 * rule.
 * <p>
 * The rules use a set of {@linkplain #tokenDecls terminals} which
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
	 * {@linkplain #name name}, which is the name
	 * used in grammar rules to denote that terminal,
	 * and the potential {@linkplain #valueType value type}
	 * associated to these tokens.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TokenDecl {
		/** The name of this token */
		public final Located<String> name;
		/**
		 * If non-null, the extent of the type of Java values
		 * associated to this token at run-time
		 */
		public final @Nullable Extent valueType;
		
		/**
		 * Builds the token declaration with the given
		 * name and value type
		 * @param name
		 * @param valueType
		 */
		public TokenDecl(Located<String> name, @Nullable Extent valueType) {
			if (name.val.chars().anyMatch(ch -> Character.isLowerCase(ch)))
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
			@Nullable Extent valueType_ = valueType;
			return "token " +
					(valueType_ == null ? "" : "{" + valueType_.find() + "} ") +
					name.val;
		}
	}
	
	/** The configuration options specified in this grammar, indexed by the option key */
	public final Map<@NonNull String, @NonNull Option> options;
	/** The Java imports to be added to the generated parser */
	public final List<@NonNull Located<String>> imports;
	/** The declarations for all terminals of this grammar */
	public final List<@NonNull TokenDecl> tokenDecls;
	/** The extent of this parser class' header */
	public final Extent header;
	/** The map of all grammar rules in the parser, indexed by their name */
	public final Map<@NonNull String, @NonNull GrammarRule> rules;
	/** The extent of this parser class' footer */
	public final Extent footer;
	
	/**
	 * Builds a grammar description from the given parameters
	 * 
	 * @param options
	 * @param imports
	 * @param tokenDecls
	 * @param header
	 * @param rules
	 * @param footer
	 */
	private Grammar(Map<@NonNull String, @NonNull Option> options,
			List<@NonNull Located<String>> imports, List<TokenDecl> tokenDecls, 
			Extent header, Map<String, GrammarRule> rules, Extent footer) {
		this.options = options;
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
		options.values().forEach(opt -> buf.append(opt).append("\n"));;
		imports.forEach(imp -> buf.append(imp.val).append("\n"));
		tokenDecls.forEach(token ->
			buf.append("\n").append(token));
		buf.append("\n\n{").append(header.find()).append("}");
		rules.forEach((entry, rule) -> 
			{ buf.append("\n\n"); rule.append(buf); });
		buf.append("\n\n{").append(footer.find()).append("}");
		return buf.toString();
	}
	
	/**
	 * Exception raised by the grammar {@linkplain Grammar.Builder builder} class
	 * when trying to construct an ill-formed grammar description.
	 * <p>
	 * The exception contains the {@linkplain #reports problems reported} during
	 * the grammar construction.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static class IllFormedException extends RuntimeException {
		private static final long serialVersionUID = -5811064298772984965L;
		
		/**
		 * The problems reported during the grammar construction,
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
	 * A builder class for {@link Grammar}, where rules can be
	 * added incrementally, and which takes care of collecting
	 * problem reports along the way
	 * 
	 * @author Stéphane Lescuyer
	 * @see #addRule(GrammarRule)
	 */
	public static final class Builder {
		private final Map<String, Option> options;
		private final List<Located<String>> imports;
		private final List<TokenDecl> tokenDecls;
		private final Extent header;
		private final Extent footer;
		private final Map<String, GrammarRule> rules;
		
		/** Problems reported when building this grammar */
		public final Reporter reporter;
		
		/**
		 * Returns a new builder with the given options, imports, header and footer
		 * @param options
		 * @param imports
		 * @param header
		 * @param footer
		 */
		public Builder(List<Option> options,
				List<Located<String>> imports, Extent header, Extent footer) {
			this.imports = imports;
			this.tokenDecls = new ArrayList<>();
			this.header = header;
			this.footer = footer;
			this.rules = new LinkedHashMap<>();
			this.reporter = new Reporter();
			
			// Index the option list by key
			Map<String, Option> indexedOptions = new LinkedHashMap<>();
			for (Option option : options) {
				if (indexedOptions.containsKey(option.key.val))
					reporter.add(Reports.duplicateOption(option));
				indexedOptions.put(option.key.val, option);
			}
			this.options = indexedOptions;
		}
		
		/**
		 * @param decl
		 * @return the new state of this builder, with the
		 *  given token declaration added to the grammar
		 */
		public Builder addToken(TokenDecl decl) {
			String key = decl.name.val;
			for (TokenDecl tdecl : tokenDecls) {
				if (key.equals(tdecl.name.val)) {
					reporter.add(Reports.duplicateTokenDeclaration(decl.name));
					return this;
				}
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
			String key = rule.name.val;
			if (rules.containsKey(key)) {
				reporter.add(Reports.duplicateRuleDeclaration(rule.name));
				return this;
			}
			this.rules.put(key, rule);
			return this;
		}
		
		/**
		 * @return a grammar description from this builder
		 * @throws IllFormedException if the described grammar
		 * 	is not well-formed
		 */
		public Grammar build() {
			sanityCheck(tokenDecls, rules);
			if (reporter.hasErrors())
				throw new IllFormedException(
					"Errors were found when trying to build this grammar (aborting):\n" + reporter,
					reporter.getReports());
				
			return new Grammar(options, imports, tokenDecls, header, rules, footer);
		}
		
		/**
		 * Performs well-formedness checks on the given grammar description,
		 * passing discovered problems to the {@link reporter}
		 * 
		 * @param tokenDecls
		 * @param rules
		 */
		private void sanityCheck(
			List<TokenDecl> tokenDecls, Map<String, GrammarRule> rules) {
			// Prepare sets of declared tokens and non-terminals
			Set<Located<String>> tokens = new HashSet<>();
			Set<Located<String>> valuedTokens = new HashSet<>();
			for (TokenDecl token : tokenDecls) {
				tokens.add(token.name);
				if (token.isValued())
					valuedTokens.add(token.name);
			}
			Set<Located<String>> nonterms =
				rules.values().stream().collect(
					() -> new HashSet<Located<String>>(), 
					(acc, rule) -> acc.add(rule.name),
					(r1, r2) -> { });
			Set<Located<String>> argnterms = new HashSet<>();
			Set<Located<String>> voidnterms = new HashSet<>();
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
						final Located<String> name = actual.item;
						if (actual.isTerminal()) {
							if (!tokens.contains(name))
								reporter.add(Reports.undeclaredToken(rule, j, name));
							// Only due the value check if the token is declared
							else if (actual.isBound() && 
								!valuedTokens.contains(name))
								reporter.add(Reports.unvaluedTokenBound(
									rule, j, Nulls.ok(actual.binding), name));
						} else {
							if (!nonterms.contains(name))
								reporter.add(Reports.undeclaredNonTerminal(rule, j, name));
							// Only due the value and args checks if the non-term is declared
							else {
								if (actual.args != null &&
										!argnterms.contains(name))
									reporter.add(Reports.unexpectedArguments(
											rule, j, Nulls.ok(actual.args), name));
								if (actual.args == null &&
										argnterms.contains(name))
									reporter.add(Reports.missingArguments(
											rule, j, name));
								// Not complete of course but better than nothing
								if (actual.isBound() &&
										voidnterms.contains(name))
									reporter.add(Reports.voidNonTerminalBound(
											rule, j, Nulls.ok(actual.binding), name));
							}
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * Static utility class to build the various problem reports
	 * that can arise in building an instance of {@link Grammar}
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class Reports {

		static IReport duplicateOption(Option option) {
			String msg = String.format("Option \"%s\" is already set, this setting will be ignored");
			return IReport.of(msg, Severity.WARNING, option.key);
		}
		
		static IReport duplicateTokenDeclaration(Located<String> token) {
			String msg = String.format("Token \"%s\" is already declared", token.val);
			return IReport.of(msg, Severity.ERROR, token);
		}
		
		static IReport duplicateRuleDeclaration(Located<String> rule) {
			String msg = String.format("Rule \"%s\" is already declared", rule.val);
			return IReport.of(msg, Severity.ERROR, rule);
		}
		
		private static String inRule(GrammarRule rule, int j) {
			return String.format("In rule \"%s\", production %d: ", rule.name.val, j);
		}
		
		static IReport undeclaredToken(GrammarRule rule, int j, Located<String> token) {
			String msg = String.format("%s undeclared token \"%s\"",
				inRule(rule, j), token.val);
			// TODO: add proposals based on hamming/levenshtein?
			return IReport.of(msg, Severity.ERROR, token);
		}
		
		static IReport unvaluedTokenBound(GrammarRule rule, int j,
			Located<String> binding, Located<String> token) {
			String msg = String.format("%s bound token \"%s\" has no declared value",
				inRule(rule, j), token.val);
			return IReport.of(msg, Severity.ERROR, binding);
		}

		static IReport undeclaredNonTerminal(GrammarRule rule, int j, Located<String> nterm) {
			String msg = String.format("%s undeclared non-terminal \"%s\"",
				inRule(rule, j), nterm.val);
			// TODO: add proposals based on hamming/levenshtein?
			return IReport.of(msg, Severity.ERROR, nterm);
		}

		static IReport unexpectedArguments(GrammarRule rule, int j,
			Extent args, Located<String> nterm) {
			String msg = String.format("%s non-terminal \"%s\" does not expect arguments",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, args);
		}

		static IReport missingArguments(GrammarRule rule, int j, Located<String> nterm) {
			String msg = String.format("%s non-terminal \"%s\" expects arguments",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, nterm);
		}

		static IReport voidNonTerminalBound(GrammarRule rule, int j,
			Located<String> binding, Located<String> nterm) {
			String msg = String.format("%s bound non-terminal \"%s\" returns void",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, binding);
		}

	}
}