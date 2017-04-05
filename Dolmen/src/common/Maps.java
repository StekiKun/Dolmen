package common;

import java.util.Collections;
import java.util.Map;

/**
 * This class contains various utility methods
 * about {@link Map}s.
 * 
 * <i>I wished I had Guava available!</i>
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Maps {

	private Maps() {
		// Static utility only
	}

	/**
	 * @return the empty unmodifiable map
	 */
	@SuppressWarnings("null")
	public static <K, V> Map<K, V> empty() {
		return Collections.emptyMap();
	}

}
