package test.grammar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;

import org.eclipse.jdt.annotation.NonNull;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
import codegen.Config;
import codegen.GrammarOutput;
import codegen.Config.Keys;
import common.Maps;
import jg.JGLexer;
import jg.JGParserGenerated;
import jl.JLLexerGenerated;
import jl.JLParser;
import syntax.Grammar;
import syntax.Grammars;
import syntax.Lexer;

/**
 * This class tests the simple grammar description parser by
 * parsing a few .jg files from the tests directory.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class TestJGParser {

	private TestJGParser() {
		// Static utility only
	}

	/** Whether tokens should be printed along the way, for debug */
	private static boolean tokenize = true;

	private static JGParserGenerated of(JGLexer lexer) {
		if (!tokenize)
			return new JGParserGenerated(lexer, JGLexer::main);
		else
			return new JGParserGenerated(lexer, lexbuf -> {
					JGParserGenerated.Token res = lexbuf.main();
					System.out.println(res);
					return res;
				});
	}
	
	static void generateLexer(String filename, String className) throws IOException {
		System.out.println("Parsing lexer description " + filename + "...");
		FileReader reader = new FileReader(filename);
		JLLexerGenerated lexer = new JLLexerGenerated(filename, reader);
		@SuppressWarnings("null")
		JLParser parser = new JLParser(lexer, JLLexerGenerated::main);
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
	
	static void generateParser(String filename, String className, boolean withPos) throws IOException {
		FileReader reader = new FileReader(filename);
		JGLexer lexer = new JGLexer(filename, reader);
		JGParserGenerated parser = of(lexer);
		Grammar grammar = parser.start();
		reader.close();
		System.out.println(grammar.toString());
		Grammars.PredictionTable predictTable =
			Grammars.predictionTable(grammar, Grammars.analyseGrammar(grammar, null));
		if (!predictTable.isLL1())
			System.out.println(predictTable.toString());
		File file = new File("src/test/examples/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			Config config = new Config(
					new EnumMap<>(
						Maps.<@NonNull Keys, @NonNull Boolean> singleton(Keys.Positions, withPos)));
			writer.append("package test.examples;\n");
			GrammarOutput.output(writer, className, config, grammar, predictTable);
		}
		System.out.println("Generated in " + file.getAbsolutePath());
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		generateLexer("tests/jl/JSon.jl", "JSonLexer");
		generateParser("tests/jg/JSon.jg", "JSonParser", false);

		generateLexer("tests/jl/JSonLW.jl", "JSonLWLexer");
		generateParser("tests/jg/JSonLW.jg", "JSonLWParser", false);
		
		generateLexer("tests/jl/JSonPos.jl", "JSonPosLexer");
		generateParser("tests/jg/JSonPos.jg", "JSonPosParser", true);
	}
}
