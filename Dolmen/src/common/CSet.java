package common;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A class to represent character sets.
 * 
 * Characters are Java characters, i.e. 16-bit unsigned integers.
 * Character sets are mostly encoded as an ordered list of
 * character intervals, with some special values for degenerate
 * cases, and are thus optimized for space rather than for lookup speed.
 * 
 * Instances of this class are immutable.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class CSet {

	/**
	 * @return {@code true} if this character set is empty
	 */
	public abstract boolean isEmpty();
	
	/**
	 * @param ch
	 * @return whether this character set contains {@code ch}
	 */
	public abstract boolean contains(char ch);
	
	@Override
	public abstract String toString();
	
	/**
	 * The set of <b>all</b> possible characters
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
		public String toString() {
			return "_";
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
		public String toString() {
			return "∅";
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
		public @NonNull String toString() {
			return "" + c;
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
				buf.append(first);
			else
				buf.append(first + "-" + last);
			if (next != null) {
				next.append(buf);
			}
			return buf;
		}
		
		@SuppressWarnings("null")
		@Override
		public String toString() {
			return append(new StringBuilder()).toString();
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
		final Interval head;

		private Intervals(Interval head) {
			this.head = head;
		}
		
		@Override
		public boolean isEmpty() {
			return false;	// head is non-null
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
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("[");
			head.append(buf);
			buf.append("]");
			@SuppressWarnings("null")
			@NonNull String res = buf.toString();
			return res;
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
		return new Intervals(new Interval(first, last, null));
	}
}