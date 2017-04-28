package test.grammar;

import org.eclipse.jdt.annotation.NonNull;

import common.Lists;
import syntax.Grammar;
import syntax.GrammarRule;
import syntax.Grammars;
import syntax.Location;
import syntax.Production;
import syntax.Grammars.Dependencies;
import syntax.Grammars.NTermsInfo;
import syntax.Grammars.PredictionTable;

/**
 * A few manual tests of {@link Grammars#analyseGrammar}.
 * TODO: make a generator and an automatic test?
 * 
 * @author Stéphane Lescuyer
 */
public final class TestAnalyseGrammar {

	private static Production.Item item(String s) {
		return new Production.Item(null, s);
	}
	
	private static Production production(@NonNull String... items) {
		Production.Builder builder = new Production.Builder(Location.DUMMY);
		for (int i = 0; i < items.length; ++i)
			builder.addItem(item(items[i]));
		return builder.build();
	}
	
	private static GrammarRule rule(String name, 
		@NonNull Production... productions) {
		GrammarRule.Builder builder = new GrammarRule.Builder(false, Location.DUMMY, name, Location.DUMMY);
		for (int i = 0; i < productions.length; ++i)
			builder.addProduction(productions[i]);
		return builder.build();
	}
	
	/**
	 * (An ambiguous grammar)
	 * 
	 * <pre>
	 *   z -> D
	 *   z -> x y z
	 * </pre>
	 * <pre>
	 *   y ->
	 *   y -> C
	 * </pre>
	 * <pre>
	 *   x -> y
	 *   x -> A
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private final static class Test1 {
		
		private final static GrammarRule ruleZ =
			new GrammarRule.Builder(true, Location.DUMMY, "z", Location.DUMMY)
				.addProduction(production("D"))
				.addProduction(production("x", "y", "z"))
				.build();
		
		private final static GrammarRule ruleY =
			new GrammarRule.Builder(false, Location.DUMMY, "y", Location.DUMMY)
				.addProduction(production())
				.addProduction(production("C"))
				.build();
		
		private final static GrammarRule ruleX =
			new GrammarRule.Builder(false, Location.DUMMY, "x", Location.DUMMY)
				.addProduction(production("y"))
				.addProduction(production("A"))
				.build();
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Location.DUMMY, Location.DUMMY)
				.addRule(ruleZ).addRule(ruleY).addRule(ruleX).build();
	}
	
	/**
	 * A nice LL(1) arithmetic expression grammar
	 * 
	 * <pre>
	 *   s -> e EOF
	 *   
	 *   e -> t e'
	 *   
	 *   e' -> PLUS t e'
	 *   e' -> MINUS t e'
	 *   e' ->
	 *   
	 *   t -> f t'
	 *   
	 *   t' -> MULT f t'
	 *   t' -> DIV f t'
	 *   t' ->
	 *   
	 *   f -> ID
	 *   f -> NUM
	 *   f -> LPAREN e RPAREN
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private final static class Test2 {

		private final static GrammarRule ruleS =
				new GrammarRule.Builder(false, Location.DUMMY, "s", Location.DUMMY)
					.addProduction(production("e", "EOF"))
					.build();

		private final static GrammarRule ruleE =
			new GrammarRule.Builder(false, Location.DUMMY, "e", Location.DUMMY)
				.addProduction(production("t", "e_"))
				.build();
			
		private final static GrammarRule ruleE_ =
			new GrammarRule.Builder(false, Location.DUMMY, "e_", Location.DUMMY)
				.addProduction(production("PLUS", "t", "e_"))
				.addProduction(production("MINUS", "t", "e_"))
				.addProduction(production())
				.build();
		
		private final static GrammarRule ruleT =
			new GrammarRule.Builder(false, Location.DUMMY, "t", Location.DUMMY)
				.addProduction(production("f", "t_"))
				.build();
		
		private final static GrammarRule ruleT_ =
			new GrammarRule.Builder(false, Location.DUMMY, "t_", Location.DUMMY)
				.addProduction(production("MULT", "f", "t_"))
				.addProduction(production("DIV", "f", "t_"))
				.addProduction(production())
				.build();
		
		private final static GrammarRule ruleF =
			new GrammarRule.Builder(false, Location.DUMMY, "f", Location.DUMMY)
				.addProduction(production("ID"))
				.addProduction(production("NUM"))
				.addProduction(production("LPAREN", "e", "RPAREN"))
				.build();
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Location.DUMMY, Location.DUMMY)
				.addRule(ruleF).addRule(ruleT_).addRule(ruleT)
				.addRule(ruleE_).addRule(ruleE).addRule(ruleS)
				.build();
	}
	
	/**
	 * A simplistic Latex grammar which enforces well-formed
	 * {@code \begin}/{@code \end} environments and bracket blocks,
	 * but is not LL(1).
	 * 
	 * <pre>
	 *   s' -> s EOF
	 *   
	 *   s ->
	 *   s -> x s
	 *   
	 *   b -> SLASH BEGIN LBRACE WORD RBRACE
	 *   e -> SLASH END LBRACE WORD RBRACE
	 *   
	 *   x -> b s e
	 *   x -> LBRACE s RBRACE
	 *   x -> WORD
	 *   x -> BEGIN
	 *   x -> END
	 *   x -> SLASH WORD
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private final static class TestLatex {
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Location.DUMMY, Location.DUMMY)
				.addRule(rule("s'", production("s", "EOF")))
				.addRule(rule("s",
							production(),
							production("x", "s")))
				.addRule(rule("b",
							production("BEGIN", "LBRACE", "WORD", "RBRACE")))
				.addRule(rule("e",
							production("SLASH", "END", "LBRACE", "WORD", "RBRACE")))
				.addRule(rule("cmd",
							production("b", "s", "e"),
							production("WORD")))
				.addRule(rule("x",
							production("LBRACE", "s", "RBRACE"),
							production("WORD"),
							production("BEGIN"),
							production("END"),
							production("SLASH", "cmd")))
				.build();
	}
	
	/**
	 * Computes analyses on the given {@code grammar}
	 * and displays them
	 * 
	 * @param grammar
	 */
	private static void test(Grammar grammar) {
		Dependencies deps = Grammars.dependencies(grammar);
		NTermsInfo info = Grammars.analyseGrammar(grammar, deps);
		System.out.println(" ========================== \n");
		System.out.println(deps);
		System.out.println(info);
		System.out.println("------Prediction Table----- \n");
		PredictionTable table = Grammars.predictionTable(grammar, info);
		System.out.println(table);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test(Test1.grammar);
		test(Test2.grammar);
		test(TestLatex.grammar);
	}

}
