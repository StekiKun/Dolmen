package org.stekikun.dolmen.test.grammar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.stekikun.dolmen.automaton.Automata;
import org.stekikun.dolmen.automaton.Determinize;
import org.stekikun.dolmen.codegen.AutomataOutput;
import org.stekikun.dolmen.codegen.Config;
import org.stekikun.dolmen.codegen.GrammarOutput;
import org.stekikun.dolmen.jge.JGELexer;
import org.stekikun.dolmen.jge.JGEParser;
import org.stekikun.dolmen.jl.JLLexerGenerated;
import org.stekikun.dolmen.jl.JLParser;
import org.stekikun.dolmen.syntax.Lexer;
import org.stekikun.dolmen.syntax.PGrammar;
import org.stekikun.dolmen.syntax.PGrammars;
import org.stekikun.dolmen.syntax.Reporter;
import org.stekikun.dolmen.unparam.Expansion;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.Grammars;
import org.stekikun.dolmen.unparam.Expansion.PGrammarNotExpandable;
import org.stekikun.dolmen.unparam.Grammar.IllFormedException;

/**
 * This class tests the extended grammar description parser by
 * parsing a few .jg files from the tests directory.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class TestJGEParser {

	private TestJGEParser() {
		// Static utility only
	}

	/** Whether tokens should be printed along the way, for debug */
	private static boolean tokenize = true;

	private static JGEParser of(JGELexer lexer) {
		if (!tokenize)
			return new JGEParser(lexer, JGELexer::main);
		else
			return new JGEParser(lexer, lexbuf -> {
					JGEParser.Token res = lexbuf.main();
					System.out.println(res);
					return res;
				});
	}
	
	static void generateLexer(String filename, String className) throws IOException {
		System.out.println("Parsing lexer description " + filename + "...");
		FileReader reader = new FileReader(filename);
		JLLexerGenerated lexer = new JLLexerGenerated(filename, reader);
		JLParser parser = new JLParser(lexer, JLLexerGenerated::main);
		Lexer lexerDef = parser.parseLexer();
		reader.close();
		System.out.println("Computing automata...");
		Automata aut = Determinize.lexer(lexerDef, true);
		File file = new File("src/org/stekikun/dolmen/test/examples/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package org.stekikun.dolmen.test.examples;\n");
			AutomataOutput.outputDefault(writer, className, aut);
		}
		System.out.println("Generated in " + file.getAbsolutePath());
	}
	
	static void generateParser(String filename, String className) throws IOException {
		FileReader reader = new FileReader(filename);
		JGELexer lexer = new JGELexer(filename, reader);
		JGEParser parser = of(lexer);
		PGrammar pgrammar = parser.start();
		reader.close();
		System.out.println(pgrammar.toString());
		Reporter reporter = new Reporter();
		PGrammars.Dependencies deps = PGrammars.dependencies(pgrammar.rules);
		PGrammars.findUnusedSymbols(pgrammar, deps, reporter);
		PGrammars.analyseGrammar(pgrammar, deps, reporter);
		if (!reporter.getReports().isEmpty()) {
			if (reporter.hasErrors()) {
				System.err.println(reporter);
				return;
			}
			System.out.println(reporter);
		}

		try {
			Expansion.checkExpandability(pgrammar);
			Grammar grammar = Expansion.of(pgrammar);
			System.out.println("Generated ground grammar with " + 
				grammar.rules.size() + " rules");
			System.out.println(grammar);
			
			Grammars.PredictionTable predictTable =
				Grammars.predictionTable(grammar, 
					Grammars.analyseGrammar(grammar, null, null));
			if (!predictTable.isLL1())
				System.out.println(predictTable.toString());
			File file = new File("src/org/stekikun/dolmen/test/examples/" + className + ".java");
			try (FileWriter writer = new FileWriter(file, false)) {
				Config config = Config.ofGrammar(grammar, null);
				writer.append("package org.stekikun.dolmen.test.examples;\n");
				GrammarOutput.output(writer, className, config, grammar, predictTable);
			}
			System.out.println("Generated in " + file.getAbsolutePath());
		} catch (PGrammarNotExpandable e) {
			System.out.println(e.getReport().display());
			return;
		} catch (IllFormedException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		generateLexer("tests/jl/JSon.jl", "JSonLexer");
		generateParser("tests/jg/JSon.jg", "JSonParser");

		generateLexer("tests/jl/JSonLW.jl", "JSonLWLexer");
		generateParser("tests/jg/JSonLW.jg", "JSonLWParser");
		
		generateLexer("tests/jl/JSonPos.jl", "JSonPosLexer");
		generateParser("tests/jg/JSonPos.jg", "JSonPosParser");
		
		generateParser("tests/jg/Templates.jg", "Templates");
	}
}