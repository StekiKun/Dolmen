package unparam;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Iterables;
import common.Nulls;
import common.SCC;
import common.SCC.Graph;
import syntax.Located;
import syntax.PGrammar;
import syntax.PGrammarRule;
import syntax.PProduction;
import syntax.PProduction.ActualExpr;

/**
 * @WIP
 * @author Stéphane Lescuyer
 */
@SuppressWarnings("javadoc")
public final class Expansion {

	public static final class PGrammarNotExpandable extends Exception {
		private static final long serialVersionUID = 4186980925380975221L;
		
		public final Located<String> rule;
		public final Located<String> formal;
		
		PGrammarNotExpandable(Formal formal) {
			super(String.format(
				"The parametric rule %s cannot be expanded because its parameter %s"
				+ " could grow infinitely.", formal.rule.val, formal.formal.val));
			this.rule = formal.rule;
			this.formal = formal.formal;
		}
	}
	
	private static final class Formal {
		private final int order;	// Ignored in equals/hashCode
		private final Located<String> rule;
		private final Located<String> formal;
		private final int formalIdx;
		
		Formal(int order, Located<String> rule, Located<String> formal, int formalIdx) {
			this.order = order;
			this.rule = rule;
			this.formal = formal;
			this.formalIdx = formalIdx;
		}
		
