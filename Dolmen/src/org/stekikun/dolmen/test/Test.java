package org.stekikun.dolmen.test;

import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Generator;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.syntax.Regulars;
import org.stekikun.dolmen.test.TestUnit.Mode;
import org.stekikun.dolmen.test.cset.TestCSetCompare;
import org.stekikun.dolmen.test.cset.TestCSetOperations;
import org.stekikun.dolmen.test.regular.TestAnalyseVars;
import org.stekikun.dolmen.test.regular.TestEncoder;
import org.stekikun.dolmen.test.regular.TestGenerateMatchers;
import org.stekikun.dolmen.test.regular.TestGenerateTMatchers;
import org.stekikun.dolmen.test.regular.TestOptimiser;
import org.stekikun.dolmen.test.regular.TestRegularProjection;
import org.stekikun.dolmen.test.regular.TestRemoveNestedBinding;

/**
 * Class to interactively test the various features 
 * in the project
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Test {

	private Test() {
		// Static utility class only
	}

	private static boolean test_cset_gen = false;
	private static void testCSetGeneration() {
		if (!test_cset_gen) return;
		CSet.generator().present();
	}
	
	private static TestRegistry testCSetOperations() {
		return TestRegistry.create()
				.addIf(new TestCSetCompare(30), 20000, true)
				.addIf(new TestCSetOperations(50), 20000, true)
				.done();
	}
	
	private static boolean test_regular_gen = false;
	private static void testRegularGeneration() {
		if (!test_regular_gen) return;
		Regular.generator().present();
	}
	
	private static boolean test_regular_wit_gen = false;
	private static void testRegularWitnessGeneration() {
		if (!test_regular_wit_gen) return;
		Regular.generator().present(
			reg -> Generator.ofIterable("Generating matching strings",
				Regulars.witnesses(reg)).present());
	}
	
	private static TestRegistry testRegularOperations() {
		return TestRegistry.create()
					.addIf(new TestRemoveNestedBinding(), 20000, true)
					.addIf(new TestGenerateMatchers(20), 20000, true)
					.addIf(new TestAnalyseVars(30), 20000, true)
					.addIf(new TestGenerateTMatchers(20), 20000, true)
					.addIf(new TestEncoder(20), 20000, true)
					.addIf(new TestOptimiser(20), 20000, true)
					.addIf(new TestRegularProjection(30), 20000, true)
					.done();
	}
	
	/**
	 * Entry point of the tests
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// 1. CSet generation
		testCSetGeneration();
		// 2. CSet operations tests
		testCSetOperations().run(Mode.BATCH);
		// 3. Regular expression generation
		testRegularGeneration();
		// 4. Regular expression matchers generation
		testRegularWitnessGeneration();
		// 5. Regular expression operations tests
		testRegularOperations().run(Mode.BATCH);
	}
}