package test.grammar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import codegen.Config;
import codegen.TokensOutput;
import syntax.Grammar;

/**
 * Tests for the generation of the tokens class
 * for a couple of grammars
 * 
 * @author Stéphane Lescuyer
 */
public class TestTokensOutput {

	private TestTokensOutput() {
		// Static utility only
	}

	private static void testOutput(String className, Grammar grammar) {
		File file = new File("src-gen/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			Config config = Config.start()
				.tokenAnnotations("@SuppressWarnings(\"javadoc\")\n" +
								  "@org.eclipse.jdt.annotation.NonNullByDefault()")
				.done();
			TokensOutput.output(writer, className, config, 0, grammar.tokenDecls);
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
		testOutput("Test1Token", TestAnalyseGrammar.Test1.grammar);
		testOutput("Test2Token", TestAnalyseGrammar.Test2.grammar);
		testOutput("LatexToken", TestAnalyseGrammar.TestLatex.grammar);
	}
}
