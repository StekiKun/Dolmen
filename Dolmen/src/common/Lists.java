package common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * This class contains various utility methods
 * about {@link List}s.
 * 
 * <i>I wished I had Guava available!</i>
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Lists {

	private Lists() {
		// Static utility only
	}
	
	/**
	 * @return the empty unmodifiable list
	 */
	public static <T> List<T> empty() {
		return Collections.emptyList();
	}

	/**
	 * @param elt
	 * @return the singleton list containing {@code elt}
	 */
	public static <T> List<T> singleton(T elt) {
		return Collections.singletonList(elt);
	}
	
	/**
	 * <b>This is not a view, it returns a new list.</b>
	 * 
	 * @param l
	 * @param f
	 * @return the list {@code l} where all elements
	 * 	have been transformed by the function {@code f}
	 */
	public static <T, U> List<U> transform(
		final List<? extends T> l, final Function<? super T, ? extends U> f) {
		List<U> res = new ArrayList<>(l.size());
		for (T t : l)
			res.add(f.apply(t));
		return res;
	}
	
}