package syntax;

import java.util.ArrayList;
import java.util.HashMap;
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
 * A parametric parser description is a set of possibly parametric
 * {@linkplain PGrammarRule grammar rules} along with arbitrary 
 * header and footer sections. The rules come in no particular order, 
 * as every rule can theoretically be used as an entry point to the 
 * generated parser. Public rules cannot be parametric.
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
public final class PGrammar {

	/** The configuration options specified in this grammar */
	public final List<@NonNull Option> options;
	/** The Java imports to be added to the generated parser */
	public final List<@NonNull Located<String>> imports;
	/** The declarations for all terminals of this grammar */
	public final List<@NonNull TokenDecl> tokenDecls;
	/** The extent of this parser class' header */
	public final Extent header;
	/** The map of all grammar rules in the parser, indexed by their name */
	public final Map<@NonNull String, @NonNull PGrammarRule> rules;
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
	private PGrammar(List<@NonNull Option> options,
			List<@NonNull Located<String>> imports, List<TokenDecl> tokenDecls, 
			Extent header, Map<String, PGrammarRule> rules, Extent footer) {
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
	public PGrammarRule rule(String name) {
		@Nullable PGrammarRule res = rules.get(name);
		if (res == null)
			throw new IllegalArgumentException("Rule " + name + " does not exist in this grammar: " + this);
		return res;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		options.forEach(opt -> buf.append(opt).append("\n"));;
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
	 * Exception raised by the grammar {@linkplain PGrammar.Builder builder} class
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
	 * A builder class for {@link PGrammar}, where rules can be
	 * added incrementally, and which takes care of collecting
	 * problem reports along the way
	 * 
	 * @author Stéphane Lescuyer
	 * @see #addRule(PGrammarRule)
	 */
	public static final class Builder {
		private final List<Option> options;
		private final List<Located<String>> imports;
		private final List<TokenDecl> tokenDecls;
		private final Extent header;
		private final Extent footer;
		private final Map<String, PGrammarRule> rules;
		
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
			this.options = options;
			this.imports = imports;
			this.tokenDecls = new ArrayList<>();
			this.header = header;
			this.footer = footer;
			this.rules = new LinkedHashMap<>();
			this.reporter = new Reporter();
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
		public Builder addRule(PGrammarRule rule) {
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
		public PGrammar build() {
			sanityCheck(reporter, tokenDecls, rules);
			if (reporter.hasErrors())
				throw new IllFormedException(
					"Errors were found when trying to build this grammar (aborting):\n" + reporter,
					reporter.getReports());
				
			return new PGrammar(options, imports, tokenDecls, header, rules, footer);
		}
		
		/**
		 * Performs well-formedness checks on the given grammar description,
		 * passing discovered problems to the {@code reporter}
		 * 
		 * @param reporter
		 * @param tokenDecls
		 * @param rules
		 */
		private static void sanityCheck(Reporter reporter,
			List<TokenDecl> tokenDecls, Map<String, PGrammarRule> rules) {
			// Prepare sets of declared tokens and non-terminals
			Set<Located<String>> tokens = new HashSet<>();
			Set<Located<String>> valuedTokens = new HashSet<>();
			for (TokenDecl token : tokenDecls) {
				tokens.add(token.name);
				if (token.isValued())
					valuedTokens.add(token.name);
			}
			Map<Located<String>, Integer> nonterms = new HashMap<>();
			Set<Located<String>> argnterms = new HashSet<>();
			Set<Located<String>> voidnterms = new HashSet<>();
			for (PGrammarRule rule : rules.values()) {
				if (rule.args != null)
					argnterms.add(rule.name);
				if (rule.returnType.find().trim().equals("void"))
					voidnterms.add(rule.name);
				nonterms.put(rule.name, rule.params.size());
			}
			// Go through every rule and check every item
			// used makes some sense
			for (PGrammarRule rule : rules.values()) {
				int i = 0;
				Set<String> formals = new HashSet<>();
				rule.params.forEach(param -> formals.add(param.val));
				
				checkExtent(reporter, rule, -1, formals, rule.returnType);
				checkExtent(reporter, rule, -1, formals, rule.args);
				for (PProduction prod : rule.productions) {
					++i;
					for (PProduction.Item item : prod.items) {
						switch (item.getKind()) {
						case ACTION:
							PProduction.ActionItem action = (PProduction.ActionItem) item;
							checkExtent(reporter, rule, i, formals, action.extent);
							break;
						case ACTUAL:
							PProduction.Actual actual = (PProduction.Actual) item;
							checkExtent(reporter, rule, i, formals, actual.args);
							checkActual(reporter, rule, i, formals,
									tokens, valuedTokens, nonterms, argnterms, voidnterms,
									actual, actual.item, true);
							break;
						case CONTINUE:
							break;
						}
					}
				}
			}
		}
		
		/**
		 * Checks that all holes in {@code extent} refer to formal
		 * parameters which are declared in {@code formals}, and
		 * reports problems to {@code reporter}. If {@code extent}
		 * is null, nothing is checked.
		 * 
		 * @param reporter
		 * @param rule
		 * @param j
		 * @param formals
		 * @param extent
		 */
		private static void checkExtent(Reporter reporter,
				PGrammarRule rule, int j, Set<String> formals, @Nullable PExtent extent) {
			if (extent == null) return;
			for (PExtent.Hole hole : extent.holes) {
				if (!formals.contains(hole.name))
					reporter.add(Reports.undeclaredFormal(rule, j, extent, hole));
			}
		}
		
		/**
		 * @WIP
		 * TODO write a specific object to hold the context and perform the checks
		 * 
		 * @param reporter
		 * @param rule
		 * @param i
		 * @param formals
		 * @param tokens
		 * @param valuedTokens
		 * @param nonterms
		 * @param argnterms
		 * @param voidnterms
		 * @param actual
		 * @param aexpr
		 * @param root
		 */
		private static void checkActual(Reporter reporter,
				PGrammarRule rule, int i, Set<String> formals,
				Set<Located<String>> tokens, Set<Located<String>> valuedTokens,
				Map<Located<String>, Integer> nonterms, Set<Located<String>> argnterms, 
				Set<Located<String>> voidnterms,
				PProduction.Actual actual, PProduction.ActualExpr aexpr, boolean root) {
			final Located<String> name = aexpr.symb;
			if (aexpr.isTerminal()) {
				if (!tokens.contains(name))
					reporter.add(Reports.undeclaredToken(rule, i, name));
				// Only do the value check if the token is declared
				else if (root && actual.isBound() && 
					!valuedTokens.contains(name))
					reporter.add(Reports.unvaluedTokenBound(
						rule, i, Nulls.ok(actual.binding), name));
			}
			else if (formals.contains(name.val)) {
				// A formal is not supposed to have parameters (no higher-order)
				if (!aexpr.params.isEmpty())
					reporter.add(Reports.parameterizedFormal(
						rule, i, name));
			}
			else {
				// Must be a non-terminal
				@Nullable Integer arity = nonterms.get(name);
				if (arity == null)
					reporter.add(Reports.undeclaredNonTerminal(rule, i, name));
				// Only do the value and args checks if the non-term is declared
				else {
					// The arities map was filled for all non-terminals
					if (aexpr.params.size() != arity)
						reporter.add(Reports.badNumberOfParameters(
							rule, i, name, arity));
					if (root && actual.args != null &&
							!argnterms.contains(name))
						reporter.add(Reports.unexpectedArguments(
								rule, i, Nulls.ok(actual.args), name));
					if (root && actual.args == null &&
							argnterms.contains(name))
						reporter.add(Reports.missingArguments(
								rule, i, name));
					// Not complete of course but better than nothing
					if (root && actual.isBound() &&
							voidnterms.contains(name))
						reporter.add(Reports.voidNonTerminalBound(
								rule, i, Nulls.ok(actual.binding), name));
				}
			}
			
			// Now check the subterms if any
			aexpr.params.forEach(sub -> 
				checkActual(reporter, rule, i, formals,
					tokens, valuedTokens, nonterms, argnterms, voidnterms,
					actual, sub, false));
		}
	}
	
	/**
	 * Static utility class to build the various problem reports
	 * that can arise in building or analyzing an instance 
	 * of {@link PGrammar}
	 * 
	 * @author Stéphane Lescuyer
	 */
	static abstract class Reports {

		static IReport duplicateTokenDeclaration(Located<String> token) {
			String msg = String.format("Token \"%s\" is already declared", token.val);
			return IReport.of(msg, Severity.ERROR, token);
		}
		
		static IReport duplicateRuleDeclaration(Located<String> rule) {
			String msg = String.format("Rule \"%s\" is already declared", rule.val);
			return IReport.of(msg, Severity.ERROR, rule);
		}
		
		static IReport unusedTerminal(Located<String> token) {
			String msg = String.format("Token \"%s\" is never used", token.val);
			return IReport.of(msg, Severity.WARNING, token);
		}

		static IReport unusedPrivateRule(Located<String> rule) {
			String msg = String.format("Rule \"%s\" is never used", rule.val);
			return IReport.of(msg, Severity.WARNING, rule);
		}
		
		private static String inRule(PGrammarRule rule, int j) {
			if (j >= 0)
				return String.format("In rule \"%s\", production %d: ", rule.name.val, j);
			else
				return String.format("In rule \"%s\": ", rule.name.val);
		}
		
		static IReport undeclaredFormal(PGrammarRule rule, int j, PExtent extent, PExtent.Hole hole) {
			String msg = String.format("%s undeclared formal parameter \"%s\"",
					inRule(rule, j), hole.name);
			return IReport.of(msg, Severity.ERROR, extent);	// TODO report on hole location
		}
		
		static IReport undeclaredToken(PGrammarRule rule, int j, Located<String> token) {
			String msg = String.format("%s undeclared token \"%s\"",
				inRule(rule, j), token.val);
			// TODO: add proposals based on hamming/levenshtein?
			return IReport.of(msg, Severity.ERROR, token);
		}
		
		static IReport unvaluedTokenBound(PGrammarRule rule, int j,
			Located<String> binding, Located<String> token) {
			String msg = String.format("%s bound token \"%s\" has no declared value",
				inRule(rule, j), token.val);
			return IReport.of(msg, Severity.ERROR, binding);
		}

		static IReport undeclaredNonTerminal(PGrammarRule rule, int j, Located<String> nterm) {
			String msg = String.format("%s undeclared non-terminal \"%s\"",
				inRule(rule, j), nterm.val);
			// TODO: add proposals based on hamming/levenshtein?
			return IReport.of(msg, Severity.ERROR, nterm);
		}

		static IReport unexpectedArguments(PGrammarRule rule, int j,
			Extent args, Located<String> nterm) {
			String msg = String.format("%s non-terminal \"%s\" does not expect arguments",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, args);
		}

		static IReport missingArguments(PGrammarRule rule, int j, Located<String> nterm) {
			String msg = String.format("%s non-terminal \"%s\" expects arguments",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, nterm);
		}

		static IReport voidNonTerminalBound(PGrammarRule rule, int j,
			Located<String> binding, Located<String> nterm) {
			String msg = String.format("%s bound non-terminal \"%s\" returns void",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, binding);
		}
		
		static IReport parameterizedFormal(PGrammarRule rule, int j, Located<String> formal) {
			String msg = String.format("%s formal \"%s\" does not expect parameters",
				inRule(rule, j), formal.val);
			return IReport.of(msg, Severity.ERROR, formal);
		}
		
		static IReport badNumberOfParameters(PGrammarRule rule, int j, 
			Located<String> formal, int expected) {
			String msg = String.format("%s non-terminal \"%s\" expects %d parameters",
				inRule(rule, j), formal.val, expected);
			return IReport.of(msg, Severity.ERROR, formal);
		}
				
	}
}