package test.grammar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
import codegen.GrammarOutput;
import jg.JGLexer;
import jg.JGParserGenerated;
import jl.JLLexerGenerated;
import jl.JLParser;
import syntax.Grammar;
import syntax.Grammars;
import syntax.Lexer;

/**
 * This class tests the grammar description parser by
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

	@SuppressWarnings("null")
	private static JGParserGenerated of(JGLexer lexer) {
		if (!tokenize)
			return new JGParserGenerated(lexer::main);
		else
			return new JGParserGenerated(new Supplier<JGParserGenerated.Token>() {
				@Override
				public JGParserGenerated.@NonNull Token get() {
					JGParserGenerated.Token res = lexer.main();
					System.out.println(res);
					return res;
				}
			});
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
	
	static void generateParser(String filename, String className) throws IOException {
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
			writer.append("package test.examples;\n");
			GrammarOutput.output(writer, "JSonParser", grammar, predictTable);
		}
		System.out.println("Generated in " + file.getAbsolutePath());
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		generateLexer("tests/jl/JSon.jl", "JSonLexer");
		generateParser("tests/jg/JSon.jg", "JSonParser");
	}
}
