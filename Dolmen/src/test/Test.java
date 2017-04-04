package test;

import common.CSet;

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

	private static void testCSetGeneration() {
		CSet.generator().present();
	}
	
	/**
	 * Entry point of the tests
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// 1. CSet generation
		testCSetGeneration();
	}
}
