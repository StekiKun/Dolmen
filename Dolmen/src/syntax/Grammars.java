package syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.annotation.Nullable;

import common.Maps;
import common.Sets;

/**
 * Static utilities about {@link Grammar}s
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Grammars {

	private Grammars() {
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
		 * The backward dependencies assocaite to each non-terminal
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
	}
	
	/**
	 * Computes the map of <i>dependencies</i> between the non-terminals
	 * of the given grammar, i.e. associates to every non-terminal in
	 * {@code grammar} the set of non-terminals which appear in the
	 * right-hand side of productions for this non-terminal.
	 * 
	 * @param grammar
	 * @return the map of dependencies
	 */
	public static Dependencies dependencies(Grammar grammar) {
		final Map<String, Set<String>> fwd = new HashMap<>(grammar.rules.size());
		final Map<String, Set<String>> bwd = new HashMap<>(grammar.rules.size());
		for (String nterm : grammar.rules.keySet()) {
			fwd.put(nterm, new HashSet<>());
			bwd.put(nterm, new HashSet<>());
		}
		
		for (GrammarRule rule : grammar.rules.values()) {
			final String name = rule.name;
			final Set<String> deps = fwd.get(name);
			
			for (Production prod : rule.productions) {
				for (Production.Item item : prod.items) {
					if (!item.isTerminal()) {
						deps.add(item.item);
						bwd.get(item.item).add(name);
					}
				}
			}
		}
		
		return new Dependencies(fwd, bwd);
	}
	
	/**
	 * Gathers the result of the computation of the 
	 * NULLABLE, FIRST and FOLLOW sets on non-terminals
	 * in a given grammar
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class NTermsInfo {
		/** 
		 * The set of nullable non-terminals, i.e. from
		 * which the empty string can be derived
		 */
		public final Set<String> nullable;
		/** 
		 * For each non-terminal X, the set of terminals 
		 * that can begin strings derived from X 
		 */
		public final Map<String, Set<String>> first;
		/**
		 * For each non-terminal X, the set of terminals
		 * which can follow X immediately in derived strings
		 */
		public final Map<String, Set<String>> follow;
		
		private NTermsInfo(Set<String> nullable,
			Map<String, Set<String>> first, Map<String, Set<String>> follow) {
			this.nullable = nullable;
			this.first = first;
			this.follow = follow;
		}
		
		/**
		 * @param nterm
		 * @return whether the non-terminal {@code nterm} is nullable
		 */
		public boolean nullable(String nterm) {
			return nullable.contains(nterm);
		}
		
		/**
		 * @param nterm
		 * @return the set of terminals that can begin strings
		 * 	derived from the non-terminal {@code nterm}
		 */
		public Set<String> first(String nterm) {
			return first.get(nterm);
		}
		
		/**
		 * @param prod
		 * @return the set of terminals that can begin strings
		 * 	derived from the given production {@code prod}
		 */
		public Set<String> first(Production prod) {
			Set<String> res = Sets.empty();
			for (Production.Item item : prod.items) {
				if (item.isTerminal()) {
					res = Sets.union(res, Sets.singleton(item.item));
					break;	// terminal is not nullable
				}
				res = Sets.union(res, first(item.item));
				if (!nullable(item.item)) break;
			}
			return res;
		}
		
		/**
		 * @param nterm
		 * @return the set of terminals that can immediately
		 * 	follow {@code nterm} in derived strings
		 */
		public Set<String> follow(String nterm) {
			return follow.get(nterm);
		}
		
		@Override
		public String toString() {
			return "{" +
				"nullable = " + nullable + ", " +
				"first = " + first + ", " +
				"follow = " + follow +
				"}";
		}
	}
	
	private static @Nullable Boolean
		nullableProd(Production prod, Map<String, Boolean> nullable) {
		for (Production.Item item : prod.items) {
			if (item.isTerminal()) return false;
			String id = item.item;
			@Nullable Boolean b = Maps.get(nullable, id);
			if (b != null) return b;
		}
		return null;
	}
	
	/**
	 * <i>NB: In all generality this is only a correct approximation, so 
	 *  non-terminals which are not in the resulting state are not guaranteed
	 *  to always produce non-empty matches.</i>
	 * 
	 * @param deps
	 * @param grammar
	 * @return the set of non-terminals in {@code grammar} 
	 * 	which can produce empty sequence of tokens 
	 */
	protected static Set<String> nullable(Dependencies deps, Grammar grammar) {
		Map<String, Boolean> nullable = new HashMap<>(grammar.rules.size());
		
		Stack<String> todo = new Stack<>();
		grammar.rules.keySet().forEach(key -> todo.push(key));
		while (!todo.isEmpty()) {
			final String name = todo.pop();
			if (nullable.containsKey(name)) continue;
			
			final GrammarRule rule = grammar.rules.get(name);
			boolean allNonNull = true;
			for (Production prod : rule.productions) {
				@Nullable Boolean b = nullableProd(prod, nullable);
				if (b == null) {
					allNonNull = false; continue;
				}
				if (b) {
					// Now we know it is nullable, let's add all
					// backward dependers to the todo stack and proceed
					nullable.put(name, true);
					todo.addAll(deps.backward.get(name));
					continue;
				}
			}
			// If all certainly non-empty, no need to retry 
			// this rule later
			if (allNonNull)
				nullable.put(name, false);
		}
		
		Set<String> res = new HashSet<>();
		nullable.forEach((s, b) -> { if (b) res.add(s); });
		return res;
	}
	
	protected static Map<String, Set<String>>
		first(Dependencies deps, Grammar grammar, Set<String> nullable) {
		Map<String, Set<String>> res = new HashMap<>(grammar.rules.size());
		// Initialize all first sets to empty
		for (String nterm : grammar.rules.keySet())
			res.put(nterm, new HashSet<>());
		
		Stack<String> todo = new Stack<>();
		grammar.rules.keySet().forEach(key -> todo.push(key));
		while (!todo.isEmpty()) {
			final String name = todo.pop();
			Set<String> first = res.get(name);
			
			final GrammarRule rule = grammar.rules.get(name);
			boolean changed = false;
			prod:
			for (Production prod : rule.productions) {
				// For each production, we go through the items in order
				// until we encounter a non-nullable item. Each item
				// visited is added to the rule's first set.
				for (Production.Item item : prod.items) {
					if (item.isTerminal()) {
						changed |= first.add(item.item);
						continue prod;
					}
					changed |= first.addAll(res.get(item.item));
					if (!nullable.contains(item.item)) break;
				}
			}
			// If we changed the FIRST set of the current rule,
			// we need to revisit the non-terminals which depend on it
			if (changed)
				todo.addAll(deps.backward.get(name));
		}
		
		return res;
	}

	protected static Map<String, Set<String>> follow(Dependencies deps, 
			Grammar grammar, Set<String> nullable, Map<String, Set<String>> first) {
		Map<String, Set<String>> res = new HashMap<>(grammar.rules.size());
		// Initialize all follow sets to empty
		for (String nterm : grammar.rules.keySet())
			res.put(nterm, new HashSet<>());
		
		Set<String> changed = new HashSet<>();
		Stack<String> todo = new Stack<>();
		grammar.rules.keySet().forEach(key -> todo.push(key));
		while (!todo.isEmpty()) {
			final String name = todo.pop();

			final GrammarRule rule = grammar.rules.get(name);
			changed.clear();
			Set<String> follow = res.get(name);	// beware: for R only
			for (Production prod : rule.productions) {
				// For each production, we go through the items in reverse
				// order by remembering the set of possible following terminals
				// the last non-terminal such that all items in-between 
				// are nullable.
				for (int k = prod.items.size() - 1; k >= 0; --k) {
					Production.Item item = prod.items.get(k);
					if (item.isTerminal()) {
						// When encountering a terminal item, we can
						// flush the current follow set and use the
						// current item instead
						follow = Sets.singleton(item.item);
						continue;
					}
					// When encountering a non-terminal item, we
					// extend its follow set with the current follow set
					// and record whether it changed or not
					String nterm = item.item;
					if (res.get(nterm).addAll(follow))
						changed.add(nterm);
					// If that non-terminal is nullable, proceed with
					// an extended follow set. Otherwise, reset the
					// follow set to FIRST(nterm).
					if (nullable.contains(nterm))
						follow = Sets.union(follow, first.get(nterm));
					else
						follow = first.get(nterm);
				}
			}
			// For every non-terminal whose follow set changed,
			// revisit the non-terminals on which it depends
			for (String ch : changed)
				todo.addAll(deps.forward.get(ch));
		}
		
		return res;
	}
	
	/**
	 * Analyze the given {@code grammar} and compute for every non-terminal
	 * whether it is <i>nullable</i>, and the associated FIRST and FOLLOW
	 * sets.
	 * 
	 * @param grammar
	 * @return the results of the analysis
	 */
	public static NTermsInfo analyseGrammar(Grammar grammar) {
		Dependencies deps = dependencies(grammar);
		Set<String> nullable = nullable(deps, grammar);
		Map<String, Set<String>> first = first(deps, grammar, nullable);
		Map<String, Set<String>> follow = follow(deps, grammar, nullable, first);
		return new NTermsInfo(nullable, first, follow);
	}
}