package org.stekikun.dolmen.test.regular;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Generator;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.syntax.Regulars;
import org.stekikun.dolmen.test.TestUnit;

/**
 * Unitary test of the methods {@link Regulars#removeNestedBindings(Regular)}
 * and {@link Regulars#removeNestedBindings2(Regular)} which are supposed
 * to perform the same task, namely transform regular expressions by
 * removing redundant nested bindings.
 * <p>
 * This testing unit also gives a means to compare the performance
 * of both alternatives.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestRemoveNestedBinding
	implements TestUnit<Regular, TestRemoveNestedBinding.Result> {

	/**
	 * The result of each these tests, i.e. the computation
	 * of both {@link Regulars#removeNestedBindings(Regular)}
	 * and {@link Regulars#removeNestedBindings2(Regular)}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Result {
		final Regular resFolder;
		final Regular resSwitch;
		
		Result(Regular resFolder, Regular resSwitch) {
			this.resFolder = resFolder;
			this.resSwitch = resSwitch;
		}
		
		@Override
		public String toString() {
			return "[resFolder = " + resFolder.toString()
				+ ", resSwitch = " + resSwitch.toString() + "]";
		}
	}

	private long timeFolder = 0;
	private long timeSwitch = 0;
	
	/**
	 * Construct a fresh instance
	 */
	public TestRemoveNestedBinding() {
		// Nothing to do
	}

	@Override
	public String name() {
		return "Testing Regulars.removeNestedBinding against Regulars.removeNestedBinding2";
	}

	@Override
	public Generator<Regular> generator() {
		return Regular.generator();
	}

	@Override
	public Result apply(Regular input) {
		long start = System.nanoTime();
		Regular regFolder = Regulars.removeNestedBindings(input);
		long inter = System.nanoTime();
		Regular regSwitch = Regulars.removeNestedBindings2(input);
		long end = System.nanoTime();
		timeFolder += inter - start;
		timeSwitch += end - inter;
		return new Result(regFolder, regSwitch);
	}

	@Override
	public @Nullable String check(Regular input, Result output) {
		// The two results must be structurally equal
		if (Regulars.equal(output.resFolder, output.resSwitch))
			return null;
		else {
			StringBuilder buf = new StringBuilder();
			buf.append("Not the same results: ");
			buf.append(" input: " + input.toString());
			buf.append(" folder: " + output.resFolder.toString());
			buf.append(" switch: " + output.resSwitch.toString());
			return buf.toString();
		}		
	}

	@Override
	public void postHook() {
		System.out.println("Total time spent in folder: " 
							+ (timeFolder / 1000) + "µs");
		System.out.println("Total time spent in switch: "
							+ (timeSwitch / 1000) + "µs");
	}

}