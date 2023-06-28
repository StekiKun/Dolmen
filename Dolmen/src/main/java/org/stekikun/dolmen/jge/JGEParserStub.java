package org.stekikun.dolmen.jge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.List;

import org.stekikun.dolmen.codegen.GrammarOutput;
import org.stekikun.dolmen.codegen.BaseParser.ParsingException;
import org.stekikun.dolmen.codegen.LexBuffer.LexicalError;
import org.stekikun.dolmen.common.Bookkeeper;
import org.stekikun.dolmen.common.CountingWriter;
import org.stekikun.dolmen.jg.JGLexer;
import org.stekikun.dolmen.jg.JGParserGenerated;
import org.stekikun.dolmen.syntax.IReport;
import org.stekikun.dolmen.syntax.Reporter;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.Grammars;

/**
 * This class is a simple entry point for the
 * generation of the parser described in the
 * 'JGEParser.jg' description file.
 *  
 * @author Stéphane Lescuyer
 */
public abstract class JGEParserStub {

	private JGEParserStub() {
		// Static utility only
	}
	
	static void generateParser(String filename, String className, boolean withPos) throws IOException {
		PrintStream log = System.out;
		Bookkeeper tasks = Bookkeeper.start(log, "Compiling grammar description " + filename);
		
		JGLexer jgLexer = null;
		try (FileReader reader = new FileReader(filename)) {
			jgLexer = new JGLexer(filename, reader);
			JGParserGenerated jgParser = new JGParserGenerated(jgLexer, JGLexer::main);
			Grammar grammar = jgParser.start();
			tasks.done("Grammar description successfully parsed");
			
			Reporter reporter = new Reporter();
			Grammars.PredictionTable predictTable =
				Grammars.predictionTable(grammar, Grammars.analyseGrammar(grammar, null, reporter));
			tasks.done("Analysed grammar and built prediction table");
			tasks.problems(reporter.getReports().size());
			List<IReport> conflicts = predictTable.findConflicts();
			if (!conflicts.isEmpty()) {
				tasks.aborted("Grammar is not LL(1)");
				log.println();
				conflicts.forEach(conflict -> log.println(conflict.display()));
				return;
			}
			tasks.done("Grammar is LL(1)");
			
			File file = new File("src/org/stekikun/dolmen/jge/" + className + ".java");
			try (Writer writer =
					new CountingWriter(new FileWriter(file, false))) {
				writer.append("package org.stekikun.dolmen.jge;\n\n");
				GrammarOutput.outputDefault(writer, className, grammar, predictTable);
				tasks.done("Generated parser in " + file);
			} catch (IOException e) {
				tasks.aborted("Could not output generated parser");
				e.printStackTrace(log);
				return;
			}
			tasks.leave();
			return;
		}
		catch (LexicalError e) {
			tasks.aborted("Lexical error in grammar description: " + e.getMessage());
		}
		catch (ParsingException e) {
			tasks.aborted("Syntax error in grammar description: " + e.getMessage());
		}
		catch (Grammar.IllFormedException e) {
			tasks.aborted("Grammar description is not well-formed: " + e.getMessage());
		}
		catch (FileNotFoundException e) {
			e.printStackTrace(log);
		}
		catch (IOException e) {
			e.printStackTrace(log);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		generateParser("src/org/stekikun/dolmen/jge/JGEParser.jg", "JGEParser", false);
	}
}