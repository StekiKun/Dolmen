package org.stekikun.dolmen.test;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Generator;
import org.stekikun.dolmen.common.Prompt;

/**
 * A common interface to test procedures
 * 
 * <p>
 * Unitary tests in this interface are described
 * by a supplier of inputs, a function to transform
 * that input in some output value, and a function to
 * test the output value against the initial input.
 * 
 * <p>
 * Inputs and outputs should have a user-friendly
 * {@link #toString()} because it will be used 
 * when displaying tests results in interactive
 * or batch mode.
 * 
 * @author Stéphane Lescuyer
 *
 * @param <Input>	the type of inputs
 * @param <Output>  the type of outputs
 */
public interface TestUnit<Input, Output> {

	/**
	 * Describes the different modes in which tests
	 * can be performed
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static enum Mode {
		/**
		 * Performs the tests <i>interactively</i>, i.e.
		 * waiting for user confirmation between each test
		 * and printing extra info about each test
		 */
		INTERACTIVE,
		/**
		 * Performs the tests in <i>batch</i>, only
		 * displaying details about the failed tests
		 */
		BATCH,
		/**
		 * Performs the tests in <i>quiet batch</i> mode,
		 * only displaying a summary after all tests have
		 * been performed
		 */
		QUIET;
	}
	
	/**
	 * @return a user-friendly description of the test
	 */
	public String name();
	
	/**
	 * @return a generator of inputs, used to provide
	 * 		sample for the testing
	 */
	public Generator<Input> generator();
	
	/**
	 * @param input
	 * @return the result of applying the tested
	 * 		procedure on {@code input}
	 */
	public Output apply(Input input);
	
	/**
	 * Verifies that the result obtained from the
	 * given {@code input} passes testing
	 * 
	 * @param input
	 * @param output
	 * @return {@code null} if the output passes the
	 * 		test, or a description of what went wrong otherwise
	 */
	public @Nullable String check(Input input, Output output);
	
	/**
	 * Post-hook ran after all tests have been performed,
	 * typically used to display gathered timings or any
	 * other useful info. The default implementation does
	 * nothing.
	 */
	public default void postHook() {
		// Nothing
	}
	
	/**
	 * Runs this testing unit on the specified number of samples.
	 * Depending on the chosen {@code mode}, the tests are
	 * performed interactively, or in batch mode.
	 * 
	 * @param numTests
	 * @param mode
	 * @see Mode
	 */
	public default void run(int numTests, Mode mode) {
		System.out.println(name());
		Generator<Input> sampler = generator();
		int success = 0;
		int failure = 0;
		int splits = numTests / 10;
		for (int i = 0; i < numTests; ++i) {
			if (mode == Mode.INTERACTIVE) {
				String line = Prompt.getInputLine("Press ENTER to proceed on test #" + i);
				if (line == null || line.equals("q") || line.equals("quit"))
					break;
			}
			
			Input input = sampler.generate();
			if (mode == Mode.INTERACTIVE)
				System.out.printf("[Test %d] Input: %s\n", i, input);
			Output output = apply(input);
			@Nullable String res = check(input, output);
			if (mode == Mode.INTERACTIVE) {
				System.out.printf("[Test %d] Output: %s\n", i, output);
				System.out.printf("[Test %d] Check result: %s\n", i, res);
			}
			
			if (res == null) ++success;
			else ++failure;
			
			if (mode != Mode.QUIET) {
				if (mode != Mode.INTERACTIVE && res != null) {
					System.out.printf("[Test %d] Input: %s\n", i, input);
					System.out.printf("[Test %d] Output: %s\n", i, output);
				}
				if (mode == Mode.INTERACTIVE || (res != null && failure <= 500)) {
					String msg = String.format("[%d/%d] %s",
						i, numTests, res == null ? "success" : res);
					System.out.println(msg);
				}
				if (res != null && failure == 500) {
					System.out.printf(
						"[%d/%d] Already 500 failed tests, not displaying anymore.\n",
						i, numTests);
				}
			}
			else {
				if (i % splits == 0) {
					System.out.printf("..%d%%..", (i * 10) / splits);
					if ((i * 10) / splits == 90) System.out.println("");
				}
			}
		}
		System.out.printf("%d samples tested: %d successful, %d failed\n",
			success + failure, success, failure);
		postHook();
	}
	
}