package test.examples;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
import codegen.GrammarOutput;
import common.Lists;
import jl.JLLexerGenerated;
import jl.JLParser;
import syntax.Grammar;
import syntax.GrammarRule;
import syntax.Grammars;
import syntax.Grammars.NTermsInfo;
import syntax.Grammars.PredictionTable;
import syntax.Lexer;
import syntax.Location;
import syntax.Production;

/**
 * Some manually defined grammars, with non-dummy
 * semantic actions, used to test generated parsers
 * 
 * @author Stéphane Lescuyer
 */
public abstract class BasicGrammars {

	private BasicGrammars() {
		// Static utility only
	}

	static Grammar.TokenDecl token(String name) {
		return new Grammar.TokenDecl(name, null);
	}
	
	static Grammar.TokenDecl vtoken(String name, String valType) {
		return new Grammar.TokenDecl(name, Location.inlined(valType));
	}
	
	static Production.Actual actual(String s) {
		int ndx = s.indexOf('=');
		if (ndx < 0)
			return new Production.Actual(null, s);
		String binding = s.substring(0, ndx).trim();
		@SuppressWarnings("null")
		@NonNull String item = s.substring(ndx + 1).trim();
		return new Production.Actual(binding, item);
	}
	
	static final Location VOID = Location.inlined("void");
	static final Location RETURN = Location.inlined("return;");
	
	static Production production(@NonNull String... actuals) {
		Location action = Location.inlined(actuals[actuals.length - 1]);
		Production.Builder builder = new Production.Builder();
		for (int i = 0; i < actuals.length - 1; ++i)
			builder.addItem(actual(actuals[i]));
		builder.addAction(action);
		return builder.build();
	}
	
	private static GrammarRule vrule(boolean vis,
		String name, @NonNull Object... productions) {
		Location retType = VOID;
		int first = 0;
		if (productions[0] instanceof String) {
			retType = Location.inlined((String) productions[0]);
			++first;
		}
		GrammarRule.Builder builder = new GrammarRule.Builder(vis, retType, name, null);
		for (int i = first; i < productions.length; ++i)
			builder.addProduction((Production) productions[i]);
		return builder.build();		
	}
	
	static GrammarRule rule(String name, @NonNull Object... productions) {
		return vrule(false, name, productions);
	}

	static GrammarRule prule(String name, @NonNull Object... productions) {
		return vrule(true, name, productions);
	}
	
	/**
	 * Ground atomic expressions, without parentheses,
	 * with integer literals, addition, substraction,
	 * negation, multiplication and semantic actions which
	 * compute the result of the expression.
	 * 
	 * <pre>
	 * start -> exp EOF
	 * 
	 * exp -> factor exp_rhs
	 * exp_rhs -> PLUS exp
	 * exp_rhs -> MINUS exp
	 * exp_rhs ->
	 * 
	 * factor -> atomic factor_rhs
	 * factor_rhs -> TIMES factor
	 * factor_rhs ->
	 * 
	 * atomic -> MINUS atomic
	 * atomic -> INT
	 * </pre>
	 * 
	 * The lexer description for this parser is
	 * in the `tests/jl/ArithGroundLexer.jl` file.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class ArithGround {
		/**
		 * The name of the file containing 
		 * the lexer description for this grammar
		 */
		public static final String LEXER = "tests/jl/ArithGroundLexer.jl";
		
		private static final String footer =
				"/**\n" +
			"     * Testing this parser\n" +
			"     */\n" +
			"    public static void main(String[] args) throws java.io.IOException {\n" +
			"		String prompt;\n" +
			"       while ((prompt = common.Prompt.getInputLine(\">\")) != null) {\n" +
			"			try {\n" +
			"				ArithGroundLexer lexer = new ArithGroundLexer(\"-\",\n" +
			"					new java.io.StringReader(prompt));\n" +
			"				ArithGroundParser parser = new ArithGroundParser(lexer::main);\n" +
			"				int e = parser.exp();\n" +
			"				System.out.println(e);\n" +
			"			} catch (ParsingException e) {\n" +
			"				e.printStackTrace();\n" +
			"			}\n" +
			"		}\n" +
			"	}\n";
		
		/**
		 * The grammar description for ground
		 * arithmetic expressions
		 * @see ArithGround
		 */
		public static final Grammar GRAMMAR =
			new Grammar.Builder(
				Lists.empty(), Location.DUMMY, Location.inlined(footer))
				// INT of int | PLUS | MINUS | TIMES | EOF
				.addToken(vtoken("INT", "int"))
				.addToken(token("PLUS"))
				.addToken(token("MINUS"))
				.addToken(token("TIMES"))
				.addToken(token("EOF"))
				
