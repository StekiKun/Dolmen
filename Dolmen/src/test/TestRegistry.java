package test;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Instances of {@link TestRegistry} collect a number of
 * different {@link TestUnit test units} which can be
 * run together. Test registries are built in two
 * phases using a <i>builder</i> class, typically
 * like this:
 * <pre>
 *   TestRegistry registry =
 *   	TestRegistry.create()
 *   		.add(myTestUnit1)
 *   		.add(myTestUnit2)
 *   		.add(myTestUnit3)
 *   		.done();
 * </pre>
 * 
 * @author Stéphane Lescuyer
 */
public final class TestRegistry {

	private final Map<TestUnit<?, ?>, Integer> testUnits;
	
	private TestRegistry(Map<TestUnit<?, ?>, Integer> testUnits) {
		this.testUnits = testUnits;
	}
	
	/**
	 * Executes all the tests in the registry
	 */
	public void run() {
		testUnits.forEach((tu, n) -> tu.run(n));
	}
	
	/**
	 * Builder class for preparing instances of {@link TestRegistry}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Builder {
		private final Map<TestUnit<?, ?>, Integer> testUnits;
		
		private Builder() {
			this.testUnits = new IdentityHashMap<>();
		}
		
		/**
		 * Add a test unit to the registry builder, to be run
		 * on the specified number of samples
		 * 
		 * @param testUnit
		 * @param samples
		 * @return this builder, for possible method chaining
		 */
		public Builder add(TestUnit<?, ?> testUnit, int samples) {
			testUnits.put(testUnit, samples);
			return this;
		}

		/**
		 * Completes the initialization of the registry
		 * 
		 * @return the registry containing all added test units
		 */
		public TestRegistry done() {
			return new TestRegistry(testUnits);
		}
	}
	
	/**
	 * @return a fresh empty test registry builder
	 */
	public static Builder create() {
		return new Builder();
	}
}
