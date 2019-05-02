package org.stekikun.dolmen.test.grammar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;

import org.eclipse.jdt.annotation.NonNull;
import org.stekikun.dolmen.automaton.Automata;
import org.stekikun.dolmen.automaton.Determinize;
import org.stekikun.dolmen.codegen.AutomataOutput;
import org.stekikun.dolmen.codegen.Config;
import org.stekikun.dolmen.codegen.GrammarOutput;
import org.stekikun.dolmen.codegen.Config.Keys;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.jg.JGLexer;
import org.stekikun.dolmen.jg.JGParserGenerated;
import org.stekikun.dolmen.jl.JLLexerGenerated;
import org.stekikun.dolmen.jl.JLParser;
import org.stekikun.dolmen.syntax.Lexer;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.Grammars;

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
	
	static void generateParser(String filename, String className, boolean withPos) throws IOException {
		FileReader reader = new FileReader(filename);
		JGLexer lexer = new JGLexer(filename, reader);
		JGParserGenerated parser = of(lexer);
		Grammar grammar = parser.start();
		reader.close();
		System.out.println(grammar.toString());
		Grammars.PredictionTable predictTable =
			Grammars.predictionTable(grammar, Grammars.analyseGrammar(grammar, null, null));
		if (!predictTable.isLL1())
			System.out.println(predictTable.toString());
		File file = new File("src/org/stekikun/dolmen/test/examples/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			Config config = new Config(
					new EnumMap<>(
						Maps.<@NonNull Keys, @NonNull Boolean> singleton(Keys.Positions, withPos)));
			writer.append("package org.stekikun.dolmen.test.examples;\n");
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
		generateParser("tests/jg/JSon.jg", "JSonParser", true);

		generateLexer("tests/jl/JSonLW.jl", "JSonLWLexer");
		generateParser("tests/jg/JSonLW.jg", "JSonLWParser", false);		
	}
}
