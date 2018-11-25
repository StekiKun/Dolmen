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
import syntax.PGrammars.Args;

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
			new Checker(reporter, tokenDecls, rules).run();
			if (reporter.hasErrors())
				throw new IllFormedException(
					"Errors were found when trying to build this grammar (aborting):\n" + reporter,
					reporter.getReports());
				
			return new PGrammar(options, imports, tokenDecls, header, rules, footer);
		}		
	}
	
	/**
	 * This class is used to perform the various structural and sanity checks
	 * on a would-be {@linkplain PGrammar parametric grammar} in {@link Builder#build()}.
	 * <p>
	 * Its {@link Checker#run()} method does all the checks on the given grammar
	 * description and reports all problems in a given {@link Reporter}. The
	 * various kinds of possible reports are gathered in {@link Reports}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Checker {
		private final Reporter reporter;
		private final Map<String, PGrammarRule> rules;
		
		private final Set<Located<String>> tokens;
		private final Set<Located<String>> valuedTokens;
		
		private final Map<Located<String>, Integer> nonterms;
		private final Set<Located<String>> argnterms;
		private final Set<Located<String>> voidnterms;
		
		/**
		 * Create a new sanity checker for the given token and rule declarations
		 * and which will report to {@code reporter}. Call {@link #run()} to
		 * perform the checks.
		 * 
		 * @param reporter
		 * @param tokenDecls
		 * @param rules
		 */
		Checker(Reporter reporter,
			List<TokenDecl> tokenDecls, Map<String, PGrammarRule> rules) {
			this.reporter = reporter;
			this.rules = rules;
			
			// Prepare sets of declared tokens, finding out which ones are valued
			this.tokens = new HashSet<>(tokenDecls.size());
			this.valuedTokens = new HashSet<>();
			for (TokenDecl token : tokenDecls) {
				tokens.add(token.name);
				if (token.isValued())
					valuedTokens.add(token.name);
			}
			// Prepare sets of non-terminals, along with their arity
			// and whether they expect args or return void
			this.nonterms = new HashMap<>(rules.size());
			this.argnterms = new HashSet<>();
			this.voidnterms = new HashSet<>();
			for (PGrammarRule rule : rules.values()) {
				if (rule.args != null)
					argnterms.add(rule.name);
				if (rule.returnType.find().trim().equals("void"))
					voidnterms.add(rule.name);
				nonterms.put(rule.name, rule.params.size());
			}
		}
		
		/**
		 * Performs well-formedness checks on the given grammar description,
		 * passing discovered problems to the {@link #reporter}
		 */
		void run() {
			// Go through every rule and check every item
			// used makes some sense
			for (PGrammarRule rule : rules.values()) {
				int i = 0;
				Set<String> formals = new HashSet<>();
				rule.params.forEach(param -> formals.add(param.val));
				
				if (rule.visibility && !formals.isEmpty())
					reporter.add(Reports.parametricPublicRule(rule));
				
				checkExtent(rule, -1, formals, rule.returnType);
				checkExtent(rule, -1, formals, rule.args);
				for (PProduction prod : rule.productions) {
					++i;
					for (PProduction.Item item : prod.items) {
						switch (item.getKind()) {
						case ACTION:
							PProduction.ActionItem action = (PProduction.ActionItem) item;
							checkExtent(rule, i, formals, action.extent);
							break;
						case ACTUAL:
							PProduction.Actual actual = (PProduction.Actual) item;
							checkExtent(rule, i, formals, actual.args);
							checkActual(rule, i, formals, actual, actual.item, true);
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
		 * reports problems to {@link #reporter}. If {@code extent}
		 * is null, nothing is checked.
		 * 
		 * @param rule
		 * @param j
		 * @param formals
		 * @param extent
		 */
		private void checkExtent(
				PGrammarRule rule, int j, Set<String> formals, @Nullable PExtent extent) {
			if (extent == null) return;
			for (PExtent.Hole hole : extent.holes) {
				if (!formals.contains(hole.name))
					reporter.add(Reports.undeclaredFormal(rule, j, extent, hole));
			}
		}
		
		/**
		 * Performs sanity checks on the structure of the actual expression {@code aexpr},
		 * which can only refer to the formal parameters given in {@code formals} and
		 * is part of the overall production item {@code actual}.
		 * <p>
		 * Parameter {@code root} specifies whether {@code aexpr} is the top-level 
		 * expression in {@code actual} or not.
		 * <p>
		 * All problems found are reported in the receiver's {@link #reporter}.
		 * 
		 * @param rule
		 * @param i
		 * @param formals
		 * @param actual
		 * @param aexpr
		 * @param root
		 */
		private void checkActual(
				PGrammarRule rule, int i, Set<String> formals,
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
				checkActual(rule, i, formals, actual, sub, false));
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
		
		static IReport unusedFormal(PGrammarRule rule, int j, Located<String> formal) {
			String msg = String.format("%s unused formal parameter \"%s\"",
					inRule(rule, j), formal.val);
			return IReport.of(msg, Severity.WARNING, formal);
		}
		
		static String inRule(PGrammarRule rule, int j) {
			if (j >= 0)
				return String.format("In rule \"%s\", production %d: ", rule.name.val, j);
			else
				return String.format("In rule \"%s\": ", rule.name.val);
		}
		
		static IReport undeclaredFormal(PGrammarRule rule, int j, PExtent extent, PExtent.Hole hole) {
			String msg = String.format("%s undeclared formal parameter \"%s\"",
					inRule(rule, j), hole.name);
			return IReport.of(msg, Severity.ERROR, extent, hole);
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
		
		static IReport noargSymbolPassed(PGrammarRule rule, int j,
			Located<String> symbol, Located<String> nterm, String formal) {
			boolean term = Character.isUpperCase(symbol.val.charAt(0));
			String msg = String.format("%s formal parameter \"%s\" of non-terminal \"%s\""
					+ " must accept arguments, but %s\"%s\" %s",
				inRule(rule, j), formal, nterm.val, 
				term ? "" : "non-terminal ", symbol.val,
				term ? "is a token" : "does not expect arguments");
			return IReport.of(msg, Severity.ERROR, symbol);
		}
		
		static IReport argSymbolPassed(PGrammarRule rule, int j,
			Located<String> symbol, Located<String> nterm, String formal) {
			String msg = String.format("%s formal parameter \"%s\" of non-terminal \"%s\""
					+ " does not accept arguments, but non-terminal \"%s\" is declared "
					+ "with arguments",
				inRule(rule, j), formal, nterm.val, symbol.val);
			return IReport.of(msg, Severity.ERROR, symbol);
		}

		static IReport voidNonTerminalBound(PGrammarRule rule, int j,
			Located<String> binding, Located<String> nterm) {
			String msg = String.format("%s bound non-terminal \"%s\" returns void",
				inRule(rule, j), nterm.val);
			return IReport.of(msg, Severity.ERROR, binding);
		}

		static IReport voidSymbolPassedAsValued(PGrammarRule rule, int j,
			Located<String> symbol, Located<String> nterm, String formal) {
			boolean term = Character.isUpperCase(symbol.val.charAt(0));
			String msg = String.format("%s formal parameter \"%s\" of non-terminal \"%s\""
					+ " expects valued expression but %s \"%s\" %s",
				inRule(rule, j), formal, nterm.val, 
				term ? "token" : "non-terminal", symbol.val,
				term ? "has no declared value" : "returns void");
			return IReport.of(msg, Severity.ERROR, symbol);
		}
		
		static IReport incompatibleSorts(PGrammarRule rule, int j,
			Located<String> formal, PGrammars.Sort found, PGrammars.Sort expected) {
			String msg = String.format("%s formal parameter \"%s\" used here %s arguments, " 
					+ "but it was already used %s arguments in this rule",
				inRule(rule, j), formal.val, 
				expected.requiresArgs == Args.MANDATORY ? "with" : "without",
				found.requiresArgs == Args.MANDATORY ? "with" : "without");
			return IReport.of(msg, Severity.ERROR, formal);
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
			
		static IReport parametricPublicRule(PGrammarRule rule) {
			String msg = String.format("Public rule \"%s\" cannnot be parametric", rule.name.val);
			return IReport.of(msg, Severity.ERROR, rule.name);
		}
	}
}