		@Override
		public int hashCode() {
			int result = rule.hashCode();
			result = 31 * result + formalIdx;
			return result;
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof Formal)) return false;
			Formal formal = (Formal) o;
			if (formalIdx != formal.formalIdx) return false;
			if (!rule.equals(formal.rule)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			return rule.val + "@" + formal.val;
		}
	}
	
	private static final class ExpansionFlowGraph implements Graph<Formal> {
		private final PGrammar grammar;
		
		static final class Edge {
			private boolean safe;
			@SuppressWarnings("unused")
			private Located<String> site;
			
			Edge(boolean safe, Located<String> site) {
				this.safe = safe;
				this.site = site;
			}
		}
		
		private final Map<Located<String>, @NonNull Formal[]> formals;
		final Map<Formal, Map<Formal, Edge>> graph;
		
		// + dangerous
		
		private ExpansionFlowGraph(PGrammar grammar) {
			// We will represent the graph with a successor map associating
			// each formal to the set of formals it flows into. We initialize
			// it by looking for all formals in order of appearance in the
			// parametric grammar.
			this.grammar = grammar;
			this.graph = new HashMap<>();
			this.formals = new HashMap<>();
			
			// Discover all formal parameters and assign them a unique Formal node
			int order = 0;
			for (PGrammarRule rule : grammar.rules.values()) {
				if (rule.params.isEmpty()) continue;

				Formal[] ruleFormals = new Formal[rule.params.size()];
				int idx = 0;
				for (Located<String> param : rule.params) {
					Formal formal = new Formal(order++, rule.name, param, idx);
					ruleFormals[idx++] = formal;
					graph.put(formal, new HashMap<>());
				}
				// All cells in ruleFormals have been set in the loop
				formals.put(rule.name, Nulls.arrayOk(ruleFormals));
			}			
		}
		
		static ExpansionFlowGraph of(PGrammar grammar) {
			return (new ExpansionFlowGraph(grammar)).compute();
		}
		
		private @NonNull Formal[] formals(Located<String> rule) {
			@NonNull Formal @Nullable [] ruleFormals = formals.get(rule);
			if (ruleFormals == null)
				throw new IllegalStateException();
			return ruleFormals;
		}
		
		private ExpansionFlowGraph compute() {
			Stack<Formal> context = new Stack<>();
			for (PGrammarRule rule : grammar.rules.values()) {
				if (rule.params.isEmpty()) continue;

				// Map every formal parmeter name in this rule to the corresponding
				// node in the expansion graph
				@NonNull Formal [] ruleFormals = formals(rule.name);
				int idx = 0;
				Map<String, Formal> paramToFormal = new HashMap<>();
				for (Located<String> param : rule.params)
					paramToFormal.put(param.val, ruleFormals[idx++]);
				
				// If the rule is parametric, we look for all rules used in
				// actuals to find where each parameter flows into.
				// NB: we can ignore potential continuation because they
				// do not affect the expandability of the grammar. Indeed
				// edges which correspond to continuations are simply safe reflexive
				// edges in the flow graph between formals, and reflexive edges do not
				// change the strongly-connected components.
				for (PProduction.Actual actual :
					Iterables.concat(Iterables.transform(rule.productions, PProduction::actuals))) {
					ActualExpr aexpr = actual.item;
					if (aexpr.params.isEmpty()) continue;
					context.clear();
					addFlowEdges(paramToFormal, context, aexpr);
				}
			}
			return this;
		}
		
		private void addFlowEdges(Map<String, Formal> paramToFormal,
			Stack<Formal> context, ActualExpr expr) {
			@Nullable Formal formal = paramToFormal.get(expr.symb.val);
			if (formal != null) {
				// There is an expansion flow from [formal] to every formal
				// parameter in [context]. 
				// It is safe if and only if the expression was shallow, ie for
				// the last step in the context path. If there is an edge between
				// two formals already, we only replace it if it was safe and the 
				// new one is dangerous.
				@Nullable Map<Formal, Edge> succs = graph.get(formal);
				if (succs == null)
					throw new IllegalStateException();
				Formal target;
				for (int i = 0; i < context.size(); ++i) {
					target = context.get(i);
					boolean safe = i == context.size() - 1;
					
					@Nullable Edge edge = succs.get(target);
					if (edge == null) {
						// New edge
						edge = new Edge(safe, expr.symb);
						succs.put(target, edge);
					}
					else {
						// Is the new edge more dangerous?
						if (!safe && edge.safe) {
							edge.safe = false;
							edge.site = expr.symb;
						}
					}
				}
				if (!expr.params.isEmpty())
					throw new IllegalStateException();
				return;
			}
			
			// Look for occurrences in subexpressions. These occurrences are
			// in a context that is extended by the formal corresponding to
			// the relative place of the subexpression.
			@NonNull Formal[] symFormals = formals(expr.symb);
			if (symFormals.length != expr.params.size())
				throw new IllegalStateException();
			int pidx = 0;
			for (ActualExpr sexpr : expr.params) {
				context.push(symFormals[pidx++]);
				addFlowEdges(paramToFormal, context, sexpr);
				context.pop();
			}
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("Expansion flow graph: (").append(graph.size()).append(" formals)\n");
			graph.forEach((src, succs) -> {
				succs.forEach((tgt, edge) -> {
					buf.append("  ")
						.append(src).append(edge.safe ? " ---> " : " +++> ")
						.append(tgt).append("\n");
				});
			});
			return buf.toString();
		}
		
		// Implementation of the Graph<Integer> interface
		
		@Override
		public int size() {
			return graph.size();
		}

		@Override
		public int index(Formal n) {
			if (!graph.containsKey(n))
				throw new NoSuchElementException();
			return n.order;
		}

		@Override
		public void successors(Formal n, Consumer<Formal> f) {
			@Nullable Map<Formal, Edge> succs = graph.get(n);
			if (succs == null) throw new NoSuchElementException();
			succs.keySet().forEach(f);
		}

		@Override
		public void iter(Consumer<Formal> f) {
			graph.keySet().forEach(f);
		}
	}
	
	public static void checkExpandability(PGrammar grammar) throws PGrammarNotExpandable {
		// Build the flow graph between formals and compute
		// the strongly-connected components
		ExpansionFlowGraph graph = ExpansionFlowGraph.of(grammar);
		SCC<Formal> sccs = SCC.of(graph);
		System.out.print(graph);
		System.out.print(sccs);
		// The expansion is guaranteed to terminate if no dangerous
		// flow edge is internal to some SCC
		for (Map.Entry<Formal, Map<Formal, ExpansionFlowGraph.Edge>> e : 
			graph.graph.entrySet()) {
			Formal src = e.getKey();
			for (Map.Entry<Formal, ExpansionFlowGraph.Edge> esucc : e.getValue().entrySet()) {
				if (esucc.getValue().safe) continue;
				Formal tgt = esucc.getKey();
				if (sccs.representative(src) != sccs.representative(tgt)) continue;
				// We have found a dangerous cycle, the grammar is not expandable
				// Let's describe the potential cycle completely
				throw new PGrammarNotExpandable(src);
			}
		}
		return;
	}
	
}
