package common;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

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
	@SuppressWarnings("null")
	public static <T> List<T> empty() {
		return Collections.emptyList();
	}

	/**
	 * @param elt
	 * @return the singleton list containing {@code elt}
	 */
	@SuppressWarnings("null")
	public static <T> @NonNull List<T> singleton(T elt) {
		return Collections.singletonList(elt);
	}
	
}