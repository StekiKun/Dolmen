package jg;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
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
		// TODO Auto-generated constructor stub
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
		File file = new File("src/jg/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package jg;\n");
			AutomataOutput.output(writer, className, aut);
		}
		System.out.println("Generated in " + file.getAbsolutePath());
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
