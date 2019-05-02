package org.stekikun.dolmen.jge;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.stekikun.dolmen.automaton.Automata;
import org.stekikun.dolmen.automaton.Determinize;
import org.stekikun.dolmen.codegen.AutomataOutput;
import org.stekikun.dolmen.common.Bookkeeper;
import org.stekikun.dolmen.jle.JLELexer;
import org.stekikun.dolmen.jle.JLEParser;
import org.stekikun.dolmen.syntax.Lexer;

/**
 * This class is a simple entry point for the
 * generation of the lexer described in the
 * 'JGELexer.jl' description file. It must be
 * generated along with the tokens and parser
 * from the grammar in `JGEParser.jg`.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class JGELexerStub {

	private JGELexerStub() {
		// Static utility only
	}

	static void generateLexer(String filename, String className) throws IOException {
		Bookkeeper tasks = Bookkeeper.start(System.out, "Generating lexer for " + filename);
		FileReader reader = new FileReader(filename);
		JLELexer lexer = new JLELexer(filename, reader);
		JLEParser parser = new JLEParser(lexer, JLELexer::main);
		Lexer lexerDef = parser.lexer();
		reader.close();
		tasks.done("Successfully parsed lexer description");
		Automata aut = Determinize.lexer(lexerDef, true);
		tasks.done("Computed automata");
		File file = new File("src/org/stekikun/dolmen/jge/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package org.stekikun.dolmen.jge;\n");
			AutomataOutput.outputDefault(writer, className, aut);
		}
		tasks.leaveWith("Generated in " + file.getAbsolutePath());
	}
	
	/**
	 * Generates the Java lexer from the lexer description
	 * in 'JGELexer.jl'.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		generateLexer("src/org/stekikun/dolmen/jge/JGELexer.jl", "JGELexer");
	}
}
