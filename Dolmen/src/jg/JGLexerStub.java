package jg;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
import common.Bookkeeper;
import jl.JLLexerGenerated;
import jl.JLParser;
import syntax.Lexer;

/**
 * This class is a simple entry point for the
 * generation of the lexer described in the
 * 'JGLexer.jl' description file. It must be
 * generated along with the tokens and parser
 * from the manually described grammar in
 * {@link JGParser}.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class JGLexerStub {

	private JGLexerStub() {
		// Static utility only
	}

	static void generateLexer(String filename, String className) throws IOException {
		Bookkeeper tasks = Bookkeeper.start(System.out, "Generating lexer for " + filename);
		FileReader reader = new FileReader(filename);
		JLLexerGenerated lexer = new JLLexerGenerated(filename, reader);
		JLParser parser = new JLParser(lexer, JLLexerGenerated::main);
		Lexer lexerDef = parser.parseLexer();
		reader.close();
		tasks.done("Successfully parsed lexer description");
		Automata aut = Determinize.lexer(lexerDef, true);
		tasks.done("Computed automata");
		File file = new File("src/jg/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package jg;\n");
			AutomataOutput.outputDefault(writer, className, aut);
		}
		tasks.leaveWith("Generated in " + file.getAbsolutePath());
	}
	
	/**
	 * Generates the Java lexer from the lexer description
	 * in 'JGLexer.jl'.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		generateLexer("src/jg/JGLexer.jl", "JGLexer");
	}
}
