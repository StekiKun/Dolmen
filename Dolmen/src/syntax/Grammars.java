package syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.annotation.Nullable;

import common.Maps;

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
	public static Set<String> nullable(Dependencies deps, Grammar grammar) {
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
}
