package test;

import common.CSet;
import syntax.Regular;

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
	
	private static boolean test_regular_gen = true;
	private static void testRegularGeneration() {
		if (!test_regular_gen) return;
		Regular.generator().present();
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
	}
}
