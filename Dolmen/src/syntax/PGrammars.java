package syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import common.Nulls;
import syntax.Located;
import syntax.PGrammar;
import syntax.PGrammarRule;
import syntax.PProduction;
import syntax.PProduction.ActualExpr;
import syntax.Reporter;

/**
 * Static utilities about {@link PGrammar}s
 * 
 * @author Stéphane Lescuyer
 */
public abstract class PGrammars {

	private PGrammars() {
		// Static utility class only
	}

	/**
	 * Represents the dependencies between the various non-terminals
	 * in some grammar. Dependencies can be useful when computing
	 * fix-points on grammar productions.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Dependencies {
		/**
		 * The forward dependencies associate to each non-terminal
		 * the set of non-terminals which appear in the right-hand
		 * side of its productions
		 */
		public final Map<String, Set<String>> forward;
		/**
		 * The backward dependencies associate to each non-terminal
		 * the set of non-terminals which mention it in the 
		 * right-hand side of their productions
		 */
		public final Map<String, Set<String>> backward;
		
		/**
		 * Builds the given dependencies
		 * @param forward
		 * @param backward
		 */
		public Dependencies(Map<String, Set<String>> forward, Map<String, Set<String>> backward) {
			this.forward = forward;
			this.backward = backward;
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("{ forward=");
			forward.forEach((nterm, s) ->
				buf.append("\n  ").append(nterm).append(" -> ").append(s));
			buf.append("\n  backward=");
			backward.forEach((nterm, s) ->
				buf.append("\n  ").append(nterm).append(" -> ").append(s));
			buf.append("\n}");
			return buf.toString();
		}
	}
	
	/**
	 * Computes the map of <i>dependencies</i> between the non-terminals
	 * of the given grammar rules, i.e. associates to every non-terminal in
	 * {@code rules} the set of non-terminals which appear in the
	 * right-hand side of productions for this non-terminal.
	 * 
	 * @param rules
	 * @return the map of dependencies
	 */
	public static Dependencies dependencies(Map<String, PGrammarRule> rules) {
		final Map<String, Set<String>> fwd = new HashMap<>(rules.size());
		final Map<String, Set<String>> bwd = new HashMap<>(rules.size());
		for (String nterm : rules.keySet()) {
			fwd.put(nterm, new HashSet<>());
			bwd.put(nterm, new HashSet<>());
		}
		
		Set<String> called = new HashSet<>();
		for (PGrammarRule rule : rules.values()) {
			final String name = rule.name.val;
			// fwd was initialized with all defined non-terminals
			final Set<String> deps = Nulls.ok(fwd.get(name));
			
			for (PProduction prod : rule.productions) {
				for (PProduction.Item item : prod.items) {
					switch (item.getKind()) {
					case ACTION:
						break;
					case ACTUAL:
						PProduction.Actual actual = (PProduction.Actual) item;
						findNTerms(actual.item, rule.params, called);
						break;
					case CONTINUE:
						called.add(name);
						break;
					}
					if (called.isEmpty()) continue;
					
					called.forEach(c -> {
						deps.add(c);
						// bwd was initialized with all defined non-terminals
						// (and the PGrammar.Builder ensures that a production
						//  rule cannot reference undefined non-terminals)
						Nulls.ok(bwd.get(c)).add(name);
					});
					called.clear();
				}
			}
		}
		
		return new Dependencies(fwd, bwd);
	}
	
	/**
	 * Finds all non-terminal symbols which appear in {@code aexpr}
	 * and adds them to {@code nterms}. {@code formals} is the list
	 * of locally available formal parameters.
	 * 
	 * @param aexpr
	 * @param formals
	 * @param nterms
	 */
	private static void findNTerms(ActualExpr aexpr, 
			List<Located<String>> formals, Set<String> nterms) {
		if (aexpr.isTerminal()) return;
		if (formals.contains(aexpr.symb)) return;
		nterms.add(aexpr.symb.val);
		for (ActualExpr sexpr : aexpr.params)
			findNTerms(sexpr, formals, nterms);
	}
	
	/**
	 * Analyses the {@code grammar}'s dependencies {@code deps} to
	 * determine non-terminals and terminals which are not really being
	 * used in this grammar description.
	 * <p>
	 * For terminals, this amounts to not being used in any production,
	 * whereas for non-terminals, it means not being used transitively
	 * by a public non-terminal (in particular blocks of mutually
	 * recursive private non-terminals dot not "justify" each other,
	 * nor does continuation in a given non-terminal of course).
	 * <p>
	 * The problems found, if any, are reported into {@code reporter}.
	 * 
	 * @param grammar
	 * @param deps
	 * @param reporter
	 */
	public static void findUnusedSymbols(
		PGrammar grammar, Dependencies deps, Reporter reporter) {
		// Start with all terminals and private non-terminals as potential suspects
		final @NonNull Set<Located<String>> unusedTerms = 
			grammar.tokenDecls.stream().map(token -> token.name).collect(Collectors.toSet());
		final @NonNull Set<Located<String>> unusedPrivateRules =
			grammar.rules.values().stream()
				.filter(rule -> !rule.visibility)
				.map(rule -> rule.name).collect(Collectors.toSet());
		
		// Crawl the grammar from public non-terminals, following dependencies
		Stack<String> todo = new Stack<>();
		for (PGrammarRule rule : grammar.rules.values())
			if (rule.visibility) todo.add(rule.name.val);
		Set<String> visited = new HashSet<>();
		
		while (!todo.isEmpty()) {
			final String name = todo.pop();
			if (!visited.add(name)) continue;
			
			final PGrammarRule rule = grammar.rule(name);
			for (PProduction prod : rule.productions) {
				for (PProduction.Actual actual : prod.actuals()) {
					removeUsedSymbols(rule.params, actual.item, unusedTerms, unusedPrivateRules);
				}
				// No need to check potential continuation because
				// self-reference does not count as usage
			}
			todo.addAll(deps.forward.get(name));
		}
		// Report all remaining symbols 
		unusedTerms.forEach(tok -> 
			reporter.add(PGrammar.Reports.unusedTerminal(tok)));
		unusedPrivateRules.forEach(rule -> 
			reporter.add(PGrammar.Reports.unusedPrivateRule(rule)));
	}
	
	/**
	 * Traverse the actual expression {@code aexpr} and removes all occurring
	 * terminals (resp. non-terminals) from {@code unusedTerms} (resp.
	 * {@code unusedPrivateRules}.
	 * The set of locally declared formal parameters {@code formals} is given
	 * in order to distinguish formal parameters from non-terminals.
	 * 
	 * @param formals
	 * @param aexpr
	 * @param unusedTerms
	 * @param unusedPrivateRules
	 */
	private static void removeUsedSymbols(List<Located<String>> formals, ActualExpr aexpr, 
			Set<Located<String>> unusedTerms, Set<Located<String>> unusedPrivateRules) {
		if (aexpr.isTerminal()) {
			unusedTerms.remove(aexpr.symb);
			return;
		}
		if (formals.contains(aexpr.symb)) return;
		unusedPrivateRules.remove(aexpr.symb);
		for (ActualExpr sexpr : aexpr.params)
			removeUsedSymbols(formals, sexpr, unusedTerms, unusedPrivateRules);
	}

}