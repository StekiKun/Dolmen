package org.stekikun.dolmen.test.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.codegen.LexBuffer;
import org.stekikun.dolmen.codegen.LexBuffer.LexicalError;
import org.stekikun.dolmen.test.examples.Alphabet;


/**
 * This class tests the input stack feature of {@link LexBuffer}s
 * by tokenizing an easy textual language with '#include' directives.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestIncludes {
	private TestIncludes() {
		// Static utility only
	}
		
	private static String testLexer(String filename) throws IOException {
		try (Reader file = new BufferedReader(new FileReader(filename))) {
			Alphabet lexer = new Alphabet(filename, file);
			StringBuilder buf = new StringBuilder();
			@Nullable String tok;
			while ((tok = lexer.main()) != null)
				buf.append(tok);
			return buf.toString();
		}
		catch (LexicalError e) {
			throw new IllegalStateException("Lexical error: " + e.getMessage());
		}
	}
		
	@SuppressWarnings("javadoc")
	public static void main(String args[]) throws IOException {
		String contents = testLexer("tests/inputs/alphabet.txt");
		String contents2 = testLexer("tests/inputs/alpha1.txt");
		String contents3 = testLexer("tests/inputs/alpha.txt");
		
		if (!contents.equals(contents2))
			throw new IllegalStateException("Mismatched parsed contents: " + contents + "<>\n" + contents2);
		if (!contents.equals(contents3))
			throw new IllegalStateException("Mismatched parsed contents: " + contents + "<>\n" + contents3);
		
		System.out.println("Include and switching directives were tested with success");
	}
}