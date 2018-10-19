package jle;

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
 * 'JLELexer.jl' description file. It must be
 * generated along with the tokens and parser
 * from the grammar description in 'JLEParser.jg'.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class JLELexerStub {

	private JLELexerStub() {
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
		File file = new File("src/jle/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package jle;\n");
			AutomataOutput.output(writer, className, aut);
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
		generateLexer("src/jle/JLELexer.jl", "JLELexer");
	}
}