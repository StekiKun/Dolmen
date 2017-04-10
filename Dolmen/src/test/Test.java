package test;

import common.CSet;
import common.Generator;
import syntax.Regular;
import syntax.Regulars;
import test.TestUnit.Mode;
import test.regular.TestAnalyseVars;
import test.regular.TestEncoder;
import test.regular.TestGenerateMatchers;
import test.regular.TestGenerateTMatchers;
import test.regular.TestOptimiser;
import test.regular.TestRemoveNestedBinding;

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
					.addIf(new TestRemoveNestedBinding(), 20000, false)
					.addIf(new TestGenerateMatchers(20), 20000, false)
					.addIf(new TestAnalyseVars(30), 20000, false)
					.addIf(new TestGenerateTMatchers(20), 20000, false)
					.addIf(new TestEncoder(20), 20000, false)
					.addIf(new TestOptimiser(20), 20000, true)
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
		// 2. Regular expression generation
		testRegularGeneration();
		// 3. Regular expression matchers generation
		testRegularWitnessGeneration();
		// 4. Regular expression operations tests
		testRegularOperations().run(Mode.INTERACTIVE);
	}
}