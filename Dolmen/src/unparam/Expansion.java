package unparam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Iterables;
import common.Lists;
import common.Maps;
import common.Nulls;
import common.SCC;
import common.SCC.Graph;
import syntax.Extent;
import syntax.Located;
import syntax.PExtent;
import syntax.PGrammar;
import syntax.PGrammarRule;
import syntax.PProduction;
import syntax.PProduction.ActualExpr;
import syntax.TokenDecl;
import unparam.Grammar.IllFormedException;

/**
 * This class deals with the process of <i>expanding</i> a {@linkplain PGrammar 
 * parametric grammar} into a {@linkplain Grammar ground one} by generating all
 * ground instances of rules from (monomorphic) public entry points.
 * <p>
 * <h2>Checking that the grammar is finitely expandable</h2>
 * <p>
 * This is potentially non-terminating if the rules interact in such a way that 
 * larger and larger instances keep being generated. The method {@link #checkExpandability(PGrammar)}
 * checks whether the expansion of a grammar is guaranteed to terminate or not, and throws
 * a {@link PGrammarNotExpandable} exception in the latter case. 
 * <p>
 * It proceeds by building the _expansion flow graph_ of the grammar, representing how the 
 * formal parameters of a rule can contribute to effective parameters of other generic rules
 * in the productions. Such contributions are <i>safe</i> if the the formal appears directly as 
 * the effective parameter, or <i>dangerous</i> if it appears deep in some rule expression.
 * <p>
 * The {@linkplain SCC strongly-connected components} of the expansion flow graph are 
 * then computed and the grammar is deemed safe for expansion if no SCC contains a dangerous
 * flow edge. Indeed, when dangerous edges are only found linking different SCCs, there is
 * no risk of an ever-growing cycle of rule instantiations occurring.
 * <p>
 * <i>NB: The expandability of the grammar is checked without taking into account the actual
 * 	rules that can be reached from the grammar's entry points. In other words, a grammar is
 *  only deemed expandable if it can be finitely expanded from <b>any</b> ground instantiation
 *  of its rules.
 * </i>
 * <h2>Expanding the parametric grammar</h2>
 * <p>
 * ...TODO doc
 * 
 * @author Stéphane Lescuyer
 */
public final class Expansion {

	/**
	 * Exception thrown when {@link Expansion#checkExpandability(PGrammar)}
	 * finds a potentially dangerous cycle preventing the expansion of a grammar.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class PGrammarNotExpandable extends Exception {
		private static final long serialVersionUID = 4186980925380975221L;
		
		/** The rule whose expansion may not terminate */
		public final Located<String> rule;
		/** 
		 * The formal parameter of {@link #rule} which may lead
		 * to ever-growing instantiations
		 */
		public final Located<String> formal;
		
