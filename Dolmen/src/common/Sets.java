package common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains various utility methods
 * about {@link Set}s.
 * 
 * <b>The various operations like {@link #union(Set, Set)}
 * and so on assume an immutable use of the sets used as
 * parameters, since they are optimized to return one of
 * the input sets instead of new sets when possible.</b>
 * 
 * <i>I wished I had Guava available!</i>
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Sets {

	private Sets() {
		// Static utility only
	}
	
	/**
	 * @return the empty unmodifiable set
	 */
	@SuppressWarnings("null")
	public static <T> Set<T> empty() {
		return Collections.emptySet();
	}

	/**
	 * @param elt
	 * @return the singleton set containing {@code elt}
	 */
	@SuppressWarnings("null")
	public static <T> Set<T> singleton(T elt) {
		return Collections.singleton(elt);
	}	
	
	/**
	 * @param elt
	 * @param s
	 * @return a set where {@code x} has been added
	 * 	to the elements of {@code s}
	 */
	public static <T> Set<T> add(T elt, Set<T> s) {
		if (s.contains(elt)) return s;
		Set<T> res = new HashSet<T>(s.size() + 1);
		res.addAll(s);
		res.add(elt);
		return res;
	}
	
	/**
	 * @param s1
	 * @param s2
	 * @return the union of {@code s1} and {@code s2}
	 */
	public static <T> Set<T> union(Set<T> s1, Set<T> s2) {
		if (s1.isEmpty()) return s2;
		if (s2.isEmpty()) return s1;
		Set<T> res = new HashSet<T>(s1);
		res.addAll(s2);
		return res;
	}
	
	/**
	 * @param s1
	 * @param s2
	 * @return the intersection of {@code s1} and {@code s2}
	 */
	public static <T> Set<T> inter(Set<T> s1, Set<T> s2) {
		if (s1.isEmpty() || s2.isEmpty())
			return s1;
		Set<T> res = new HashSet<T>(s1);
		res.retainAll(s2);
		if (res.isEmpty()) return empty();
		return res;
	}

	/**
	 * @param s1
	 * @param s2
	 * @return the set of elements in {@code s1} and not in {@code s2}
	 */
	public static <T> Set<T> diff(Set<T> s1, Set<T> s2) {
		if (s1.isEmpty() || s2.isEmpty()) return s1;
		Set<T> res = new HashSet<T>(s1);
		res.removeAll(s2);
		if (res.isEmpty()) return empty();
		return res;
	}

	/**
	 * @param s1
	 * @param s2
	 * @return the symmetric difference between {@code s1} and {@code s2}
	 */
	public static <T> Set<T> symdiff(Set<T> s1, Set<T> s2) {
		if (s1.isEmpty()) return s2;
		if (s2.isEmpty()) return s1;
		return diff(union(s1, s2), inter(s1, s2));
	}
}
