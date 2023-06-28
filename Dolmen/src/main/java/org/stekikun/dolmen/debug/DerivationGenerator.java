package org.stekikun.dolmen.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Nulls;
import org.stekikun.dolmen.syntax.TokenDecl;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.GrammarRule;
import org.stekikun.dolmen.unparam.Production;
import org.stekikun.dolmen.unparam.Production.ItemKind;

/**
 * TODO
 * 
 * @author Stéphane Lescuyer
 */
public final class DerivationGenerator {
	private final Grammar grammar;
	
	// Indexing terminals
	private final Map<String, Short> tokenIndices;
	
	// Indexing non-terminals
	private final Map<String, Short> ruleIndices;
	private final @NonNull GrammarRule[] rulesIndexed;
	
	/**
	 * An instance of this class is used for every non-terminal
	 * in the grammar to list complete derivations of this non-terminal
	 * that have already computed, in increasing order of depth.
	 * <p>
	 * This makes it possible to generate derivations of larger depths
	 * without having to recompute the possible sub-derivations.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private final class DerivationCache {
		// The list of markers in the {@link #cache} list. The i-th marker
		// marks the starting index for the derivations of depth i + 1 in the
		// cache.
		private final List<Integer> markers;
		// The list of cached derivations, in increasing order of depth
		private final List<Derivation> cache;
		
		DerivationCache() {
			this.markers = new ArrayList<>();
			this.cache = new ArrayList<>();
		}
		
		private int start(short depth) {
			if (depth == 0) return 0;
			return markers.get(depth - 1);
		}
		
		private int end(short depth) {
			return markers.get(depth);
		}
		
		/**
		 * NB: It may be that there are no derivations of such depth.
		 * 
		 * @param depth
		 * @return whether this cache knows about derivations of the given {@code depth}
		 */
		boolean knowsDepth(short depth) {
			return markers.size() > depth;
		}
		
		/**
		 * Must only be called on a depth that is {@link #knowsDepth(short) known}
		 * in this cache.
		 * 
		 * @param depth
		 * @return whether the list of derivations of the given {@code depth}
		 * 	  in the cache is empty or not
		 */
		boolean hasDepth(short depth) {
			return end(depth) > start(depth);
		}
		
		/**
		 * @param depth
		 * @return how many derivations are known for depth less or equal to {@code depth}
		 */
		int knownForMaxDepth(short depth) {
			return end(depth);
		}

		/**
		 * Calls the given {@code consumer} method on all the cached derivations of
		 * depth less or equal to {@code depth}, in increasing order of depth
		 * 
		 * @param depth
		 * @param consumer
		 */
		void forMaxDepth(short depth, Consumer<Derivation> consumer) {
			final int end = end(depth);
			for (int i = 0; i < end; ++i)
				consumer.accept(cache.get(i));
		}

		/**
		 * Calls the given {@code consumer} method on all the cached derivations of
		 * depth exactly equal to {@code depth}
		 * 
		 * @param depth
		 * @param consumer
		 */
		void forDepth(short depth, Consumer<Derivation> consumer) {
			final int end = end(depth);
			for(int i = start(depth); i < end; ++i)
				consumer.accept(cache.get(i));
		}
		