		PGrammarNotExpandable(Formal formal) {
			super(String.format(
				"The parametric rule %s cannot be expanded because its parameter %s"
				+ " could grow infinitely.", formal.rule.val, formal.formal.val));
			this.rule = formal.rule;
			this.formal = formal.formal;
		}
	}
	
	/**
	 * A {@link Formal} represents one of the formal parameters of rules
	 * in a parametric grammar. It is characterized by its {@link #rule},
	 * its {@linkplain #formal name}, and its {@linkplain #formalIdx index}
	 * amongst the rule's parameters.
	 * <p>
	 * Additionnally, it has a unique {@link #order} number which is used
	 * to efficiently handle formals as nodes in {@linkplain ExpansionFlowGraph}.
	 * 
	 * @author Stéphane Lescuyer
	 */
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
	
	/**
	 * Describes the <i>expansion flow graph</i> of some parametric grammar.
	 * <p>
	 * The vertices of this graph are the {@linkplain Formal formals} of 
	 * the grammar, and there is an edge from the {@code i}-th parameter
	 * {@code ri} of some rule {@code r} to the {@code j}-th parameter 
	 * of some rule {@code s} if there is at least one production 
	 * item in {@code r} where {@code ri} appears in an expression
	 * passed as the {@code j}-th subterm of {@code s}. Such an
	 * edge is dangerous if and only if the subterm is not reduced
	 * to {@code ri} itself.  
	 * 
	 * @see Expansion
	 * @see #of
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class ExpansionFlowGraph implements Graph<Formal> {
		private final PGrammar grammar;
		
		/**
		 * Decorates an edge in the expansion flow graph 
		 * 
		 * @author Stéphane Lescuyer
		 */
		static final class Edge {
			// Whether the edge is safe or not
			private boolean safe;
			// The location of the actual which explains this edge
			@SuppressWarnings("unused")
			private Located<String> site;
			
			Edge(boolean safe, Located<String> site) {
				this.safe = safe;
				this.site = site;
			}
		}
		
		// For every _parametric_ rule in the grammar, the formals
		// which represents the rule's formal parameters, in order
		private final Map<Located<String>, @NonNull Formal[]> formals;
		// To every formal in the grammar, associates its successors
		// and the associated edge descriptors
		final Map<Formal, Map<Formal, Edge>> graph;
		
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
		
		/**
		 * @param grammar
		 * @return the expansion flow graph for the given parametric grammar
		 */
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

		/**
		 * Adds the contributions to the expansion flow graph that stem from
		 * some actual expression {@code aexpr} used in a production of a
		 * rule whose formal parameters are given by {@code paramToFormal}.
		 * <p>
		 * The {@code context} in which the actual expression appears in the
		 * production represents the path where the expression can be found
		 * starting from some top-level actual in the production. Each step
		 * down the term is represented by the corresponding <i>formal</i>:
		 * if the sub-expression is in the {@code n}-th subterm of some
		 * application of parametric rule {@code r}, this is described in
		 * the context by the {@link Formal} representing the {@code n}-th
		 * formal parameter of {@code r}.
		 * 
		 * @param paramToFormal
		 * @param context
		 * @param expr
		 */
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
	
	/**
	 * Checks that the given parametric grammar can be expanded
	 * in a finite fashion. Note that the check does not take the
	 * actual entry points of the grammar into account. In particular,
	 * the grammar may fail this check because of some rules which are
	 * declared and yet actually unused in the grammar.
	 * 
	 * @param grammar
	 * @throws PGrammarNotExpandable if expanding this grammar may not terminate
	 */
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
	
	/**
	 * @param rule		the name of the non-terminal rule
	 * @param params	can be empty, but must be ground
	 * 
	 * @return the generated name used to represent the <b>ground</b>
	 * 	instance of the non-terminal {@code rule} applied to {@code params}
	 */
	private static String ruleName(String rule, List<ActualExpr> params) {
		if (params.isEmpty()) return rule;
		StringBuilder buf = new StringBuilder();
		buf.append(rule).append('<');
		boolean first = true;
		for (ActualExpr ae : params) {
			if (first) first = false;
			else buf.append(", ");
			buf.append(ruleName(ae.symb.val, ae.params));
		}
		buf.append('>');
		return buf.toString();
	}
	
	/**
	 * This container class represents a ground instance of
	 * some {@linkplain #nterm non-terminal} applied to some
	 * {@linkplain #params effective parameters}. 
	 * <p
	 * This represents a unit of pending work during the 
	 * expansion of a parametric grammar. The field {@link #name}
	 * gives the actual name of the corresponding instance
	 * in the expanded grammar.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Instance {
		final String nterm;
		final List<ActualExpr> params;
		final String name;
		
		Instance(String nterm, List<ActualExpr> params) {
			this.nterm = nterm;
			this.params = params;
			this.name = ruleName(nterm, params);
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	private final PGrammar pgrammar;

	// Ground instantiations of rules in {@code pgrammar} which may still
	// need to be generated
	private final Stack<Instance> todo;
	
	// Ground instances of rules in {@code pgrammar} which have already
	// been generated, mapped to their name for convenience
	private final Map<String, GrammarRule> generatedRules;
	
	// Cache associating some _ground_ actual expressions to an extent
	// representing the return type (or value type for expressions reduced
	// to terminals). The empty option is used to denote the fact that
	// the expression is not valued.
	private final Map<ActualExpr, Optional<Extent>> returnTypes;
	
	/**
	 * Must only be used on a grammar which has passed {@link #checkExpandability(PGrammar)}
	 * successfully.
	 * 
	 * @param pgrammar
	 */
	private Expansion(PGrammar pgrammar) {
		this.pgrammar = pgrammar;
		this.todo = new Stack<>();
		this.generatedRules = new HashMap<>();
		this.returnTypes = new HashMap<>();
	}
	
	/**
	 * This performs the expansion (aka <i>monomorphization</i>) of the parametric
	 * grammar {@code pgrammar}, which must have been {@linkplain #checkExpandability(PGrammar) 
	 * checked} to be expandable beforehand.
	 * <p> 
	 * The result is a non-parametric {@link Grammar} whose public entry points 
	 * are the same as {@code pgrammar} and must parse the same language.
	 * 
	 * @param pgrammar
	 * @return the result of expanding {@code pgrammar}
	 * 
	 * @throws IllFormedException if the expanded grammar happens to be ill-formed
	 */
	public static Grammar of(PGrammar pgrammar) {
		Expansion exp = new Expansion(pgrammar);
		exp.realize();
		Grammar.Builder builder = new Grammar.Builder(
			pgrammar.options, pgrammar.imports, pgrammar.header, pgrammar.footer);
		pgrammar.tokenDecls.forEach(builder::addToken);
		// Try and preserve the original relative order between rules by adding
		// rules in the order of the original rule they originate from
		for (String ruleName : pgrammar.rules.keySet()) {
			for (GrammarRule rule : exp.generatedRules.values()) {
				if (ruleName.equals(rule.name.val)
					|| rule.name.val.startsWith(ruleName + "<"))
					builder.addRule(rule);
			}
		}
		// exp.generatedRules.values().forEach(builder::addRule);
		return builder.build();
	}

	private void realize() {
		// Find all public rules, which must be ground, and add them to the stack
		// of required ground instances
		for (PGrammarRule rule : pgrammar.rules.values()) {
			if (!rule.visibility) continue;
			todo.add(new Instance(rule.name.val, Lists.empty()));
		}
		
		// Now realize all pending instances, until there are no more.
		// This must terminate by virtue of the expandability check
		while (!todo.isEmpty()) {
			Instance pending = todo.pop();
			
			// Check if this instantiation has already been generated
			if (generatedRules.containsKey(pending.name)) continue;
			
			// Otherwise, find the parametric rule to apply and realize
			// the instance. This may result in adding new instances to
			// the pending stack.
			realizeRule(pending.name, pgrammar.rule(pending.nterm), pending.params);
		}
	}
	
	/**
	 * This generates the ground instance of {@code rule} applied to
	 * (ground) effective parameters {@link effective}, and records
	 * it with the given {@code ruleName}.
	 * 
	 * @param ruleName
	 * @param prule
	 * @param effective
	 * @return the ground rule corresponding to the given instantiation problem
	 */
	private GrammarRule realizeRule(String ruleName, 
			PGrammarRule prule, List<ActualExpr> effective) {
		// This is a new instantiation that we must perform
		Map<String, Extent> replacements = replacementMap(prule, effective);
		Extent ruleReturn = prule.returnType.instantiate(replacements);
		@Nullable PExtent pruleArgs = prule.args;
		@Nullable Extent ruleArgs =
			pruleArgs == null ? null : pruleArgs.instantiate(replacements);
		Map<String, ActualExpr> pinst = new LinkedHashMap<>(effective.size());
		for (int i = 0; i < effective.size(); ++i)
			pinst.put(prule.params.get(i).val, effective.get(i));
		
		List<Production> productions = new ArrayList<>(prule.productions.size());
		for (PProduction pprod : prule.productions)
			productions.add(realizeProduction(pprod, pinst, replacements));
		
		GrammarRule rule = 
			new GrammarRule(prule.visibility, ruleReturn, 
				Located.like(ruleName, prule.name), ruleArgs, productions);
		generatedRules.put(ruleName, rule);
		return rule;
	}
	
	/**
	 * @param pprod
	 * @param pinst
	 * @param replacements
	 * @return the ground production corresponding to the given, potentially
	 * 	parametric, production where formal parameters are instantiated to
	 * 	ground expressions as given by {@code pinst}, and replacements for
	 *  holes in {@link PExtent}s is given by {@code replacements}
	 */
	private Production realizeProduction(PProduction pprod, 
			Map<String, ActualExpr> pinst, Map<String, Extent> replacements) {
		List<Production.Item> items = new ArrayList<>(pprod.items.size());
		for (PProduction.Item pitem : pprod.items) {
			switch (pitem.getKind()) {
			case ACTION:
				PProduction.ActionItem action = (PProduction.ActionItem) pitem;
				Extent extent = action.extent.instantiate(replacements);
				items.add(new Production.ActionItem(extent));
				continue;
			case ACTUAL:
				PProduction.Actual actual = (PProduction.Actual) pitem;
				items.add(realizeActual(actual, pinst, replacements));
				continue;
			case CONTINUE:
				PProduction.Continue cont = (PProduction.Continue) pitem;
				items.add(new Production.Continue(cont.cont));
				continue;
			}
		}
		return new Production(items);
	}

	/**
	 * As a side-effect, this may record a new ground instance into the stack
	 * of pending instances, in case the given production item is the result of
	 * applying some parametric non-terminal.
	 * 
	 * @param pactual
	 * @param pinst
	 * @param replacements
	 * @return the ground production item corresponding to the given, potentially
	 * 	parametric, actual where formal parameters are instantiated to
	 * 	ground expressions as given by {@code pinst}, and replacements for
	 *  holes in {@link PExtent}s is given by {@code replacements}
	 */
	private Production.Actual realizeActual(PProduction.Actual pactual, 
			Map<String, ActualExpr> pinst, Map<String, Extent> replacements) {
		// Apply the instantiation to the actual to find a ground actual expression
		ActualExpr instExpr = instantiateItem(pactual.item, pinst);
		// If the resulting expression is an application, register it as a new
		// required instance
		String itemName;
		if (!instExpr.isTerminal()) {
			Instance newInstance = new Instance(instExpr.symb.val, instExpr.params);
			if (!generatedRules.containsKey(newInstance.name))
				todo.add(newInstance);
			itemName = newInstance.name;
		}
		else
			itemName = instExpr.symb.val;
		
		@Nullable PExtent pargs = pactual.args;
		@Nullable Extent args = pargs == null ? null : pargs.instantiate(replacements);
		return new Production.Actual(pactual.binding, 
				Located.like(itemName, pactual.item.symb), args);
	}
	
	/**
	 * @param aexpr
	 * @param pinst
	 * @return the result of substituting all formals following {@code pinst}
	 * 	in the actual expression {@code aexpr}
	 */
	private ActualExpr instantiateItem(ActualExpr aexpr, Map<String, ActualExpr> pinst) {
		// If the symbol is a terminal, it needs no instantiation
		if (aexpr.isTerminal()) return aexpr;
		// If the symbol is a formal, we replace it with its image 
		// in the given substitution
		@Nullable ActualExpr img = pinst.get(aexpr.symb.val);
		if (img != null) return img;
		// Otherwise, the symbol is a non-terminal, not necessarily parametric
		if (aexpr.params.isEmpty()) return aexpr;
		List<ActualExpr> params = new ArrayList<>(aexpr.params.size());
		for (ActualExpr eparam : aexpr.params)
			params.add(instantiateItem(eparam, pinst));
		return new ActualExpr(aexpr.symb, params);
	}
	

	/**
	 * The effective parameters in {@code effective} must be <i>ground</i>. The returned
	 * replacement map is suitable to instantiate {@linkplain PExtent parameterized extents}
	 * that appear in {@code prule}.
	 * 
	 * @param prule
	 * @param effective
	 * @return a map associating replacements for all formals in {@code prule} which
	 * 	happen to represent valued expressions, when the rule is applied to the 
	 *  <i>ground</i> effective parameters {@code effective}
	 */
	private Map<String, Extent> replacementMap(PGrammarRule prule, List<ActualExpr> effective) {
		if (prule.params.size() != effective.size())
			throw new IllegalArgumentException();
		if (prule.params.isEmpty())
			return Maps.empty();
		Map<String, Extent> replacements = new HashMap<>();
		for (int i = 0; i < prule.params.size(); ++i) {
			@Nullable Extent ext = returnType(effective.get(i));
			if (ext != null)
				replacements.put(prule.params.get(i).val, ext);
		}
		return replacements;
	}

	/**
	 * The actual expression {@code aexpr} must be <i>ground</i>. The returned
	 * extent is suitable to build {@link #replacementMap(PGrammarRule, List) replacements}
	 * for holes in parameterized extents.
	 * 
	 * @param aexpr
	 * @return an extent that represents the return type associated to the
	 * 	given expression {@code expr}, or {@code null} if this expression is
	 *  not valued (which can for now only happens with expressions reduced
	 *  to a non-valued terminal symbol)
	 */
	private @Nullable Extent returnType(ActualExpr aexpr) {
		@Nullable Optional<Extent> cached = returnTypes.get(aexpr);
		if (cached != null)
			return cached.orElse(null);
		// Compute the return type for the given ground expression 
		// and record it in the cache
		@Nullable Extent res;
		String sym = aexpr.symb.val;
		if (aexpr.isTerminal()) {
			// The return type is either that of the token,
			// or {@code null} if the token is not valued
			Optional<TokenDecl> tdo = 
				pgrammar.tokenDecls.stream().filter(td -> td.name.val.equals(sym)).findFirst();
			if (!tdo.isPresent())
				throw new IllegalStateException("Unknown terminal \"" + 
						sym + "\" in actual expression " + aexpr);
			res = tdo.get().valueType;
		}
		else {
			// The expression must be ground, so the symbol must be a non-terminal
			PGrammarRule prule = pgrammar.rule(sym);
			// If it is an application, we must fetch the return types
			// of the parameters first. Hoping that all formals which appear
			// in holes are actually valued.
			Map<String, Extent> replacements = replacementMap(prule, aexpr.params);
			res = prule.returnType.instantiate(replacements);
		}
		returnTypes.put(aexpr, Optional.ofNullable(res));
		return res;
	}
}
