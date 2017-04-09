package common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

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
	
	/**
	 * 
	 * @param k
	 * @param v
	 * @return the singleton hash map binding {@code k} to {@code v}
	 */
	public static <K, V> Map<K, V> singleton(K k, V v) {
		return new HashMap<>(Collections.singletonMap(k, v));
	}

	/**
	 * <i>This is a strongly-typed alternative to {@link Map#get(Object)},
	 * 	which restricts the type of the given key to a sub-type of
	 * 	the declared key type of the map, and also specifies the
	 * 	returned value can be {@code null}. For some reason, the JDT
	 *  seemed to believe the result of {@link Map#get(Object)} was
	 *  non-null and this was leading to some bogus dead-code warnings.</i>
	 * 
	 * @param m
	 * @param key
	 * @return the element associated to {@code key} in {@code m},
	 * 		or {@code null} if there is no such element
	 */
	public static <K, V> @Nullable V get(Map<? super K, V> m, K key) {
		return m.get(key);
	}
}
