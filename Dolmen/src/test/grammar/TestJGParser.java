package test.grammar;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;

import codegen.GrammarOutput;
import jg.JGLexer;
import jg.JGParserGenerated;
import syntax.Grammar;
import syntax.Grammars;

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
	
	static void testParse(String filename) throws IOException {
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
		FileWriter writer = new FileWriter("src-gen/JSonParser.java");
		GrammarOutput.output(writer, "JSonParser", grammar, predictTable);
		writer.close();
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		testParse("tests/jg/JSon.jg");
	}
}