		/**
		 * Registers the given sequence of {@code derivations} in the cache
		 * as all the derivations of exactly the given {@code depth}. {@code depth}
		 * must be unknown in the cache, and must be the lowest unknown depth.
		 * 
		 * @param depth
		 * @param derivations
		 */
		void addDepth(short depth, List<Derivation> derivations) {
			if (markers.size() != depth)
				throw new IllegalArgumentException();
			cache.addAll(derivations);
			markers.add(cache.size());
			// Sanity checking
			for (Derivation d : derivations) {
				if (d.getHeight() != depth + 1) {
					throw new IllegalArgumentException("Derivation of depth " + d.getHeight()
						+ " whereas " + (depth + 1) + " is expected:\n" + d.display(grammar));
				}
			}
		}
	}
	
	// Caching the generated derivations for every rule
	private final DerivationCache[] ruleCache;
	
	/**
	 * This builds an object capable of generating derivations for the
	 * given {@code grammar}. The same object can be used more than once,
	 * and subsequent calls can be faster thanks to the computations from
	 * the former calls being cached.
	 * 
	 * @see #forEachDerivation(String, short, Consumer)
	 * 
	 * @param grammar
	 */
	public DerivationGenerator(Grammar grammar) {
		this.grammar = grammar;
		// Indexing terminals
		this.tokenIndices = new LinkedHashMap<>(grammar.tokenDecls.size());
		short tokenIdx = 0;
		for (TokenDecl td : grammar.tokenDecls)
			this.tokenIndices.put(td.name.val, tokenIdx++);
		// Indexing non-terminals
		this.ruleIndices = new LinkedHashMap<>(grammar.rules.size());
		GrammarRule[] rulesIndexed0 = new GrammarRule[grammar.rules.size()];
		short ruleIdx = 0;
		for (GrammarRule rule : grammar.rules.values()) {
			this.ruleIndices.put(rule.name.val, ruleIdx);
			// Initializing every cell in rulesIndexed0 justifies calling Nulls.arrayOk below
			rulesIndexed0[ruleIdx++] = rule;
		}
		this.rulesIndexed = Nulls.arrayOk(rulesIndexed0);
		// Derivations cache
		this.ruleCache = new DerivationCache[grammar.rules.size()];
	}
	
	/**
	 * Generates all the possible derivations of the non-terminal symbol
	 * in the underlying grammar with the name {@code ruleName} which are
	 * of depth less or equal to {@code depth}. The {@code consumer} method
	 * is called on each derivation in turn, in increasing order of depth.
	 * 
	 * @param ruleName		the non-terminal to derivate
	 * @param depth			the maximum depth (inclusive)
	 * @param consumer		what to do with the generated derivations
	 */
	public void forEachDerivation(String ruleName, short depth, Consumer<Derivation> consumer) {
		@Nullable Short rule = ruleIndices.get(ruleName);
		if (rule == null) throw new NoSuchElementException("Unknown rule " + ruleName);
		DerivationCache cache = generate(rule, depth);
		cache.forMaxDepth(depth, consumer);
	}
	
	private DerivationCache cacheFor(int rule) {
		@Nullable DerivationCache cache = ruleCache[rule];
		if (cache == null) {
			cache = new DerivationCache();
			ruleCache[rule] = cache;
		}
		return cache;
	}
	
	private DerivationCache generate(short rule, short depth) {
		// If the derivations have already been cached, nothing to do
		final DerivationCache cache = cacheFor(rule);
		if (cache.knowsDepth(depth)) return cache;
		// Otherwise, generate for the corresponding rule
		generateUpTo(cache, rule, depth);
		return cache;
	}
	
	private void generateUpTo(DerivationCache cache, short rule, short depth) {
		final GrammarRule grule = rulesIndexed[rule];
		// Iteratively generate derivations of depth 0 to the required one
		for (short i = 0; i <= depth; ++i) {
			if (cache.knowsDepth(i)) continue;
			generateDepth(cache, grule, rule, i);
		}
	}
	
	private void generateDepth(DerivationCache cache, GrammarRule grule, short rule, short depth) {
		// Assumes that cache has all the depths strictly below depth, and that grule is #rule
		ArrayList<Derivation> derivations = new ArrayList<>(grule.productions.size());
		short prod = -1;
		for (Production gprod : grule.productions) {
			++prod;
			generateProduction(derivations, rule, gprod, prod, depth);
		}
		// Cache the new derivations
		cache.addDepth(depth, derivations);
	}
	
	private void generateProduction(List<Derivation> acc, short rule, Production gprod, short prod, short depth) {
		if (depth == 0) {
			// At depth-0, we can only produce a non-terminal from terminals
			int nterms = hasNonTerminal(gprod);
			if (nterms < 0) return;
			final List<Derivation> leaves;
			if (nterms == 0) leaves = Collections.emptyList();
			else {
				leaves = new ArrayList<>(nterms);
				for (Production.Actual actual : gprod.actuals()) {
					short idx = Nulls.ok(tokenIndices.get(actual.item.val));
					leaves.add(Derivation.terminal(idx));
				}
			}
			acc.add(Derivation.production(rule, prod, leaves));
			return;
		}
		// At other depths, we need at least one of the sub-derivations to be of
		// the previous depth. In general, try to predict the number of derivations
		// that will be produced and see if it is positive.
		final short pdepth = (short) (depth - 1);
		final int count = hasActualDepth(rule, gprod, pdepth);
		if (count == 0) return;
		
		final List<Production.Item> pitems = producerItems(gprod);
		final int lastNonTerm = lastNonTerm(pitems);
		final int nchildren = pitems.size();
		final List<Derivation> buffer = new ArrayList<Derivation>(nchildren);
		class Producer {
			final int i;
			final boolean atMaxDepth;
			
			Producer(int i, boolean atMaxDepth) {
				this.i = i;
				this.atMaxDepth = atMaxDepth;
			}
			
			private void produce0() {
				final Production.Item item = pitems.get(i);
				final short irule;
				switch (item.getKind()) {
				case ACTUAL:
					Production.Actual actual = (Production.Actual) item;
					if (actual.isTerminal()) {
						short tok = Nulls.ok(tokenIndices.get(actual.item.val));
						buffer.add(Derivation.terminal(tok));
						new Producer(i + 1, pdepth == 0 ? true : atMaxDepth).produce();
						buffer.remove(i);
						return;
					}
					irule = Nulls.ok(ruleIndices.get(actual.item.val));
					break;
				case CONTINUE:
					irule = rule;
					break;
				case ACTION:
				default:
					throw new IllegalStateException();
				}
				DerivationCache cache = cacheFor(irule);
				if (atMaxDepth) {
					// Once at max depth, we produce everything at max depth or below
					cache.forMaxDepth(pdepth, subd -> {
						buffer.add(subd);
						new Producer(i + 1, true).produce();
						buffer.remove(i);
					});
				}
				else {
					// If not at max depth yet, we produce separately at max depth,
					// and strictly below where max depth must still be reached
					cache.forDepth(pdepth, subd -> {
						buffer.add(subd);
						new Producer(i + 1, true).produce();
						buffer.remove(i);
					});
					if (pdepth > 0 && i < lastNonTerm) {
						// No need to do that if depth was 0, or if
						// we had reached the last producer item
						cache.forMaxDepth((short) (pdepth - 1), subd -> {
							buffer.add(subd);
							new Producer(i + 1, false).produce();
							buffer.remove(i);		
						});
					}
				}
			}
			
			void produce() {
				// We should come in with exactly i derivations in the buffer
				if (buffer.size() != i) throw new IllegalStateException();
				// If we are done, register the new complete derivation
				if (i == nchildren)
					acc.add(Derivation.production(rule, prod, new ArrayList<>(buffer)));
				// Otherwise, we produce the possible derivations for the
				// current item and proceed recursively
				else
					produce0();
			}
		}
		new Producer(0, false).produce();
	}
	
	/**
	 * @param prod
	 * @return {@code -1} if and only if the given production
	 * 	contains a non-terminal or a continuation, and the number
	 *  of actual terminals it contains otherwise
	 */
	private static int hasNonTerminal(Production prod) {
		int res = 0;
		for (Production.Item item : prod.items) {
			switch (item.getKind()) {
			case ACTION:
				continue;
			case ACTUAL:
				Production.Actual actual = (Production.Actual) item;
				if (actual.isTerminal()) { ++res; continue; }
				//$FALL-THROUGH$
			case CONTINUE:
				return -1;
			default:
				throw new IllegalStateException();
			}
		}
		return res;
	}
	
	private int hasActualDepth(short rule, Production prod, short depth) {
		boolean found = false;
		int count = 1;
		for (Production.Actual actual : prod.actuals()) {
			// A terminal can only produce itself
			if (actual.isTerminal()) continue;
			// If the actual is a non-terminal, we need to make sure its
			// derivations are generated first; this recursive process will
			// stop as the depth decreases each time.
			@Nullable Short nterm = ruleIndices.get(actual.item.val);
			if (nterm == null) throw new IllegalStateException(actual.item.val);
			DerivationCache cache = generate(nterm, depth);
			// We now check if it has derivations of the exact depth at all,
			// how many below that depth in general
			if (!found) found = cache.hasDepth(depth);
			count *= cache.knownForMaxDepth(depth);
			// If we are down to no solution at all, we can abort
			if (count == 0) return 0;
		}
		if (prod.continuation() != null) {
			DerivationCache cache = cacheFor(rule);
			if (!found) found = cache.hasDepth(depth);
			count *= cache.knownForMaxDepth(depth);
		}
		if (!found) return 0;
		return count;
	}
	
	private static List<Production.Item> producerItems(Production gprod) {
		ArrayList<Production.Item> res = new ArrayList<>(gprod.items.size());
		for (Production.Item item : gprod.items) {
			if (item.getKind() == ItemKind.ACTION) continue;
			res.add(item);
		}
		res.trimToSize();
		return res;
	}
	
	private static int lastNonTerm(List<Production.Item> items) {
		int last = -1;
		int i = 0;
		for (Production.Item item : items) {
			if (item instanceof Production.Actual) {
				if (!((Production.Actual) item).isTerminal())
					last = i;
			}
			++i;
		}
		return last;
	}
}
