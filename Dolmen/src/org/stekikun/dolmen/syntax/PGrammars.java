package org.stekikun.dolmen.syntax;

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
import org.stekikun.dolmen.common.Lists;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.common.Nulls;
import org.stekikun.dolmen.common.SCC;
import org.stekikun.dolmen.syntax.PProduction.ActualExpr;

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

	/**
	 * Enumeration representing the different constraints which can exist
	 * on the arguments of a symbol in a grammar: arguments can either
	 * be mandatory, forbidden, or there may be no constraint at all.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@SuppressWarnings("javadoc")
	public static enum Args {
		MANDATORY, 
		FORBIDDEN, 
		ANY
	}
	
	/**
	 * This enumeration describes the various sorts which can characterize
	 * the kind of terminals or non-terminals (or more generally actual
	 * expressions) that can stand for some formal parameter in a parametric
	 * grammar.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static enum Sort {
		/** Sort which describes formals which can stand for any expression */
		ALL(false, Args.ANY),
		/** 
		 * Sort which describes formals which only stand for expressions that
		 * do not expect arguments, i.e. tokens or applications of non-terminals
		 * which were declared without arguments
		 */
		NO_ARGS(false, Args.FORBIDDEN),
		/** 
		 * Sort which describes formals which only stand for valued expressions,
		 * i.e. valued tokens or applications of non-terminals which do not
		 * simply return {@code void} and expect no arguments
		 */
		VALUED(true, Args.ANY),
		/** 
		 * Sort which describes formals which only stand for expressions that
		 * expect arguments, i.e. applications of non-terminals which were
		 * declared with arguments 
		 */
		ARGS(false, Args.MANDATORY),
		/** 
		 * Sort which describes formals which only stand for valued expressions that
		 * expect arguments, i.e. applications of non-terminals which do not
		 * simply return {@code void} and were declared with arguments
		 */
		NO_ARGS_VALUED(true, Args.FORBIDDEN),
		/** 
		 * Sort which describes formals which only stand for valued expressions
		 * that expect arguments, i.e. applications of non-terminals which were
		 * declared with arguments and do not simply return {@code void}
		 */
		ARGS_VALUED(true, Args.MANDATORY);
		
		/**
		 * Whether this sort requires valued expressions
		 */
		public final boolean requiresValue;
		/**
		 * Whether this sort requires expressions that expect arguments or
		 * expressions that expect no arguments, or whether everything is
		 * allowed
		 */
		public final Args requiresArgs;
		
		private Sort(boolean requiresValue, Args requiresArgs) {
			this.requiresValue = requiresValue;
			this.requiresArgs = requiresArgs;
		}
		
		/**
		 * @param requiresValue
		 * @param requiresArgs
		 * @return the sort constant representing the given constraints
		 */
		static Sort of(boolean requiresValue, Args requiresArgs) {
			switch (requiresArgs) {
			case ANY:
				return requiresValue ? VALUED : ALL;
			case FORBIDDEN:
				return requiresValue ? NO_ARGS_VALUED : NO_ARGS;
			case MANDATORY:
				return requiresValue ? ARGS_VALUED : ARGS;
			}
			throw new IllegalStateException("Unknown Args kind: " + requiresArgs);
		}

		/**
		 * @param s1
		 * @param s2
		 * @return the sort representing the intersection
		 * 	of the constraints given by {@code s1} and {@code s2},
		 *  or {@code null} if the sorts are incompatible
		 */
		static final @Nullable Sort unify(Sort s1, Sort s2) {
			if (s1 == s2) return s1;
			if (s1 == ALL) return s2;
			if (s2 == ALL) return s1;
			boolean valued = s1.requiresValue && s2.requiresValue;
			Args args = s1.requiresArgs;
			switch (s2.requiresArgs) {
			case ANY:
				break;
			case FORBIDDEN:
				if (args == Args.MANDATORY) return null;
				args = Args.FORBIDDEN;
				break;
			case MANDATORY:
				if (args == Args.FORBIDDEN) return null;
				args = Args.MANDATORY;
				break;
			}
			return of(valued, args);
		}
	}
	
	private static final boolean debug = false;
	
	/**
	 * This analyses various rule applications in the parametric
	 * grammar {@code grammar} by inferring {@linkplain Sort sorts}
	 * for all the rules' formal parameters and checking that
	 * effective parameters in all applications are abiding by
	 * the constraints expressed by the sorts. 
	 * <p>
	 * All problems found are reported in {@code reporter}.
	 * 
	 * @param grammar
	 * @param dependencies_	the {@linkplain Dependencies dependencies}
	 * 		between non-terminals in {@code grammar}, or {@code null}
	 * 		if the dependencies should be computed by the method
	 * @param reporter
	 * @return the map associating every non-terminal in {@code grammar}
	 * 	(even non-parametric ones) to the list of sorts that have been
	 * 	inferred for its formal parameters
	 */
	public static Map<String, List<Sort>> analyseGrammar(
		PGrammar grammar, @Nullable Dependencies dependencies_, Reporter reporter) {
		Dependencies dependencies = dependencies_ == null ?
			dependencies(grammar.rules) : dependencies_;
		
		// First, infer the sort of all formal parameters based
		// on how they are used in their rules
		Map<String, List<Sort>> sorts =
			new SortInference(grammar, dependencies, reporter).compute();

		if (debug) {
			for (Map.Entry<String, List<Sort>> ruleSorts : sorts.entrySet()) {
				@NonNull String ruleName = ruleSorts.getKey();
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
		}
		
		// Then check all rule applications against these sorts
		new SortChecker(grammar, sorts, reporter).check();
		
		return sorts;
	}
	
	/**
	 * This class analyses the way formal parameters are used in
	 * the productions across some parametric grammar in order
	 * to infer an adequate {@link Sort} for each formal representing
	 * the kind of actual expressions that it can accept safely.
	 * <p>
	 * When incompatible constraints are encountered on some formal
	 * parameter, the issue is reported in a configured {@link Reporter}.
	 * 
	 * @see #compute()
	 * @author Stéphane Lescuyer
	 */
	private static final class SortInference {
		private final PGrammar grammar;
		private final Dependencies dependencies;
		private final Reporter reporter;
		
		private final Map<String, List<Sort>> sorts;

		/**
		 * Initialize the sort inference on the given grammar, based
		 * on the {@linkplain Dependencies dependencies} for the grammar
		 * 
		 * @param grammar
		 * @param dependencies
		 * @param reporter
		 */
		SortInference(PGrammar grammar, Dependencies dependencies, Reporter reporter) {
			this.grammar = grammar;
			this.dependencies = dependencies;
			this.reporter = reporter;
			this.sorts = new LinkedHashMap<>();
		}
		
		/**
		 * Performs the whole sort inference for all rules and formals
		 * in the underlying grammar.
		 * <i>
		 *  The returned map contains all non-terminals, even
		 * 	the rules that are non-parametric.
		 * </i>
		 * 
		 * @return for each rule, the list of sorts of the rule's formals
		 * 	in the same order as they are declared
		 */
		Map<String, List<Sort>> compute() {
			SCC<String> sccs = SCC.of(dependencies);
			sccs.iter(this::handleSCC);
			return sorts;
		}
		
		/**
		 * Sort inference for the non-terminals in {@code scc},
		 * which form a strongly-connected component of the grammar.
		 * <p>
		 * Other non-terminals on which this SCC depends must already
		 * have been analyzed.
		 * 
		 * @param scc
		 */
		private void handleSCC(List<String> scc) {
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
		
		private void handleMutualRules(List<PGrammarRule> rules) {
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
			boolean changed;
			do {
				changed = false;
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

		private List<Sort> handleRule(PGrammarRule rule) {
			if (rule.params.isEmpty()) return Lists.empty();
			// Initialize sorts for all formal parameters
			Map<String, Sort> paramSorts = new LinkedHashMap<>();
			for (Located<String> param : rule.params)
				paramSorts.put(param.val, Sort.ALL);
			sorts.put(rule.name.val, new ArrayList<>(paramSorts.values()));
			// Handle the rule
			return handleRule0(rule, paramSorts);
		}

		private List<Sort> handleRule0(PGrammarRule rule, Map<String, Sort> paramSorts) {
			// Handle return type and arguments
			handleExtent(paramSorts, rule.returnType);
			handleExtent(paramSorts, rule.args);
			// Handle productions
			int prodIdx = 0;
			for (PProduction prod : rule.productions) {
				++prodIdx;
				for (PProduction.Item item : prod.items) {
					switch (item.getKind()) {
					case ACTION:
						handleExtent(paramSorts, ((PProduction.ActionItem) item).extent);
						break;
					case ACTUAL: {
						PProduction.Actual actual = (PProduction.Actual) item;
						handleActual(rule, prodIdx, paramSorts, actual.item,
							Sort.of(actual.isBound(),
								actual.args == null ? Args.FORBIDDEN : Args.MANDATORY));
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
		
		private void handleExtent(Map<String, Sort> paramSorts, @Nullable PExtent extent) {
			if (extent == null) return;
			for (PExtent.Hole hole : extent.holes)
				paramSorts.merge(hole.name, Sort.VALUED, (s1, s2) ->
					// Unification with Sort.VALUED always succeeds
					Nulls.ok(Sort.unify(s1, s2)));
		}
		
		private void handleActual(PGrammarRule rule, int prodIdx, 
				Map<String, Sort> paramSorts, ActualExpr aexpr, Sort sort) {
			if (aexpr.isTerminal()) return;
			String sym = aexpr.symb.val;
			// If the expression is reduced to a formal parameter, record its local sort
			if (paramSorts.containsKey(sym)) {
				paramSorts.merge(sym, sort, (s1, s2) -> {
					@Nullable Sort s = Sort.unify(s1, s2);
					if (s != null) return s;
					// The two sorts are not compatible, report the issue
					reporter.add(PGrammar.Reports.incompatibleSorts(
							rule, prodIdx, aexpr.symb, s1, s2));
					return s1; // arbitrary
				});
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
				handleActual(rule, prodIdx, paramSorts, aexpr.params.get(i), ntsorts.get(i));
			return;
		}
	}
	
	/**
	 * This class is used to inspect a parametric grammar and report all
	 * invalid rule applications based on the {@linkplain SortInference sorts inferred}
	 * for the various formal parameters in the grammar.
	 * 
	 * @see SortChecker#check()
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class SortChecker {
		private final PGrammar grammar;
		private final Map<String, List<Sort>> sorts;
		private final Reporter reporter;
		
		// Tokens which are declared with a value type
		private final Set<Located<String>> valuedTokens;
		// Non-terminals which are declared with arguments
		private final Set<Located<String>> argsNTerms;
		// Non-terminals which only return 'void'
		private final Set<Located<String>> voidNTerms;
		
		SortChecker(PGrammar grammar, Map<String, List<Sort>> sorts, Reporter reporter) {
			this.grammar = grammar;
			this.sorts = sorts;
			this.reporter = reporter;
			
			// Fetch the set of tokens which are valued, the
			// sets of non-terminals which expect arguments and
			// the sets of non-terminals which return void
			this.valuedTokens = grammar.tokenDecls.stream()
					.filter(td -> td.valueType != null)
					.map(td -> td.name)
					.collect(Collectors.toSet());
			this.argsNTerms = new HashSet<>();
			this.voidNTerms = new HashSet<>();
			for (PGrammarRule rule : grammar.rules.values()) {
				if (rule.args != null)
					argsNTerms.add(rule.name);
				if (rule.returnType.find().trim().equals("void"))
					voidNTerms.add(rule.name);
			}
		}

		/**
		 * Checks the underlying grammar based on the inferred sort map
		 * and reports all issues in the underlying {@link Reporter}
		 */
		void check() {
			// Go through the rules' productions and check that
			// all parametric rules applications are done in a way
			// that is consistent with the expected sorts.
			// NB: inconsistencies in valuedness and arguments on
			// top-level non-terminals and tokens have been reported
			// at the grammar creation.
			for (PGrammarRule rule : grammar.rules.values()) {
				int prodIdx = 0;
				// [sorts] contains sort information for all parametric rules
				List<Sort> formalSorts = 
					sorts.getOrDefault(rule.name.val, Lists.empty());
				Map<String, Sort> formals = new HashMap<>();
				for (int i = 0; i < rule.params.size(); ++i)
					formals.put(rule.params.get(i).val, formalSorts.get(i));
				
				for (PProduction prod : rule.productions) {
					++prodIdx;
					for (PProduction.Actual actual : prod.actuals()) {
						checkActualExpr(rule, prodIdx, formals, actual.item);
						// Perform an extra check for incompatible use of
						// formals with and without arguments

					}
				}
			}
		}
		
		private void checkActualExpr(PGrammarRule rule, int prodIdx,
				Map<String, Sort> formals, ActualExpr aexpr) {
			if (aexpr.params.isEmpty()) return;
			// Fetch the applied symbol and the sorts of its formal parameters
			Located<String> sym = aexpr.symb;
			// [sorts] contains sort information for all parametric rules
			List<Sort> symSorts = Nulls.ok(sorts.get(sym.val));
			// For all effective parameters, check that the head symbol is
			// compatible with the expected sort
			for (int i = 0; i < symSorts.size(); ++i) {
				Sort expectedSort = symSorts.get(i);
				ActualExpr effective = aexpr.params.get(i);
				final Sort foundSort;
				if (effective.isTerminal()) {
					foundSort = Sort.of(valuedTokens.contains(effective.symb), Args.FORBIDDEN);
				}
				else if (formals.containsKey(effective.symb.val)) {
					foundSort = Nulls.ok(formals.get(effective.symb.val));
				}
				else {
					foundSort = Sort.of(!voidNTerms.contains(effective.symb),
										argsNTerms.contains(effective.symb) ?
											Args.MANDATORY : Args.FORBIDDEN);
				}
				// Whether the rule expects a valued parameter and is fed
				// a non-valued effective parameter 
				if (expectedSort.requiresValue && !foundSort.requiresValue) {
					reporter.add(PGrammar.Reports.voidSymbolPassedAsValued(
						rule, prodIdx,
						effective.symb, sym, grammar.rule(sym.val).params.get(i).val));
				}
				// Whether the rule expects a parameter w/ args and is fed
				// a no-arg effective parameter.
				if (expectedSort.requiresArgs == Args.MANDATORY 
						&& foundSort.requiresArgs == Args.FORBIDDEN) {
					reporter.add(PGrammar.Reports.noargSymbolPassed(
							rule, prodIdx, 
							effective.symb, sym, grammar.rule(sym.val).params.get(i).val));					
				}
				// Whether the rule expects a parameter w/out args and is fed
				// an effective parameter with arguments
				else if (expectedSort.requiresArgs == Args.FORBIDDEN 
						&& foundSort.requiresArgs == Args.MANDATORY) {
					reporter.add(PGrammar.Reports.argSymbolPassed(
							rule, prodIdx, 
							effective.symb, sym, grammar.rule(sym.val).params.get(i).val));										
				}
				// Don't forget to check the subexpression recursively
				checkActualExpr(rule, prodIdx, formals, effective);
			}
		}
	}
	
}