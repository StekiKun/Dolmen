package test.cset;

import java.util.Random;
import java.util.function.BiFunction;

import org.eclipse.jdt.annotation.Nullable;

import common.CSet;
import common.Generator;
import test.TestUnit;

/**
 * A test unit which generates pairs of character sets and tests the 
 * correctness of the various character sets operations provided by
 * {@link CSet}: {@link CSet#union}, {@link CSet#diff}, {@link CSet#inter}
 * and {@link CSet#complement}.
 * 
 * @author Stéphane Lescuyer
 */
public class TestCSetOperations 
	implements TestUnit<TestCSetOperations.Input, @Nullable String> {

	/**
	 * Inputs for this test unit are pairs of character sets
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static class Input {
		final CSet cs1;
		final CSet cs2;
		
		Input(CSet cs1, CSet cs2) {
			this.cs1 = cs1;
			this.cs2 = cs2;
		}
		
		@Override public String toString() {
			return String.format("[cs1=%s, cs2=%s]", cs1, cs2);
		}
		
		static Generator<Input> generator() {
			final Generator<CSet> gen = CSet.generator();
			return new Generator<TestCSetOperations.Input>() {
				@Override
				public String name() {
					return "Generator for pairs of CSets";
				}

				@Override
				public Input generate() {
					return new Input(gen.get(), gen.get());
				}
			};
		}
	}
	
	private int maxSamples;
	
	/**
	 * Returns a new instance of this test unit,
	 * which tests at most {@code maxSamples} characters
	 * per generated pair of character sets
	 * 
	 * @param maxSamples
	 */
	public TestCSetOperations(int maxSamples) {
		this.maxSamples = maxSamples;
	}
	
	@Override
	public String name() {
		return "Testing that CSet operations are correct";
	}

	@Override
	public Generator<Input> generator() {
		return Input.generator();
	}

	
	private void testBinop(StringBuilder buf, 
		Input input, CSet result, String name,
		BiFunction<Boolean, Boolean, Boolean> property,
		char sample) {
		boolean b1 = input.cs1.contains(sample);
		boolean b2 = input.cs2.contains(sample);
		boolean b = result.contains(sample);
		if (property.apply(b1, b2) == b)
			return;
		
		buf.append("Issue with " + name).append(": ");
		buf.append("[cs1=").append(input.cs1).append(",");
		buf.append("cs2=").append(input.cs2).append(",");
		buf.append("res=").append(result).append(",");
		buf.append("sample=").append(CSet.charToString(sample, false)).append(",");
		buf.append(b1).append(",").append(b2).append(",").append(b);
		buf.append("]");
	}
	
	private static boolean testUnion(boolean b1, boolean b2) {
		return b1 || b2;
	}
	private static boolean testInter(boolean b1, boolean b2) {
		return b1 && b2;
	}
	private static boolean testDiff(boolean b1, boolean b2) {
		return b1 && !b2;
	}
	private static boolean testCompl(boolean b1, boolean b2) {
		return !b1;
	}
	
	@Override
	public @Nullable String apply(Input input) {
		final CSet cs1 = input.cs1;
		final CSet cs2 = input.cs2;
		StringBuilder buf = new StringBuilder();
		
		// Union
		CSet union = CSet.union(cs1, cs2);
		// Inter
		CSet inter = CSet.inter(cs1, cs2);
		// Diff
		CSet diff = CSet.diff(cs1, cs2);
		// Complement
		CSet compl = CSet.complement(cs1);
		
		Random rand = new Random(System.nanoTime());
		for (int i = 0; i < maxSamples; ++i) {
			char sample = (char) rand.nextInt(0xFFFF + 1);
			testBinop(buf, input, union, "union", 
					TestCSetOperations::testUnion, sample);
			testBinop(buf, input, inter, "inter", 
					TestCSetOperations::testInter, sample);
			testBinop(buf, input, diff, "diff",
					TestCSetOperations::testDiff, sample);
			if (sample != 0xFFFF)	// complement excludes EOF
				testBinop(buf, input, compl, "complement",
						TestCSetOperations::testCompl, sample);
		}
		
		if (buf.length() == 0) return null;
		return buf.toString();
	}

	@Override
	public @Nullable String check(Input input, @Nullable String output) {
		return output;
	}
	
}