				.addRule(prule("start", "int",
					production("n = exp", "EOF", "return n;")))
				// exp
				.addRule(rule("exp", "int",
					production("n1 = factor", "n2 = exp_rhs", "return n1 + n2;")))
				.addRule(rule("exp_rhs", "int",
					production("PLUS", "n = exp", "return n;"),
					production("MINUS", "n = exp", "return -n;"),
					production("return 0;")))
				// factor
				.addRule(rule("factor", "int",
					production("n1 = atomic", "n2 = factor_rhs", "return n1 * n2;")))
				.addRule(rule("factor_rhs", "int",
					production("TIMES", "n = factor", "return n;"),
					production("return 1;")))
				// atomic
				.addRule(rule("atomic", "int",
					production("MINUS", "n = atomic", "return -n;"),
					production("n = INT", "return n;")))
				.build();
	}
	
	/**
	 * Straight-line arithmetic programs with
	 * variables, statements and printing, which
	 * can be interpreted in semantic actions.
	 * 
	 * <pre>
	 * program -> stmts EOF
	 * 
	 * stmts -> stmt stmts_rhs
	 * stmts_rhs ->
	 * stmts_rhs -> SEMICOLON stmts
	 * 
	 * stmt -> ID ASSIGN expr
	 * stmt -> PRINT LPAREN pexpr pexprs RPAREN
	 * 
	 * pexprs -> 
	 * pexprs -> COMMA pexpr pexprs
	 * pexpr -> expr
	 * 
	 * expr -> term
	 * // expr -> stmts COMMA expr
	 * 
	 * term -> factor term_rhs
	 * term_rhs -> PLUS term
	 * term_rhs -> MINUS term
	 * term_rhs ->
	 * 
	 * factor -> atomic factor_rhs
	 * factor_rhs -> TIMES factor
	 * factor_rhs -> DIV factor
	 * factor_rhs ->
	 * 
	 * atomic -> INT
	 * atomic -> ID
	 * atomic -> LPAREN expr RPAREN
	 * </pre>
	 * 
	 * The lexer description for this parser is
	 * in the `tests/jl/StraightLineLexer.jl` file.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class StraightLineProgram {
		/**
		 * The name of the file containing 
		 * the lexer description for this grammar
		 */
		public static final String LEXER = "tests/jl/StraightLineLexer.jl";
		
		private static final String header =
				"/**\n" +
			"     * Packs a boolean and an integer\n" +
			"     */\n" +
			"    private static final class OpInt {\n" +
			"        final boolean mult;\n" +
			"        final int val;\n" +
			"        \n" +
			"        OpInt(boolean mult, int val) {\n" +
			"            this.mult = mult; this.val = val;\n" +
			"        }\n" +
			"    }\n" +
			"    \n" +
			"    private java.util.Map<String, Integer> env =\n" +
			"        new java.util.HashMap<>();\n" +
			"    \n" +
			"    private void update(String id, int n) {\n" +
			"        env.put(id, n);\n" +
			"    }\n" +
			"    \n" +
			"    private int lookup(String id) {\n" +
			"        if (!env.containsKey(id))\n" +
			"            throw new ParsingException(\"Undefined identifier: \" + id);\n" +
			"        return env.get(id);\n" +
			"    }\n" +
			"\n";
		
		private static final String footer =
				"/**\n" +
			"     * Testing this parser\n" +
			"     */\n" +
			"    public static void main(String[] args) throws java.io.IOException {\n" +
			"		String prompt;\n" +
			"       while ((prompt = common.Prompt.getInputLine(\">\")) != null) {\n" +
			"			try {\n" +
			"				StraightLineLexer lexer = new StraightLineLexer(\"-\",\n" +
			"					new java.io.StringReader(prompt));\n" +
			"				StraightLineParser parser = new StraightLineParser(lexer::main);\n" +
			"				parser.program();\n" +
			"			} catch (ParsingException e) {\n" +
			"				e.printStackTrace();\n" +
			"			}\n" +
			"		}\n" +
			"	}\n";
		
		/**
		 * The grammar description for straight-line programs
		 * 
		 * @see StraightLineProgram
		 */
		public static final Grammar GRAMMAR =
			new Grammar.Builder(
				Lists.empty(), Location.inlined(header), Location.inlined(footer))
				// INT of int | ID of string | PLUS | MINUS | TIMES | DIV | 
				// 	SEMICOLON | ASSIGN | PRINT | LPAREN | RPAREN | COMMA | EOF
				.addToken(vtoken("INT", "int"))
				.addToken(vtoken("ID", "String"))
				.addToken(token("PLUS"))
				.addToken(token("MINUS"))
				.addToken(token("TIMES"))
				.addToken(token("DIV"))
				.addToken(token("SEMICOLON"))
				.addToken(token("ASSIGN"))
				.addToken(token("PRINT"))
				.addToken(token("LPAREN"))
				.addToken(token("RPAREN"))
				.addToken(token("COMMA"))
				.addToken(token("EOF"))
				
				.addRule(prule("program", "void",
					production("stmts", "EOF", "return;")))
				// stmts
				.addRule(rule("stmts", "void",
					production("stmt", "stmts_rhs", "return;")))
				.addRule(rule("stmts_rhs", "void",
					production("return;"),
					production("SEMICOLON", "stmts", "return;")))
				.addRule(rule("stmt", "void",
					production("id = ID", "ASSIGN", "n = expr", "update(id, n); return;"),
					production("PRINT", "LPAREN", "pexpr", "pexprs", "RPAREN", "return;")))
				// pexprs
				.addRule(rule("pexprs", "void",
					production("return;"),
					production("COMMA", "pexpr", "pexprs", "return;")))
				.addRule(rule("pexpr", "void",
					production("e = expr", "System.out.println(e); return;")))
				// expr
				.addRule(rule("expr", "int",
					production("n = term", "return n;")))
					// production("stmts", "COMMA", "n = expr", "return n;")))
				// term
				.addRule(rule("term", "int",
					production("n1 = factor", "n2 = term_rhs", "return n1 + n2;")))
				.addRule(rule("term_rhs", "int",
					production("PLUS", "n = term", "return n;"),
					production("MINUS", "n = term", "return -n;"),
					production("return 0;")))
				// factor
				.addRule(rule("factor", "int",
					production("n1 = atomic", "op_n2 = factor_rhs",
						"return op_n2.mult ? n1 * op_n2.val : n1 / op_n2.val;")))
				.addRule(rule("factor_rhs", "OpInt",
					production("TIMES", "n = factor", "return new OpInt(true, n);"),
					production("DIV", "n = factor", "return new OpInt(false, n);"),
					production("return new OpInt(true, 1);")))
				// atomic
				.addRule(rule("atomic", "int",
					production("id = ID", "return lookup(id);"),
					production("n = INT", "return n;"),
					production("LPAREN", "e = expr", "RPAREN", "return e;")))
				.build();
	}
	
	private static void printConflicts(PredictionTable table) {
		System.out.println(table.toString());
	}
	
	@SuppressWarnings("unused")
	private static void testOutput(String className, Grammar grammar) {
		File file = new File("src-gen/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			NTermsInfo infos = Grammars.analyseGrammar(grammar, null);
			PredictionTable predictTable = Grammars.predictionTable(grammar, infos);
			if (!predictTable.isLL1()) {
				System.out.println("Cannot generate parser for non-LL(1) grammar:");
				printConflicts(predictTable);
				return;
			}
			GrammarOutput.output(writer, className, grammar, predictTable);
			System.out.println("----------JAVA--------");
			System.out.println("Generated in " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void generateLexer(String filename, String className) throws IOException {
		System.out.println("Parsing lexer description " + filename + "...");
		FileReader reader = new FileReader(filename);
		JLLexerGenerated lexer = new JLLexerGenerated(filename, reader);
		@SuppressWarnings("null")
		JLParser parser = new JLParser(lexer::main);
		Lexer lexerDef = parser.parseLexer();
		reader.close();
		System.out.println("Computing automata...");
		Automata aut = Determinize.lexer(lexerDef, true);
		File file = new File("src/test/examples/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package test.examples;\n");
			AutomataOutput.output(writer, className, aut);
		}
		System.out.println("Generated in " + file.getAbsolutePath());
	}
	
	static void generateParser(String className, Grammar grammar)
			throws IOException {
		System.out.println("Analysing grammar description...");
		NTermsInfo infos = Grammars.analyseGrammar(grammar, null);
		PredictionTable predictTable = Grammars.predictionTable(grammar, infos);
		
		File file = new File("src/test/examples/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package test.examples;\n");
			if (!predictTable.isLL1()) {
				System.out.println("Cannot generate parser for non-LL(1) grammar:");
				printConflicts(predictTable);
				return;
			}
			GrammarOutput.output(writer, className, grammar, predictTable);

		}
		System.out.println("Generated in " + file.getAbsolutePath());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// generateLexer(ArithGround.LEXER, "ArithGroundLexer");
		// generateParser("ArithGroundParser", ArithGround.GRAMMAR);

		generateLexer(StraightLineProgram.LEXER, "StraightLineLexer");
		generateParser("StraightLineParser", StraightLineProgram.GRAMMAR);
	}
}