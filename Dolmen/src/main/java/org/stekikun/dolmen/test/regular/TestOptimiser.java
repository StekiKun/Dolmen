package org.stekikun.dolmen.test.regular;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Generator;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.common.Sets;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.syntax.Regulars;
import org.stekikun.dolmen.syntax.Regulars.VarsInfo;
import org.stekikun.dolmen.tagged.Encoder;
import org.stekikun.dolmen.tagged.Optimiser;
import org.stekikun.dolmen.tagged.TRegular;
import org.stekikun.dolmen.tagged.TRegulars;
import org.stekikun.dolmen.tagged.Optimiser.Allocated;
import org.stekikun.dolmen.tagged.Optimiser.IdentInfo;
import org.stekikun.dolmen.tagged.Optimiser.TagAddr;
import org.stekikun.dolmen.tagged.Optimiser.TagKey;
import org.stekikun.dolmen.tagged.TRegular.TagInfo;
import org.stekikun.dolmen.test.TestUnit;

/**
 * Testing unit which generates regular expressions, encodes
 * them as a tagged regular expression, optimises them using
 * {@link Optimiser}, generates matchers for the original
 * expression and compares the bindings found when
 * matching the generated string with both the syntactic
 * and optimised regular expression.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestOptimiser
	implements TestUnit<Regular, TestOptimiser.Result> {

	/**
	 * The result of each of these tests, regrouping
	 * the tagged regular expression, the result of
	 * variable analysis on the original expression
	 * and the optimised regular expression.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Result {
		final List<CSet> charSets;
		final VarsInfo varsInfo;
		final TRegular tagged;
		final Allocated allocated;
		
		Result(List<CSet> charSets, VarsInfo varsInfo,
				TRegular tagged, Allocated allocated) {
			this.charSets = charSets;
			this.varsInfo = varsInfo;
			this.tagged = tagged;
			this.allocated = allocated;
		}
		
		@Override
		public String toString() {
			return "{charSets=" + charSets + ", tagged=" 
				+ tagged + ", varsInfo=" + varsInfo 
				+ ", allocated=" + allocated + "}";
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
	public TestOptimiser(int maxSamples) {
		this.maxSamples = maxSamples;
	}

	@Override
	public String name() {
		return "Testing that optimising regular expressions does not"
				+ " change the language it recognizes.";
	}

	@Override
	public Generator<Regular> generator() {
		// This test must use a special generator config to disallow
		// regular expressions (r as c) where r is statically of size 0
		// (i.e. epsilon, eof, or a combination thereof).
		// The reason for this limitation is that when such expressions appear
		// after an EOF, the test can record failures (albeit very rarely) 
		// For instance, consider the following regexp:
		//    r = (EOF | "" as a) ("" as b)
		// Because of the binding to b, the right hand side cannot be optimized
		// away in Regular, and because of strict matching, the only way to match
		// the empty input string "" with r is to skip the EOF and choose a,
		// yielding the bindings {a = "", b = ""}. Now, after optimisation of tags,
		// the binding for b is not necessary anymore, and the whole right hand
		// side of r is optimized away in TRegular:
		//   optimized(r) = (EOF | {start tag de a} {end tag de a})
		// This regular expression will accept the same match as r,
		// but it also has strict matches that go through EOF. If such
		// a match is chosen, then the binding for a will be reported
		// as being unaccounted for.
		//
		// More generally, the issue lays with strict vs non-strict matching
		// and the presence of EOF, but any empty-matching expression on the
		// right-hand side of an EOF with bindings can produce these failure.
		// The simplest way to get rid of them is to avoid generating such
		// dummy bindings. Of course this means we don't test the optimisation
		// of such expressions but I would rather thoroughly test expressions
		// which have a reasonable chance of happening in a real lexer description.
		
		Regular.Gen.Config config = new Regular.Gen.Config();
		// disable empty bindings
		config.allowEmptyBindings = false;
		return new Regular.Gen(new java.util.Random(), config);
	}

	@Override
	public Result apply(Regular input) {
		Encoder encoder = new Encoder(true);
		VarsInfo varsInfo = Regulars.analyseVars(input);
		TRegular tagged = encoder.encode(input, varsInfo.getCharVars(), 0);
		Allocated allocated = Optimiser.optimise(varsInfo, true, tagged);
		return new Result(encoder.getCharacterSets(), varsInfo, tagged, allocated);
	}

	@Override
	public @Nullable String check(Regular input, Result output) {
		// Generate at most maxSamples unique matchers for input,
		// match them both for input and output.tagged,
		// compare the bound substrings
		int found = 0;
		Set<String> checked = new HashSet<>();
		// Important: generate matchers and check them with
		//	respect to the version where nested bindings have
		//  been removed. It makes a difference in cases like
		//	 ((r as c)* as b)* 
		//  where removing the b binding can lead to collapsing
		//  the two Kleene stars into one: (r as c)*
		// This is correct in general but since we are using a
		// finite approximation in Regulars.matches/TRegulars.matches
		// the two are actually not equivalent and yield different
		// matches.
		Regular regular = Regulars.removeNestedBindings2(input);
		final TRegular optimised = output.allocated.regular;
		for (String matcher : Regulars.witnesses(regular)) {
			if (checked.add(matcher)) {
				++found;
				
				@Nullable Map<String, String> bindings = Regulars.matches(regular, matcher);
				if (bindings == null)
					return "Generated matcher \"" + matcher + "\" did not match " + regular;
				@Nullable Map<TagInfo, Integer> markers =
					TRegulars.matches(output.charSets, optimised, matcher);
				if (markers == null)
					return "Generated matcher \"" + matcher + "\" did not match optimised "
							+ output.tagged + " wrt " + output.charSets;
				
				Set<TagInfo> visited = new HashSet<>();
				// Each string in bindings must correspond to one or two markers,
				// either directly or via relative indexing wrt other markers
				for (Map.Entry<String, String> binding : bindings.entrySet()) {
					final String key = binding.getKey();
					Object poss = 
						findTaggedPositions(matcher, markers, 
							output.allocated, key, visited);
					if (poss instanceof String) return ((String) poss);
					int[] iposs = (int[]) (poss);
					String bound = matcher.substring(iposs[0], iposs[1]);
					if (!bound.equals(binding.getValue()))
						return "In matcher \"" + matcher + "\", subexpression " 
							+ binding.getKey() + " tagged to \"" + bound 
							+	"\" differs from matched substring \"" 
							+ binding.getValue() + "\"";
				}
				
				// Conversely, all markers must be accounted by bindings
				Set<TagInfo> unaccounted = Sets.diff(markers.keySet(), visited);
				if (!unaccounted.isEmpty())
					return "With matcher \"" + matcher + "\", the tags "
							+ unaccounted + " were unaccounted for in "
							+ "the bindings " + bindings;
				
				if (found == maxSamples) break;
			}
		}
		
		// Everything ok
		return null;
	}

	private static Object
		findTaggedPositions(String input, Map<TagInfo, Integer> markers, 
				Allocated allocated, String name, Set<TagInfo> visited) {
		@Nullable IdentInfo info = Maps.get(allocated.identInfos, name);
		if (info == null)
			return "Did not find info for " + name + " in " + allocated.identInfos;
		
		TagAddr start = info.start;
		Object posStart =
			decodeTagAddr(input, markers, allocated, visited, start);
		if (posStart instanceof String) return posStart;
		
		TagAddr end = info.end;
		Object posEnd;
		if (end == null)
			posEnd = ((Integer) posStart) + 1;
		else {
			posEnd = decodeTagAddr(input, markers, allocated, visited, end);
			if (posEnd instanceof String) return posEnd;
		}
		
		int istart = (Integer) posStart;
		int iend = (Integer) posEnd;
		return new int[] { istart, iend };
	}
	
	private static Object decodeTagAddr(String input, 
			Map<TagInfo, Integer> markers, Allocated allocated, 
			Set<TagInfo> visited, TagAddr addr) {
		int base = 0;
		switch (addr.base) {
		case TagAddr.START: base = 0; break;
		case TagAddr.END: base = input.length(); break;
		default: {
			// base is a memory cell index
			// find the remaining tags corresponding to this memory cell
			// there must be one because tags used as base addresses
			// are not removed and are matched as soon as depending
			// tags are matched
			Integer found = null;
			for (Map.Entry<TagInfo, Integer> marked : markers.entrySet()) {
				TagInfo ti = marked.getKey();
				TagKey tk = new TagKey(ti);
				@Nullable TagAddr ta = Maps.get(allocated.env, tk);
				if (ta == null) continue;	// it shouldn't actually happen
				if (ta.base == addr.base && ta.offset == 0) {
					visited.add(ti);
					found = marked.getValue(); break;
				}
			}
			if (found == null)
				return "Tag address " + addr + " is based on memory cell "
					+ addr.base + ", which does not correspond to any tag "
					+ "bound in the matcher " + markers + 
					" for \"" + input +"\"";
			base = found; break;
		}
		}
		return new Integer(base + addr.offset);
	}
}
