package test.grammar;

import org.eclipse.jdt.annotation.NonNull;

import common.Lists;
import syntax.Located;
import syntax.Extent;
import syntax.TokenDecl;
import unparam.Grammar;
import unparam.GrammarRule;
import unparam.Grammars;
import unparam.Production;
import unparam.Grammars.Dependencies;
import unparam.Grammars.NTermsInfo;
import unparam.Grammars.PredictionTable;

/**
 * A few manual tests of {@link Grammars#analyseGrammar}.
 * TODO: make a generator and an automatic test?
 * 
 * @author Stéphane Lescuyer
 */
public final class TestAnalyseGrammar {

	static TokenDecl token(String name) {
		return new TokenDecl(Located.dummy(name), null);
	}
	
	static TokenDecl vtoken(String name, String valType) {
		return new TokenDecl(Located.dummy(name), Extent.inlined(valType));
	}
	
	static Production.Actual actual(String s) {
		return new Production.Actual(null, Located.dummy(s), null);
	}
	
	static final Extent VOID = Extent.inlined("void");
	static final Extent RETURN = Extent.inlined("return;");
	
	static Production production(@NonNull String... items) {
		Production.Builder builder = new Production.Builder();
		for (int i = 0; i < items.length; ++i)
			builder.addActual(actual(items[i]));
		builder.addAction(RETURN);
		return builder.build();
	}
	
	static GrammarRule rule(String name,
		@NonNull Production... productions) {
		GrammarRule.Builder builder =
			new GrammarRule.Builder(false, VOID, Located.dummy(name), null);
		for (int i = 0; i < productions.length; ++i)
			builder.addProduction(productions[i]);
		return builder.build();
	}

	static GrammarRule prule(String name,
		@NonNull Production... productions) {
		GrammarRule.Builder builder =
			new GrammarRule.Builder(true, VOID, Located.dummy(name), null);
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
	final static class Test1 {
		
		private final static GrammarRule ruleZ =
			new GrammarRule.Builder(true, VOID, Located.dummy("z"), null)
				.addProduction(production("D"))
				.addProduction(production("x", "y", "z"))
				.build();
		
		private final static GrammarRule ruleY =
			new GrammarRule.Builder(false, VOID, Located.dummy("y"), null)
				.addProduction(production())
				.addProduction(production("C"))
				.build();
		
		private final static GrammarRule ruleX =
			new GrammarRule.Builder(false, VOID, Located.dummy("x"), null)
				.addProduction(production("y"))
				.addProduction(production("A"))
				.build();
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Lists.empty(), Extent.DUMMY, Extent.DUMMY)
				.addToken(token("A")).addToken(token("C")).addToken(token("D"))
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
	final static class Test2 {

		private final static GrammarRule ruleS =
				new GrammarRule.Builder(true, VOID, Located.dummy("s"), null)
					.addProduction(production("e", "EOF"))
					.build();

		private final static GrammarRule ruleE =
			new GrammarRule.Builder(false, VOID, Located.dummy("e"), null)
				.addProduction(production("t", "e_"))
				.build();
			
		private final static GrammarRule ruleE_ =
			new GrammarRule.Builder(false, VOID, Located.dummy("e_"), null)
				.addProduction(production("PLUS", "t", "e_"))
				.addProduction(production("MINUS", "t", "e_"))
				.addProduction(production())
				.build();
		
		private final static GrammarRule ruleT =
			new GrammarRule.Builder(false, VOID, Located.dummy("t"), null)
				.addProduction(production("f", "t_"))
				.build();
		
		private final static GrammarRule ruleT_ =
			new GrammarRule.Builder(false, VOID, Located.dummy("t_"), null)
				.addProduction(production("MULT", "f", "t_"))
				.addProduction(production("DIV", "f", "t_"))
				.addProduction(production())
				.build();
		
		private final static GrammarRule ruleF =
			new GrammarRule.Builder(false, VOID, Located.dummy("f"), null)
				.addProduction(production("ID"))
				.addProduction(production("NUM"))
				.addProduction(production("LPAREN", "e", "RPAREN"))
				.build();
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Lists.empty(), Extent.DUMMY, Extent.DUMMY)
				.addToken(token("PLUS")).addToken(token("MINUS"))
				.addToken(token("MULT")).addToken(token("DIV"))
				.addToken(token("EOF")).addToken(token("LPAREN")).addToken(token("RPAREN"))
				.addToken(vtoken("ID", "String")).addToken(vtoken("NUM", "int"))
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
	 *   start -> s EOF
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
	final static class TestLatex {
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Lists.empty(), Extent.DUMMY, Extent.DUMMY)
				.addToken(token("SLASH")).addToken(token("BEGIN")).addToken(token("END"))
				.addToken(token("LBRACE")).addToken(token("RBRACE"))
				.addToken(vtoken("WORD", "String")).addToken(token("EOF"))
				.addRule(prule("start", production("s", "EOF")))
				.addRule(rule("s",
							production(),
							production("x", "s")))
				.addRule(rule("b",
							production("SLASH", "BEGIN", "LBRACE", "WORD", "RBRACE")))
				.addRule(rule("e",
							production("SLASH", "END", "LBRACE", "WORD", "RBRACE")))
				.addRule(rule("x",
							production("b", "s", "e"),
							production("LBRACE", "s", "RBRACE"),
							production("WORD"),
							production("BEGIN"),
							production("END"),
							production("SLASH", "WORD")))
				.build();
	}
	
