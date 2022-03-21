package org.stekikun.dolmen.test.grammar;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.debug.DerivationGenerator;
import org.stekikun.dolmen.jge.JGELexer;
import org.stekikun.dolmen.jge.JGEParser;
import org.stekikun.dolmen.syntax.PGrammar;
import org.stekikun.dolmen.syntax.PGrammars;
import org.stekikun.dolmen.syntax.Reporter;
import org.stekikun.dolmen.unparam.Expansion;
import org.stekikun.dolmen.unparam.Expansion.PGrammarNotExpandable;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.Grammar.IllFormedException;

/**
 * This test the generation of all derivations for a {@link Grammar}
 * using {@link DerivationGenerator}. Beware that the number of
 * possible derivations typically grows very fast, so be careful when
 * picking a maximum depth to generate to.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class TestDerivations {

	private TestDerivations() {
		// Static utility only
	}

	private static @Nullable Grammar
	loadGrammar(String filename, String className) throws IOException {
		final PGrammar pgrammar;
		try (FileReader reader = new FileReader(filename)) {
			JGELexer lexer = new JGELexer(filename, reader);
			JGEParser parser = new JGEParser(lexer, JGELexer::main);
			pgrammar = parser.start();
		}
		Reporter reporter = new Reporter();
		PGrammars.Dependencies deps = PGrammars.dependencies(pgrammar.rules);
		PGrammars.findUnusedSymbols(pgrammar, deps, reporter);
		PGrammars.analyseGrammar(pgrammar, deps, reporter);
		if (!reporter.getReports().isEmpty()) {
			if (reporter.hasErrors()) {
				System.err.println(reporter);
				return null;
			}
			System.out.println(reporter);
		}

		try {
			Expansion.checkExpandability(pgrammar);
			Grammar grammar = Expansion.of(pgrammar);
			System.out.println("Loaded grammar from " + filename);
//			System.out.println(grammar);
			return grammar;
		} catch (PGrammarNotExpandable e) {
			System.out.println(e.getReport().display());
			return null;
		} catch (IllFormedException e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Generates all derivations up to the given {@code depth} for the
	 * non-terminal with the given {@code ruleName} in {@code grammar}.
	 * 
	 * @param grammar
	 * @param ruleName
	 * @param depth
	 */
	static void generateDerivations(Grammar grammar, String ruleName, int depth) {
		DerivationGenerator gen = new DerivationGenerator(grammar);
		long start = System.currentTimeMillis();
		AtomicInteger count = new AtomicInteger(0);
		gen.forEachDerivation(ruleName, (short) depth, d -> {
			System.out.println("Derivation [height=" + d.getHeight() + "]");
			System.out.println(d.display(grammar));
			count.incrementAndGet();
		});
		long elapsed = System.currentTimeMillis() - start;
		System.out.println("Found " + count.get() + " derivations in " + elapsed + "ms.");
	}
	
	/**
	 * This loads a grammar and tries to systematically generates the possible
	 * derivations of some non-terminal of the grammar.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Grammar jsonLW = Objects.requireNonNull(loadGrammar("tests/jg/JSonLW.jg", "JSonParser"));
		generateDerivations(jsonLW, "json", 8);
//		generateDerivations(jsonLW, "pair", 2);
//		generateDerivations(jsonLW, "more_members", 2);
//		generateDerivations(jsonLW, "members", 3);
//		generateDerivations(jsonLW, "object", 4);
	}
}
