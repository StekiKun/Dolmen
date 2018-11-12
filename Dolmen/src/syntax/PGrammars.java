package syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Lists;
import common.Maps;
import common.Nulls;
import common.SCC;
import syntax.PProduction.ActualExpr;

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
	public static final class Dependencies implements SCC.Graph<String> {
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

		@Override
		public int size() {
			return forward.size();
		}

		private @Nullable Map<String, Integer> indexing = null;
		
		@Override
		public int index(String n) {
			@Nullable Map<String, Integer> indices = indexing;
			if (indices == null) {
				int idx = 0;
				indices = new HashMap<>();
				for (String sym : forward.keySet())
					indices.put(sym, idx++);
				indexing = indices;
			}
			@Nullable Integer res = indices.get(n);
			if (res == null) throw new NoSuchElementException();
			return res;
		}

		@Override
		public void successors(String n, Consumer<String> f) {
			@Nullable Set<String> succs = forward.get(n);
			if (succs == null) throw new NoSuchElementException();
			succs.forEach(f);
		}

		@Override
		public void iter(Consumer<String> f) {
			forward.keySet().forEach(f);
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

	@SuppressWarnings("javadoc")
	public static enum Sort {
		ALL(false, false),
		VALUED(true, false),
		ARGS(false, true),
		ARGS_VALUED(true, true);
		
		public final boolean requiresValue;
		public final boolean requiresArgs;
		
		private Sort(boolean requiresValue, boolean requiresArgs) {
			this.requiresValue = requiresValue;
			this.requiresArgs = requiresArgs;
		}
		
		static Sort of(boolean requiresValue, boolean requiresArgs) {
			if (requiresValue)
				return requiresArgs ? ARGS_VALUED : VALUED;
			return requiresArgs ? ARGS : ALL;
		}

		static final Sort unify(Sort s1, Sort s2) {
			if (s1 == s2) return s1;
			if (s1 == ARGS_VALUED) return s1;
			if (s2 == ARGS_VALUED) return s2;
			if (s1 == ALL) return s2;
			if (s2 == ALL) return s1;
			// At that point, s1 and s2 are different
			// and belong to {VALUED, ARGS}
			return ARGS_VALUED;
		}
	}
	
	@SuppressWarnings("javadoc")
	public static Map<String, List<Sort>> analyseGrammar(
		PGrammar grammar, Dependencies dependencies, Reporter reporter) {
		
		Map<String, List<Sort>> sorts =
			new SortInference(grammar, dependencies).compute();
		for (Map.Entry<String, List<Sort>> ruleSorts : sorts.entrySet()) {
			String ruleName = ruleSorts.getKey();
			PGrammarRule rule = grammar.rule(ruleName);
			if (rule.params.isEmpty()) continue;
			System.out.print("Rule " + ruleName + ": ");
			for (int i = 0; i < rule.params.size(); ++i) {
				if (i > 0) System.out.print(", ");
				System.out.print(rule.params.get(i).val
						+ " ∈ " + ruleSorts.getValue().get(i));
			}
			System.out.println("");
		}
		
		@SuppressWarnings("unused")
		Set<String> valuedTokens = grammar.tokenDecls.stream()
				.filter(td -> td.valueType != null)
				.map(td -> td.name.val)
				.collect(Collectors.toSet());

		return sorts;
	}
	
	private static final class SortInference {
		private final PGrammar grammar;
		private final Dependencies dependencies;
		
		private final Map<String, List<Sort>> sorts;
		
		SortInference(PGrammar grammar, Dependencies dependencies) {
			this.grammar = grammar;
			this.dependencies = dependencies;
			this.sorts = new LinkedHashMap<>();
		}
		
		Map<String, List<Sort>> compute() {
			SCC<String> sccs = SCC.of(dependencies);
			sccs.iter(this::handleSCC);
			return sorts;
		}
		
		void handleSCC(List<String> scc) {
			// Simple case of an SCC reduced to one rule
			if (scc.size() == 1) {
				String ruleName = scc.get(0);
				sorts.put(ruleName, handleRule(grammar.rule(ruleName)));
				return;
			}
			// Block of mutually recursive rules
			List<PGrammarRule> rules = Lists.transform(scc, grammar::rule);
			handleMutualRules(rules);
		}
		
		void handleMutualRules(List<PGrammarRule> rules) {
			// For every rule, initialize a result in the sorts array
			// so that mutually recursive occurrences can be resolved
			List<Map<String, Sort>> mutParamSorts = new ArrayList<>();
			for (PGrammarRule rule : rules) {
				if (rule.params.isEmpty()) {
					mutParamSorts.add(Maps.empty());
					continue;
				}
				// Initialize sorts for all formal parameters
				Map<String, Sort> paramSorts = new LinkedHashMap<>();
				for (Located<String> param : rule.params)
					paramSorts.put(param.val, Sort.ALL);
				mutParamSorts.add(paramSorts);
				sorts.put(rule.name.val, new ArrayList<>(paramSorts.values()));
			}

			// Now this is a bit of a silly strategy but let's handle
			// all rules every iteration, and iterate as long as one of the
			// rule's sorts changes.
			boolean changed = false;
			do {
				int idx = 0;
				for (PGrammarRule rule : rules) {
					if (rule.params.isEmpty()) continue;
					List<Sort> previous = sorts.get(rule.name.val);
					List<Sort> newer = handleRule0(rule, mutParamSorts.get(idx++));
					if (!newer.equals(previous)) {
						changed = true;
						sorts.replace(rule.name.val, newer);
					}
				}
			} while (changed);
			
			// At this point, the sorts are stable for the whole block
			// and the results are already stored in [sorts]. We are done.
		}

		List<Sort> handleRule(PGrammarRule rule) {
			if (rule.params.isEmpty()) return Lists.empty();
			// Initialize sorts for all formal parameters
			Map<String, Sort> paramSorts = new LinkedHashMap<>();
			for (Located<String> param : rule.params)
				paramSorts.put(param.val, Sort.ALL);
			sorts.put(rule.name.val, new ArrayList<>(paramSorts.values()));
			// Handle the rule
			return handleRule0(rule, paramSorts);
		}

		List<Sort> handleRule0(PGrammarRule rule, Map<String, Sort> paramSorts) {
			// Handle return type and arguments
			handleExtent(paramSorts, rule.returnType);
			handleExtent(paramSorts, rule.args);
			// Handle productions
			for (PProduction prod : rule.productions) {
				for (PProduction.Item item : prod.items) {
					switch (item.getKind()) {
					case ACTION:
						handleExtent(paramSorts, ((PProduction.ActionItem) item).extent);
						break;
					case ACTUAL: {
						PProduction.Actual actual = (PProduction.Actual) item;
						handleActual(paramSorts, actual.item,
							Sort.of(actual.isBound(), actual.args != null));
						break;
					}
					case CONTINUE:
						// Continuation do not change the sort
						break;
					}
				}
			}

			// Return the array giving the sort for each formal parameter of the rule
			return new ArrayList<>(paramSorts.values());
		}
		
		void handleExtent(Map<String, Sort> paramSorts, @Nullable PExtent extent) {
			if (extent == null) return;
			for (PExtent.Hole hole : extent.holes)
				paramSorts.merge(hole.name, Sort.VALUED, Sort::unify);
		}
		
		void handleActual(Map<String, Sort> paramSorts, ActualExpr aexpr, Sort sort) {
			if (aexpr.isTerminal()) return;
			String sym = aexpr.symb.val;
			// If the expression is reduced to a formal parameter, record its local sort
			if (paramSorts.containsKey(sym)) {
				paramSorts.merge(sym, sort, Sort::unify);
				return;
			}
			if (aexpr.params.isEmpty()) return;
			// If the expression is a non-terminal application, descend recursively
			// in all subterms, applying the sort which is already known for that
			// non-terminal (we know we have seen it before by virtue of the fact
			// that SCCs are visited in reverse topological order, and because
			// partial results for the rules in the current SCC have been introduced
			// in [sorts] to handle recursion/mutual recursion.
			@Nullable List<Sort> ntsorts = sorts.get(sym);
			if (ntsorts == null)
				throw new IllegalStateException("Non-terminal " + sym + " was not visited yet");
			for (int i = 0; i < aexpr.params.size(); ++i)
				handleActual(paramSorts, aexpr.params.get(i), ntsorts.get(i));
			return;
		}
	}
	
}