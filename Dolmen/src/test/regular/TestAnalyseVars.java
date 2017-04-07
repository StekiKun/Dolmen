package test.regular;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import common.Generator;
import common.Sets;
import syntax.Regular;
import syntax.Regulars;
import syntax.Regulars.VarsInfo;
import test.TestUnit;

/**
 * Testing unit which generates matchers for regular
 * expressions using {@link Regulars#witnesses},
 * matches them using {@link Regulars#matches(Regular, String)}
 * and compares the obtained bindings with the classification
 * returned by {@link Regulars#analyseVars(Regular)}.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestAnalyseVars
	implements TestUnit<Regular, TestAnalyseVars.Result> {

	/**
	 * The result of each of these tests, regrouping the
	 * result of {@link Regulars#analyseVars(Regular)},
	 * a list of matchers for the input regular expression,
	 * and the bindings obtained for each of these matchers,
	 * in the same order.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Result {
		final VarsInfo varsInfo;
		final Set<String> matchers;
		final List<@Nullable Map<String, String>> bindings;
		
		Result(VarsInfo varsInfo, 
				Set<String> matchers, 
				List<@Nullable Map<String, String>> bindings) {
			this.varsInfo = varsInfo;
			this.matchers = matchers;
			this.bindings = bindings;
		}
	}
	
	private final int maxSamples;
	
	/**
	 * Returns a new instance of this test unit,
	 * which tests at most {@code maxSamples} 
	 * matchers per regular expression
	 * 
	 * @param maxSamples
	 */
	public TestAnalyseVars(int maxSamples) {
		this.maxSamples = maxSamples;
	}

	@Override
	public String name() {
		return "Testing that the variable analysis performed by Regulars.analyseVars" 
				+ " correspond to bindings found by Regulars.matches";
	}

	@Override
	public Generator<Regular> generator() {
		return Regular.generator();
	}

	@Override
	public Result apply(Regular input) {
		// Analyse variables in input regexp
		VarsInfo varsInfo = Regulars.analyseVars(input);
		// Find at most maxSamples unique witnesses
		int found = 0;
		Set<String> matchers = new HashSet<String>();
		for (String matcher : Regulars.witnesses(input)) {
			if (matchers.add(matcher)) {
				if (++found == maxSamples) break;
			}
		}
		// Compute bindings for these witnesses
		List<@Nullable Map<String, String>> bindings =
			new ArrayList<>(matchers.size());
		for (String matcher : matchers)
			bindings.add(Regulars.matches(input, matcher));
		
		return new Result(varsInfo, matchers, bindings);
	}

	@Override
	public @Nullable String check(Regular input, Result output) {
		// Traverse matchers set and bindings combined, and check them
		final VarsInfo info = output.varsInfo;
		Iterator<String> matcherIt = output.matchers.iterator();
		for (Map<String, String> bindings : output.bindings) {
			String matcher = matcherIt.next();
			if (bindings == null)
				return "Failed to match [" + matcher + "] against regexp " + input;
			 
			// Now check every found binding
			for (Map.Entry<String, String> entry : bindings.entrySet()) {
				final String key = entry.getKey();
				final String bound = entry.getValue();
				
				// The key must be in allVars
				if (!info.allVars.contains(key))
					return "Found bound variable " + key 
							+ " which was not in allVars=" + info.allVars
							+ "(matcher=" + matcher + ", regexp=" + input + ")";
				// If the bound substring has size > 1, then
				// the variable must be in strVars
				if (bound.length() != 1 && !info.strVars.contains(key))
					return "Found variable " + key + " bound to non-char \"" 
						+ bound + "\" which was not in strVars=" + info.strVars
						+ "(matcher=" + matcher + ", regexp=" + input + ")";
				// chrVars is a heuristic, it can't really be incorrect
				// Actual char variables will be chrVars-strVars
				
				// Cannot check dblVars unless matching
				// returns a multimap instead
			}
			// Check that all bindings that were not present
			// were indeed in optVars
			@SuppressWarnings("null")
			Set<String> missing = Sets.diff(info.allVars, bindings.keySet());
			if (!info.optVars.containsAll(missing)) {
				missing.removeAll(info.optVars);
				return "Unbound variables " + missing 
						+ " were not in optVars=" + info.optVars
						+ "(matcher=" + matcher + ", regexp=" + input + ";";
			}
		}
		return null;
	}

}