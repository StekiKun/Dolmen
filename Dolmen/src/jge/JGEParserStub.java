package jge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.List;

import codegen.BaseParser.ParsingException;
import codegen.GrammarOutput;
import codegen.LexBuffer.LexicalError;
import common.Bookkeeper;
import common.CountingWriter;
import jg.JGLexer;
import jg.JGParserGenerated;
import syntax.Grammar;
import syntax.Grammars;
import syntax.IReport;

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
			
			Grammars.PredictionTable predictTable =
				Grammars.predictionTable(grammar, Grammars.analyseGrammar(grammar, null));
			tasks.done("Analysed grammar and built prediction table");
			List<IReport> conflicts = predictTable.findConflicts();
			if (!conflicts.isEmpty()) {
				tasks.aborted("Grammar is not LL(1)");
				log.println(conflicts);
				return;
			}
			tasks.done("Grammar is LL(1)");
			
			File file = new File("src/jge/" + className + ".java");
			try (Writer writer =
					new CountingWriter(new FileWriter(file, false))) {
				writer.append("package jge;\n\n");
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
			tasks.aborted("Lexical error in grammar description");
			log.println(e.getMessage());
		}
		catch (ParsingException e) {
			tasks.aborted("Syntax error in grammar description");
			log.println(e.getMessage());
		}
		catch (Grammar.IllFormedException e) {
			tasks.aborted("Grammar description is not well-formed");
			log.println(e.getMessage());
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
		generateParser("src/jge/JGEParser.jg", "JGEParser", false);
	}
}