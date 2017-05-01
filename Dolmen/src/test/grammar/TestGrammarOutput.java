package test.grammar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import codegen.GrammarOutput;
import syntax.Grammar;
import syntax.Grammars;
import syntax.Grammars.NTermsInfo;
import syntax.Grammars.PredictionTable;

/**
 * Tests for the generation of the parser class
 * for a couple of grammars
 * 
 * @author Stéphane Lescuyer
 */
public class TestGrammarOutput {

	private TestGrammarOutput() {
		// Static utility only
	}

	private static void printConflicts(PredictionTable table) {
		System.out.println(table.toString());
	}
	
	private static void testOutput(String className, Grammar grammar) {
		File file = new File("src-gen/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			NTermsInfo infos = Grammars.analyseGrammar(grammar, null);
			PredictionTable predictTable = Grammars.predictionTable(grammar, infos);
			if (!predictTable.isLL1()) {
				System.out.println("Cannot generate parser for non-LL(1) grammar:");
				printConflicts(predictTable);
				return;
			}
			GrammarOutput.output(writer, className, grammar, predictTable);
			System.out.println("----------JAVA--------");
			System.out.println("Generated in " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testOutput("Test1", TestAnalyseGrammar.Test1.grammar);
		testOutput("Test2", TestAnalyseGrammar.Test2.grammar);
		testOutput("Latex", TestAnalyseGrammar.TestLatex.grammar);
		testOutput("LatexLL1", TestAnalyseGrammar.TestLatexLL1.grammar);
	}
}