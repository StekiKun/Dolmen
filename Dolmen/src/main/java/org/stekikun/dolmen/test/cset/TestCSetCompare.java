package org.stekikun.dolmen.test.cset;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Generator;
import org.stekikun.dolmen.test.TestUnit;

/**
 * A test unit which generates character sets and tests the algebraic
 * property of the <i>natural ordering</i> on {@link CSet} as defined
 * by its implementation of the {@link Comparable} interface.
 * 
 * @author Stéphane Lescuyer
 */
public class TestCSetCompare implements TestUnit<CSet, @Nullable String> {

	private int maxSamples;
	
	/**
	 * Returns a new instance of this test unit,
	 * which tests at most {@code maxSamples} samples
	 * per generated character set
	 * 
	 * @param maxSamples
	 */
	public TestCSetCompare(int maxSamples) {
		this.maxSamples = maxSamples;
	}
	
	@Override
	public String name() {
		return "Testing that CSet implements the Comparable interface in a sound way";
	}

	@Override
	public Generator<CSet> generator() {
		return CSet.generator();
	}

	@Override
	public @Nullable String apply(CSet input) {
		StringBuilder buf = new StringBuilder();
		
		// Check that order is reflexive
		int c = input.compareTo(input);
		if (c != 0) {
			buf.append("Order irreflexive on ").append(input);
			buf.append(" (result ").append(c).append(")");
		}
		
		// Check that comparison is asymmetric and transitive
		Generator<CSet> gen = CSet.generator();
		for (int i = 0; i < maxSamples; ++i) {
			CSet cs = gen.get();
			
			int c1 = input.compareTo(cs);
			int c2 = cs.compareTo(input);
			if (-c1 != c2) {
				buf.append("Order asymmetry issue on ").append(input).append(" and ").append(cs);
				buf.append(" (results ").append(c1).append(" <-> ").append(c2);
			}
			
			// Check transitivity
			for (int j = 0; j < maxSamples; ++j) {
				CSet cs2 = gen.get();
				int c3 = cs.compareTo(cs2);
				if (c3 == c1) {
					int c13 = input.compareTo(cs2);
					if (c13 != c3) {
						buf.append("Order transitivity issue on ")
							.append(input).append(", ").append(cs).append(" and ").append(cs2);
						buf.append(" (results ").append(c1).append(" /\\ ").append(c3)
							.append(" -> ").append(c13);
					}
				}
			}
		}
		
		if (buf.length() == 0) return null;
		return buf.toString();
	}

	@Override
	public @Nullable String check(CSet input, @Nullable String output) {
		return output;
	}
	
}
