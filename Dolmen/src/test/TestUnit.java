package test;

import org.eclipse.jdt.annotation.Nullable;

import common.Generator;

/**
 * A common interface to test procedures
 * 
 * <p>
 * Unitary tests in this interface are described
 * by a supplier of inputs, a function to transform
 * that input in some output value, and a function to
 * test the output value against the initial input.
 * 
 * @author Stéphane Lescuyer
 *
 * @param <Input>	the type of inputs
 * @param <Output>  the type of outputs
 */
public interface TestUnit<Input, Output> {

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
	 * other useful info
	 */
	public void postHook();
	
	/**
	 * 
	 * @param numTests
	 */
	public default void run(int numTests) {
		System.out.println(name());
		Generator<Input> sampler = generator();
		int success = 0;
		int failure = 0;
		for (int i = 0; i < numTests; ++i) {
			Input input = sampler.generate();
			Output output = apply(input);
			@Nullable String res = check(input, output);
			if (res == null) ++success;
			else ++failure;
			String msg = String.format("[%d/%d] %s",
				i, numTests, res == null ? "success" : res);
			System.out.println(msg);
		}
		System.out.printf("%d samples tested: %d successful, %d failed",
			numTests, success, failure);
		postHook();
	}
	
}