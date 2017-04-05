package common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class contains various utility
 * methods about {@link Iterable}s.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Iterables {

	private Iterables() {
		// Static utility only
	}

	/**
	 * @return the empty iterable
	 */
	public static <T> Iterable<T> empty() {
		return Sets.empty();
	}

	/**
	 * @param elt
	 * @return the iterable containing the single element {@code elt}
	 */
	public static <T> Iterable<T> singleton(T elt) {
		return Sets.singleton(elt);
	}
	
	/**
	 * @param iterators
	 * @return a concatenation of all iterators in {@code iterators}
	 * 		in order
	 */
	private static <T> Iterator<T> concatIterator(
		Iterator<? extends Iterator<? extends T>> iterators) {
		return new Iterator<T>() {
			@SuppressWarnings("null")
			private Iterator<? extends T> current = Collections.emptyIterator();
			
			@Override
			public boolean hasNext() {
				boolean currentHasNext;
				while ((currentHasNext = current.hasNext())
						&& iterators.hasNext()) {
					current = iterators.next();
				}
				return currentHasNext;
			}

			@Override
			public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return current.next();
			}	
		};
	}

	/**
	 * @param iterables
	 * @return an {@link Iterable} representing the concatenation
	 * 	of all iterables in {@code iterables}, in their relative order
	 */
	public static <T> Iterable<T> concat(
		Iterable<? extends Iterable<? extends T>> iterables) {
		Stream<? extends Iterable<? extends T>> stream =
			StreamSupport.stream(iterables.spliterator(), false);
		return new Iterable<T>() {
			@SuppressWarnings("null")
			@Override
			public Iterator<T> iterator() {
				return concatIterator(stream
										  .map(it -> it.iterator())
										  .iterator());
			}
		};
	}

	/**
	 * @param its
	 * @return an {@link Iterable} representing the concatenation
	 * 	of all iterables in {@code its}, in their relative order
	 */
	@SafeVarargs
	public static <T> Iterable<T> concat(Iterable<? extends T>... its) {
		@SuppressWarnings("null")
		List<Iterable<? extends T>> iterables = Arrays.asList(its);
		return new Iterable<T>() {
			@SuppressWarnings("null")
			@Override
			public Iterator<T> iterator() {
				return concatIterator(iterables.stream()
										  .map(it -> it.iterator())
										  .iterator());
			}
		};
	}
	
	/**
	 * @param it
	 * @param f
	 * @return a transformed iterable where the function
	 * 	{@code f} has been applied to all elements in {@code it}
	 */
	public static <T, U> Iterable<U> transform(
		Iterable<? extends T> it, Function<? super T, ? extends U> f) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> iterator = it.iterator();
				return new Iterator<U>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public U next() {
						T t = iterator.next();
						return f.apply(t);
					}
				};
			}
		};
	}
}
