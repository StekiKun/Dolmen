package org.stekikun.dolmen.test.regular;

import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Generator;
import org.stekikun.dolmen.common.Nulls;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.syntax.Regulars;
import org.stekikun.dolmen.test.TestUnit;

/**
 * Testing unit which generates regular expressions
 * and tests their {@link Regulars#project projection}
 * on a number of samples.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestRegularProjection
	implements TestUnit<Regular, @Nullable String> {

	private final int maxSamples;
	
	/**
	 * Creates a test unit which will try at most {@code maxSamples}
	 * on each regular expression
	 * 
	 * @param maxSamples
	 */
	public TestRegularProjection(int maxSamples) {
		this.maxSamples = maxSamples;
	}
	
	@Override
	public String name() {
		return "Testing that projection of regular expressions is correct";
	}

	@Override
	public Generator<Regular> generator() {
		return Regular.generator();
	}

	// A projection map is well-formed if and only if:
	//  - its keys are non-empty
	//  - its keys are pairwise disjoint character sets
	private StringBuilder testWF(StringBuilder buf, Map<CSet, Regular> map) {
		map.forEach((cs, r) -> {
			if (cs.isEmpty())
				buf.append("Empty character set in map\n");
			
			map.forEach((cs2, r2) -> {
				if (cs == cs2) {
					if (r != r2) {
						buf.append("Duplicate keys in map: ");
						buf.append(cs).append("->").append(r);
						buf.append(" and ");
						buf.append(cs2).append("->").append(r2);
						buf.append("\n");
					}
				}
				else {
					CSet inter = CSet.inter(cs, cs2);
					if (!inter.isEmpty()) {
						buf.append("Conflicting entries: ");
						buf.append(cs).append("->").append(r);
						buf.append(" and ");
						buf.append(cs2).append("->").append(r2);
						buf.append(" not disjoint: ").append(inter);
						buf.append("\n");
					}
				}
			});
		});
		return buf;
	}
	
	// The domain of a projection map is the union of its keys
	private CSet domain(Map<CSet, Regular> map) {
		return map.keySet().stream().reduce(CSet.EMPTY, CSet::union);
	}
	
	// If a non-empty string s starting with c matches the regular expression
	// then c must be in the projection's domain and the remainder of the matcher
	// must match the projected regexp
	private StringBuilder testMatcher(StringBuilder buf,
			Regular reg, Map<CSet, Regular> map, String matcher) {
		char c = matcher.charAt(0);
		
		Optional<@NonNull CSet> key =
			map.keySet().stream().filter(cs -> cs.contains(c)).findFirst();
		if (!key.isPresent()) {
			buf.append("Matcher ").append(matcher)
				.append(" correspond to empty projection in ").append(map)
				.append("\n");
			return buf;
		}
		
		Regular regc = Nulls.ok(map.get(key.get()));
		// Lax matching must be used because projecting (a b EOF)*
		// expands the Kleene star and this does not preserve strict
		// semantics of EOF
		if (Regulars.matches(regc, matcher.substring(1), false) == null) {			
			buf.append(String.format("Matcher %s matches regular expression %s"
				+ " but its tail %s does not match the projection on %c (%s, from %s)\n",
				matcher, reg, matcher.substring(1), c, regc, map));
		}
		return buf;
	}
	
	@Override
	public @Nullable String apply(Regular input) {
		StringBuilder buf = new StringBuilder();
		
		Map<CSet, Regular> allMap = Regulars.project(input, CSet.ALL);
		testWF(buf, allMap);
		
		CSet first = Regulars.first(input);
		if (!CSet.equivalent(first, domain(allMap)))
			buf.append("Domain of *-projection ").append(domain(allMap))
				.append(" differs from first-set ").append(first).append("\n");
		
		Map<CSet, Regular> firstMap = Regulars.project(input, first);
		testWF(buf, firstMap);
		if (!CSet.equivalent(first, domain(firstMap)))
			buf.append("Domain of first-projection ").append(domain(firstMap))
				.append(" differs from first-set ").append(first).append("\n");
		
		int i = 0;
		for (String matcher : Regulars.witnesses(input)) {
			if (matcher.isEmpty()) continue;
			++i;
			if (i > maxSamples) break;
			
			testMatcher(buf, input, allMap, matcher);
			testMatcher(buf, input, firstMap, matcher);
		}
		
		if (buf.length() == 0) return null;
		return buf.toString();
	}

	@Override
	public @Nullable String check(Regular input, @Nullable String output) {
		return output;
	}
}
