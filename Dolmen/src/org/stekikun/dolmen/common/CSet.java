package org.stekikun.dolmen.common;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A class to represent character sets.
 * <p>
 * Characters are Java characters, i.e. 16-bit unsigned integers.
 * Character sets are mostly encoded as an ordered list of
 * character intervals, with some special values for degenerate
 * cases, and are thus optimized for space rather than for lookup speed.
 * <p>
 * The special value 0xFFFF is reserved to denote end of file.
 * It is not a valid Unicode character anyway.
 * <p>
 * Instances of this class are immutable.
 * <p>
 * They implement the {@link Comparable} interface via a total 
 * ordering based on the lexicographic comparison of the character set,
 * with the empty character set being a conventional minimum.
 * It is not a particularly meaningful total order but it is
 * used to guarantee a deterministic traversal order in 
 * {@link CSet}-indexed maps. A more natural, yet partial, order
 * based on character set inclusion can be retrieved via
 * the method {@link #included(CSet, CSet)}.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class CSet implements Comparable<CSet> {

	/**
	 * @return {@code true} if this character set is empty
	 */
	public abstract boolean isEmpty();
	
	/**
	 * @param ch
	 * @return whether this character set contains {@code ch}
	 */
	public abstract boolean contains(char ch);
	
	/**
	 * @return the number of characters in this set
	 */
	public abstract int cardinal();
	
	@Override
	public abstract String toString();

	@Override
	public final boolean equals(@Nullable Object o) {
		if (!(o instanceof CSet))
			return false;
		CSet cs = (CSet) o;
		return equivalent(this, cs);
	}
	
	@Override
	public abstract int hashCode();
	
	@Override
	public final int compareTo(@Nullable CSet cs) {
		if (cs == null)
			throw new NullPointerException();
		return compare(this, cs);
	}
	
	/**
	 * @param ch
	 * @param javaSafe	should be {@code true} if the result
	 * 		string may end up in Java sources
	 * @return a string to display {@code ch}, either
	 * 	using the character itself if appropriate, or
	 *  the '\\uxxxx' form. If {@code javaSafe} is {@code true}
	 *  the '\\uxxxx' is not used because it would be unescaped
	 *  by a Java compiler, and the prefix '0x' is used instead
	 */
	public static String charToString(char ch, boolean javaSafe) {
		// Check for characters which must be escaped
		switch (ch) {
		case '[':
		case ']':
		case '^':
		case '\\':
		case '-':
		case '_':
			return "\\" + ch; // printable but escaped
		// Some printable ones that are not alphanumeric
		case '"':
		case '\'':
		case '/':
		case '$':
		case '#':
		case ':':
		case ';':
		case ',':
		case '~':
		case '+':
		case '*':
		case '.':
		case '@':
		case '!':
		case '?':
		case '|':
		case '<':
		case '>':
		case '(':
		case ')':
		case '{':
		case '}':
			return "" + ch;
		case 0xFFFF:
			return "EOF";
		}
		if (Character.isAlphabetic(ch) || Character.isDigit(ch))
			return "" + ch; // printable
		return (javaSafe ? "0x" : "\\u") + String.format("%04x", (short)ch);
	}
	
	/**
	 * The set of <b>all</b> possible characters, including
	 * the special end-of-file marker.
	 * <p>
	 * <i>Warning: also contains character values which
	 * do not correspond to a valid Unicode character.</i>
	 */
	public static final CSet ALL = new CSet() {
		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean contains(char ch) {
			return true;
		}

		@Override
		public int cardinal() {
			return 0x10000;
		}
		
		@Override
		public String toString() {
			return "_";
		}

		@Override
		public int hashCode() {
			return 0xFFFFFFF0;
		}
	};
	
	/**
	 * The <b>empty</b> character set
	 */
	public static final CSet EMPTY = new CSet() {
		@Override
		public boolean isEmpty() {
			return true;
		}
		
		@Override
		public boolean contains(char ch) {
			return false;
		}
		
		@Override
		public int cardinal() {
			return 0;
		}
		
		@Override
		public String toString() {
			return "∅";
		}

		@Override
		public int hashCode() {
			return 0xFFFFFFFF;
		}
	};
	
	/**
	 * Character sets reduced to a single character
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Singleton extends CSet {
		final char c;
		
		Singleton(char c) {
			this.c = c;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean contains(char ch) {
			return ch == c;
		}
		
		@Override
		public int cardinal() {
			return 1;
		}

		@Override
		public @NonNull String toString() {
			return charToString(c, true);
		}

		@Override
		public int hashCode() {
			return c;
		}
	}
	/**
	 * @param c
	 * @return the singleton character set containing {@code c}
	 */
	public static CSet singleton(char c) {
		return new Singleton(c);
	}
	
	/**
	 * Describes a single contiguous interval of characters,
	 * from {@link #first} to {@link #last}, inclusive.
	 * 
	 * Each interval can potentially be linked to another
	 * interval, forming a linked list of strictly ordered
	 * intervals.
	 * 
	 * Intervals are immutable, including the link pointers.
	 * Therefore common suffixes can be shared without restrain.
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Interval {
		final char first;
		final char last;
		@Nullable final Interval next;
		
		Interval(char first, char last, @Nullable Interval next) {
			assert (first <= last);
			this.first = first;
			this.last = last;
			this.next = next;
			// Check ordering with tail
			if (next != null)
				assert (last < next.first);
		}
		
		private StringBuilder append(StringBuilder buf) {
			if (first == last) 
				buf.append(charToString(first, true));
			else
				buf.append(charToString(first, true))
				   .append("-")
				   .append(charToString(last, true));
			if (next != null) {
				next.append(buf.append(' '));
			}
			return buf;
		}
		
		@Override
		public String toString() {
			return append(new StringBuilder()).toString();
		}
		
		@Override
		public int hashCode() {
			int n = 0;
			if (next != null)
				n = next.hashCode();
			return ((first << 16) | last) ^ n;
		}
	}
	
	/**
	 * Character sets described by the reunion of
	 * several character ranges, stored as an
	 * ordered linked list of {@link Interval}s
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static final class Intervals extends CSet {
		@Nullable final Interval head;

		private Intervals(@Nullable Interval head) {
			this.head = head;
		}
		
		@Override
		public boolean isEmpty() {
			return head == null;
		}

		@Override
		public boolean contains(char ch) {
			Interval cur = head;
			while (cur != null) {
				if (ch < cur.first) return false; // by ordering
				if (ch <= cur.last) return true;
				cur = cur.next;
			}
			return false;
		}

		@Override
		public int cardinal() {
			int res = 0;
			Interval cur = head;
			while (cur != null) {
				res += cur.last - cur.first + 1;
				cur = cur.next;
			}
			return res;
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("[");
			if (head != null)
				head.append(buf);
			buf.append("]");
			return buf.toString();
		}

		@Override
		public int hashCode() {
			if (head != null)
				return head.hashCode();
			return 0;
		}
	}
	
	/**
	 * @param first
	 * @param last
	 * @return the character set describing the single
	 * 		character range from {@code first} to
	 * 		{@code last}, inclusive
	 */
	public static CSet interval(char first, char last) {
		if (first > last) throw new IllegalArgumentException();
		if (first == last) return new Singleton(first);
		return new Intervals(new Interval(first, last, null));
	}

	/**
	 * Special character set used to denote end-of-file
	 */
	public static final CSet EOF = singleton((char)0xFFFF);

	/**
	 * Like {@link #ALL} but does not contain the special
	 * end-of-input marker
	 */
	public static final CSet ALL_BUT_EOF =
		interval((char)0, (char)0xFFFE);
	
	private static CSet intervals(@Nullable Interval i) {
		if (i == null) return EMPTY;
		if (i.next != null) return new Intervals(i);
		if (i.first == i.last) return new Singleton(i.first);
		if (i.first == 0) {
			if (i.last == 0xFFFE) return ALL_BUT_EOF;
			if (i.last == 0xFFFF) return ALL;
		}
		return new Intervals(i);
	}
	
	/**
	 * Applies the function {@code f} to all characters
	 * in the character set
	 * 
	 * @param f
	 */
	public void forEach(Consumer<? super Character> f) {
		@Nullable Interval cur = intervalsOf(this);
		while (cur != null) {
			for (int c = cur.first; c <= cur.last; ++c)
				f.accept((char) c);
			cur = cur.next;
		}
		return;
	}
	
	/**
	 * Applies the function {@code f} to all intervals in
	 * the character set. The intervals are guaranteed to
	 * be normalized in the following fashion:
	 * <ul>
	 * <li> there will be no empty intervals
	 * <li> intervals are visited in a strict increasing order
	 * <li> in particular, two successive intervals are necessarily
	 *      separated by at least one character or they would have been merged
	 * </ul>
	 * 
	 * @param f
	 */
	public void forEachInterval(BiConsumer<? super Character, ? super Character> f) {
		@Nullable Interval cur = intervalsOf(this);
		while (cur != null) {
			f.accept(cur.first, cur.last);
			cur = cur.next;
		}
		return;
	}
	
	/*
	 * Character set operations
	 * 
	 * All character sets operations are implemented
	 * with the following principles in mind:
	 * 
	 * - canonicity: character sets are supposed to be
	 * 	canonical in terms of representation (i.e. no 
	 * 	degenerate interval but ALL, EMPTY or Singleton
	 *  instead), and of course well-formed in that lists
	 *  of intervals are strictly-ordered. All operations
	 *  return character sets with the same property, and
	 *  so that this property can be relied upon when
	 *  comparing character sets, for instance.
	 * - suffix of interval lists are shared as most as
	 *  possible during operations, in order to take profit
	 *  of the fact that intervals are immutable.
	 */
	
	private static final Interval ALL_INTERVAL =
		new Interval((char)0, (char)0xFFFF, null);
	
	private static @Nullable Interval intervalsOf(CSet cs) {
		if (cs instanceof Intervals)
			return ((Intervals) cs).head;
		if (cs instanceof Singleton) {
			char c = ((Singleton) cs).c;
			return new Interval(c, c, null);
		}
		if (cs == ALL) return ALL_INTERVAL;
		if (cs == EMPTY) return null;
		throw new IllegalStateException();
	}
	
	/**
	 * @param i1
	 * @param i2
	 * @return the union of two ordered lists of intervals
	 * 		as an ordered list of intervals
	 */
	private static 
	@Nullable Interval iunion(
		@Nullable Interval i1, @Nullable Interval i2) {
		if (i1 == null) return i2;
		if (i2 == null) return i1;
		if (i1.first > i2.first) return iunion(i2, i1);
		if (i1.last + 1 < i2.first) {
			return new Interval(i1.first, i1.last,
								iunion(i1.next, i2));
		} else if (i1.last < i2.last) {
			return iunion(
					new Interval(i1.first, i2.last, i2.next),
					i1.next);
		} else {
			return iunion(i1, i2.next);
		}
	}
	
	/**
	 * Returns the union of the two given character sets
	 */
	public static CSet union(CSet cs1, CSet cs2) {
		if (cs1 == EMPTY || cs2 == ALL) return cs2;
		if (cs2 == EMPTY || cs1 == ALL) return cs1;
		// No EMPTY, no ALL
		return intervals(
				iunion(intervalsOf(cs1), intervalsOf(cs2)));		
	}
	
	/**
	 * @param charSets
	 * @return the union of all the given character sets
	 */
	public static CSet union(@NonNull CSet... charSets) {
		CSet res = EMPTY;
		for (int i = 0; i < charSets.length; ++i)
			res = union(res, charSets[i]);
		return res;
	}
	
	/**
	 * @param chars
	 * @return the character set containing exactly the given characters
	 */
	public static CSet chars(char... chars) {
		CSet res = EMPTY;
		for (int i = 0; i < chars.length; ++i)
			res = union(res, singleton(chars[i]));
		return res;
	}
	
	private static char maxChar(char c1, char c2) {
		if (c1 > c2) return c1;
		return c2;
	}
	
	/**
	 * @param i1
	 * @param i2
	 * @return the intersection of two ordered list
	 * 		of intervals, as an ordered list of intervals
	 */
	private static 
	@Nullable Interval iinter(
		@Nullable Interval i1, @Nullable Interval i2) {
		if (i1 == null || i2 == null) return null;
		if (i1.last < i2.first) {
			return iinter(i1.next, i2);
		} else if (i2.last < i1.first){
			return iinter(i1, i2.next);
		} else if (i1.last < i2.last) {
			return new Interval(maxChar(i1.first, i2.first), 
						i1.last,
						iinter(i1.next, i2));
		} else {
			return new Interval(maxChar(i1.first, i2.first),
						i2.last,
						iinter(i1, i2.next));
		}
	}
	
	/**
	 * Returns the intersection of the two given character sets
	 */
	public static CSet inter(CSet cs1, CSet cs2) {
		if (cs1 == EMPTY || cs2 == EMPTY) return EMPTY;
		if (cs1 == ALL) return cs2;
		if (cs2 == ALL) return cs1;
		if (cs1 instanceof Singleton) {
			char c1 = ((Singleton) cs1).c;
			if (cs2.contains(c1)) return cs1;
			else return EMPTY;
		}
		if (cs2 instanceof Singleton) {
			char c2 = ((Singleton) cs2).c;
			if (cs1.contains(c2)) return cs2;
			else return EMPTY;
		}
		return intervals(
				iinter(intervalsOf(cs1), intervalsOf(cs2)));
	}
	
	/**
	 * @param i1
	 * @param i2
	 * @return the difference between the ordered intervals
	 * 		{@code i1} and {@code i2}, in this order
	 */
	private static
	@Nullable Interval idiff(
		@Nullable Interval i1, @Nullable Interval i2) {
		if (i2 == null) return i1;
		if (i1 == null) return null;
		if (i1.last < i2.first) {
			return new Interval(i1.first, i1.last,
					idiff(i1.next, i2));
		} else if (i2.last < i1.first){
			return idiff(i1, i2.next);
		} else {
			Interval r = i1.next;
			Interval r2 = i2;
			if (i2.last < i1.last) {
				r = new Interval((char) (i2.last + 1), 
							i1.last, r);
				r2 = i2.next;
			}
			if (i1.first < i2.first) {
				return new Interval(i1.first, (char) (i2.first - 1),
							idiff(r, r2));
			} else {
				return idiff(r, r2);
			}
		}
	}
	
	/**
	 * @param cs1
	 * @param cs2
	 * @return the character set of all characters
	 * 		in {@code cs1} except those that are in {@code cs2}
	 */
	public static CSet diff(CSet cs1, CSet cs2) {
		if (cs1 == EMPTY || cs2 == ALL) return EMPTY;
		if (cs2 == EMPTY) return cs1;
		if (cs1 instanceof Singleton) {
			char c1 = ((Singleton) cs1).c;
			if (cs2.contains(c1)) return EMPTY;
			else return cs1;
		}
		if (cs2 instanceof Singleton) {
			char c2 = ((Singleton) cs2).c;
			if (!cs1.contains(c2)) return cs1;
		}
		return intervals(
				idiff(intervalsOf(cs1), intervalsOf(cs2)));
	}
	
	/**
	 * @param cs
	 * @return the complement of the character set {@code cs},
	 * 	<b>excluding the end-of-input marker</b>
	 */
	public static CSet complement(CSet cs) {
		return diff(ALL_BUT_EOF, cs);
	}
	
	private static int icompare(
			@Nullable Interval i1, @Nullable Interval i2) {
		if (i1 == i2) return 0;
		if (i1 == null) return -1;
		if (i2 == null) return 1;
		
		int r = i1.first - i2.first;
		if (r != 0) return r;
		r = i1.last - i2.last;
		if (r != 0) return r;
		return icompare(i1.next, i2.next);
	}
	
	/**
	 * Implements the total order between character sets based on
	 * a lexicographic comparison of their intervals. In particular,
	 * the smallest character set wrt this order is {@link #EMPTY},
	 * and the largest is {@link #EOF}.
	 * 
	 * @param cs1
	 * @param cs2
	 * @return -1, 0, or 1 depending on whether {@code cs1} is smaller,
	 * 	equivalent or larger than {@code cs2}
	 */
	private static int compare(CSet cs1, CSet cs2) {
		// By canonicity, equivalent character sets are 
		// represented in the same manner interval-wise so
		// the order will be a morphism for equivalence
		if (cs1 == cs2) return 0;
		if (cs1 == EMPTY) return -1;
		if (cs2 == EMPTY) return 1;
		return Integer.signum(icompare(intervalsOf(cs1), intervalsOf(cs2)));
	}
	
	private static boolean iequivalent(
			@Nullable Interval i1, @Nullable Interval i2) {
		if (i1 == i2) return true;
		if (i1 == null || i2 == null) return false;
		if (i1.first != i2.first) return false;
		if (i1.last != i2.last) return false;
		return iequivalent(i1.next, i2.next);
	}
	
	/**
	 * @param cs1
	 * @param cs2
	 * @return {@code true} if and only if the two given
	 * 	character sets are equivalent
	 */
	public static boolean equivalent(CSet cs1, CSet cs2) {
		// By canonicity, equivalent character sets are 
		// represented in the same manner
		if (cs1 == cs2) return true;
		// Can't have cs1 and cs2 both equal to EMPTY or ALL
		if (cs1.getClass() != cs2.getClass()) return false;
		if (cs1 instanceof Singleton) {
			char c1 = ((Singleton) cs1).c;
			char c2 = ((Singleton) cs2).c;
			return c1 == c2;
		}
		return iequivalent(intervalsOf(cs1), intervalsOf(cs2));
	}
	
	/**
	 * @param cs1
	 * @param cs2
	 * @return {@code true} if and only if the character set
	 * {@code cs1} is included in {@code cs2}
	 */
	public static boolean included(CSet cs1, CSet cs2) {
		return diff(cs2, cs1) == CSet.EMPTY;
	}
	
	/*
	 * Generating random character sets
	 */
	
	/**
	 * Generates random character sets based on a
	 * probability {@linkplain Config configuration}
	 * and a {@linkplain Random random number generator}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Gen implements Generator<CSet> {

		/**
		 * Configuration of the generator
		 * 
		 * <i> Cumulative probabilities for the different 
		 * character set kinds must be increasing, since
		 * they are tested in the order of definition. </i>
		 * 
		 * @author Stéphane Lescuyer
		 */
		public static final class Config {
			/** Minimum char value generated */
			public char minChar = 16;
			/** Maximum char value generated */
			public char maxChar = 128;
 
			/** Probability to generate {@link CSet#EMPTY} */
			public float empty = 0.15f;
			/** Probability to generate {@link CSet#ALL} or {@link CSet#ALL_BUT_EOF} */
			public float all = 0.35f;
			/** Whether {@link CSet#ALL} or {@link CSet#ALL_BUT_EOF} should be generated */
			public boolean eof = true;
			/** Probability to generate singleton character set */
			public float singleton = 0.75f;
			/** Maximum number of character intervals generated */
			public int maxIntervals = 4;
			/** Maximum size of each generated interval */
			public int maxSize = 20;
		}
		
		private final Random random;
		private final Config config;

		/**
		 * Creates a character set generator based on the
		 * given random-number generator and configuration
		 * 
		 * @param random
		 * @param config
		 */
		public Gen(Random random, Config config) {
			this.random = random;
			this.config = config;
		}
		
		private Gen() {
			this(new Random(), new Config());
		}
		
		private char nextChar() {
			int k = random.nextInt(config.maxChar - config.minChar);
			return (char) (config.minChar + k);
		}
		
		@Override
		public CSet generate() {
			float f = random.nextFloat();
			if (f < config.empty) return EMPTY;
			if (f < config.all) return (config.eof ? ALL : ALL_BUT_EOF);
			if (f < config.singleton) return singleton(nextChar());
			
			// Generate n random intervals in [minChar, maxChar]
			// and take the reunion of these
			int n = 1 + random.nextInt(config.maxIntervals);
			@Nullable Interval interval = null;
			for (int i = 0; i < n; ++i) {
				char first = nextChar();
				char last = (char) (first + random.nextInt(config.maxSize));
				if (last > config.maxChar)
					last = config.maxChar;
				interval = iunion(new Interval(first, last, null), interval);
			}
			return intervals(interval);
		}
		
		@Override
		public String name() {
			return "Character set generation";
			// TODO: print config?
		}
	}
	/**
	 * @return a character set random generator
	 * 		based on default configuration
	 */
	public static Gen generator() {
		return new Gen();
	}
	
	/**
	 * @param cset
	 * @return an iterable sequence of (some, not necessary all) 
	 * 		   characters that belong to {@code cset}
	 */
	public static Iterable<Character> witnesses(CSet cset) {
		if (cset == EMPTY) return Iterables.empty();
		if (cset == ALL) return Iterables.singleton('a');
		if (cset instanceof Singleton) {
			char c = ((Singleton) cset).c;
			return Iterables.singleton(c);
		}
		Interval i = ((Intervals) cset).head;
		if (i == null) return Iterables.empty();
		return Iterables.singleton(i.first);
	}
}