	/**
	 * A simplistic Latex grammar which enforces well-formed
	 * {@code \begin}/{@code \end} environments and bracket blocks,
	 * and is LL(1).
	 * 
	 * <pre>
	 *   start -> s EOF
	 *   
	 *   s ->
	 *   s -> SLASH cmd
	 *   s -> x s
	 *   
	 *   cmd -> b s_env
	 *   cmd -> WORD s
	 *	
	 *	 s_env -> SLASH s_env_cmd
	 *	 s_env -> x s_env
	 *
	 *	 s_env_cmd -> WORD s_env
	 *   s_env_cmd -> e
	 *
	 *   b -> BEGIN LBRACE WORD RBRACE
	 *   e -> END LBRACE WORD RBRACE
	 *   
	 *   x -> WORD
	 *   x -> BEGIN
	 *   x -> END
	 *   x -> LBRACE s RBRACE
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	final static class TestLatexLL1 {
		
		final static Grammar grammar =
			new Grammar.Builder(Lists.empty(), Lists.empty(), Extent.DUMMY, Extent.DUMMY)
				.addToken(token("SLASH")).addToken(token("BEGIN")).addToken(token("END"))
				.addToken(token("LBRACE")).addToken(token("RBRACE"))
				.addToken(vtoken("WORD", "String")).addToken(token("EOF"))
				.addRule(prule("start", production("s", "EOF")))
				.addRule(rule("s",
							production(),
							production("SLASH", "cmd"),
							production("x", "s")))
				.addRule(rule("cmd",
							production("b", "s_env"),
							production("WORD", "s")))
				.addRule(rule("s_env",
							production("SLASH", "s_env_cmd"),
							production("x", "s_env")))
				.addRule(rule("s_env_cmd",
							production("WORD", "s_env"),
							production("e")))
				.addRule(rule("b",
							production("BEGIN", "LBRACE", "WORD", "RBRACE")))
				.addRule(rule("e",
							production("END", "LBRACE", "WORD", "RBRACE")))
				.addRule(rule("x",
							production("LBRACE", "s", "RBRACE"),
							production("WORD"),
							production("BEGIN"),
							production("END")))
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
		NTermsInfo info = Grammars.analyseGrammar(grammar, deps, null);
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
		test(TestLatexLL1.grammar);
	}

}
