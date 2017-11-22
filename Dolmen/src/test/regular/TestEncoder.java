package test.regular;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import common.CSet;
import common.Generator;
import common.Maps;
import common.Sets;
import syntax.Regular;
import syntax.Regulars;
import syntax.Regulars.VarsInfo;
import tagged.Encoder;
import tagged.TRegular;
import tagged.TRegular.TagInfo;
import tagged.TRegulars;
import test.TestUnit;

/**
 * Testing unit which generates regular expressions, encodes
 * them as a tagged regular expression, generate matchers
 * for the former and compare the bindings found when
 * matching the generated string with both the syntactic
 * and tagged regular expression.
 * 
 * @author Stéphane Lescuyer
 */
public final class TestEncoder 
	implements TestUnit<Regular, TestEncoder.Result> {

	/**
	 * The result of each of these tests, regrouping
	 * the encoded regular expression, the set of bound
	 * names associated to subexpressions of size 1, 
	 * and the character sets used to encode the expression.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Result {
		final List<CSet> charSets;
		final TRegular tagged;
		final Set<String> charVars;
		
		Result(List<CSet> charSets, TRegular tagged, Set<String> charVars) {
			this.charSets = charSets;
			this.tagged = tagged;
			this.charVars = charVars;
		}
		
		@Override
		public String toString() {
			return "{charSets=" + charSets + ", tagged=" 
				+ tagged + ", charVars=" + charVars + "}";
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
	public TestEncoder(int maxSamples) {
		this.maxSamples = maxSamples;
	}

	@Override
	public String name() {
		return "Testing that encoding regular expressions does not"
				+ " change the language it recognizes.";
	}

	@Override
	public Generator<Regular> generator() {
		return Regular.generator();
	}

	@Override
	public Result apply(Regular input) {
		Encoder encoder = new Encoder(true);
		VarsInfo varsInfo = Regulars.analyseVars(input);
		TRegular tagged = encoder.encode(input, varsInfo.getCharVars(), 0);
		return new Result(encoder.getCharacterSets(), tagged, varsInfo.getCharVars());
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
		for (String matcher : Regulars.witnesses(regular)) {
			if (checked.add(matcher)) {
				++found;
				
				@Nullable Map<String, String> bindings = Regulars.matches(regular, matcher);
				if (bindings == null)
					return "Generated matcher \"" + matcher + "\" did not match " + regular;
				@Nullable Map<TagInfo, Integer> markers =
					TRegulars.matches(output.charSets, output.tagged, matcher);
				if (markers == null)
					return "Generated matcher \"" + matcher + "\" did not match encoded "
							+ output.tagged + " wrt " + output.charSets;
				
				Set<TagInfo> visited = new HashSet<>();
				// Each string in bindings must correspond to one or two markers
				for (Map.Entry<String, String> binding : bindings.entrySet()) {
					TagInfo tagStart = new TagInfo(binding.getKey(), true, 0);
					visited.add(tagStart);
					@Nullable Integer mark = Maps.get(markers, tagStart);
					if (mark == null)
						return "Tagged matcher did not contain tag " + tagStart 
							+ " but other matcher had the binding " 
							+ binding.getKey() + "->" + binding.getValue();
					int finish;
					if (output.charVars.contains(binding.getKey()))
						finish = mark + 1;
					else {
						TagInfo tagEnd = new TagInfo(binding.getKey(), false, 0);
						visited.add(tagEnd);
						@Nullable Integer fmark = Maps.get(markers, tagEnd);
						if (fmark == null)
							return "Tagged matcher did not contain tag " + tagStart 
									+ " but other matcher had the (non-char) binding " 
									+ binding.getKey() + "->" + binding.getValue();
						finish = fmark;
					}
					String bound = matcher.substring(mark, finish);
					if (!bound.equals(binding.getValue()))
						return "In matcher \"" + matcher + "\", subexpression " 
							+ binding.getKey() + " tagged to \"" + bound 
							+	"\" differs from matched substring \"" 
							+ binding.getValue() + "\"";
				}
				
				// Conversely, all markers must be accounted by bindings
				Set<TagInfo> unaccounted = Sets.diff(markers.keySet(), visited);
				if (!unaccounted.isEmpty())
					return "Some tags were unaccounted for: " + unaccounted;
				
				if (found == maxSamples) break;
			}
		}
		
		// Everything ok
		return null;
	}

}
