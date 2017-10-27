package common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
				while (!(currentHasNext = current.hasNext())
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
		
		return new Iterable<T>() {
			@SuppressWarnings("null")
			@Override
			public Iterator<T> iterator() {
				return concatIterator(
					transformIterator(iterables.iterator(),
						iterable -> iterable.iterator()));
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
		final List<Iterable<? extends T>> iterables = Arrays.asList(its);
		return new Iterable<T>() {
			@SuppressWarnings("null")
			@Override
			public Iterator<T> iterator() {
				List<Iterator<? extends T>> iterators =
					new ArrayList<>(iterables.size());
				for (Iterable<? extends T> it : iterables)
					iterators.add(it.iterator());
				return concatIterator(iterators.iterator());
			}
		};
	}

	/**
	 * @param it
	 * @param f
	 * @return an iterator based on {@code it} where all
	 * 		elements have been transformed by {@code f}
	 */
	private static <T, U> Iterator<U> transformIterator(
		Iterator<? extends T> it, Function<? super T, ? extends U> f) {
		return new Iterator<U>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public U next() {
				T t = it.next();
				return f.apply(t);
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
				@SuppressWarnings("null")
				@NonNull Iterator<? extends T> iterator = it.iterator();
				return transformIterator(iterator, f);
			}
		};
	}

	/**
	 * @param iterable
	 * @param clazz
	 * @return the given {@code iterable} filtered to only contain elements
	 * 	of the class whose descriptor is {@code clazz}
	 */
	public static <T, U extends T> Iterable<@NonNull U>
		filterClass(Iterable<@NonNull T> iterable, Class<? extends U> clazz) {
		return new Iterable<@NonNull U>() {
			@Override
			public Iterator<@NonNull U> iterator() {
				final Iterator<@NonNull T> it = iterable.iterator();
				
				return new Iterator<@NonNull U>() {
					// Next filtered available actual, if any
					@Nullable U next;
					
					@SuppressWarnings("unchecked")
					@Override
					public boolean hasNext() {
						if (next != null) return true;
						while (it.hasNext()) {
							T item = it.next();
							if (item.getClass() == clazz) {
								next = (U) item;
								return true;
							}
						}
						return false;
					}

					@Override
					public @NonNull U next() {
						hasNext();
						@Nullable U res = next;
						next = null;
						if (res != null) return res;
						else throw new NoSuchElementException();
					}
				};
			}
		};
	}
	
	/**
	 * @param it
	 * @return the number of elements in the iterable {@code it}
	 */
	public static <T> int size(Iterable<T> it) {
		int count = 0;
		for (@SuppressWarnings("unused") T t : it)
			++count;
		return count;
	}
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		List<Integer> l = new java.util.ArrayList<Integer>();
//		l.add(5); l.add(99); l.add(66);
//		
//		for (int k : Iterables.singleton(55))
//			System.out.println("" + k);
//		
//		List<Iterable<Integer>> lits = new java.util.ArrayList<>();
//		lits.add(Arrays.asList(1, 2));
//		lits.add(Arrays.asList(1));
//		lits.add(Iterables.empty());
//		lits.add(Iterables.singleton(23));
//		lits.add(l);
//		for (int k : Iterables.concat(lits))
//			System.out.println("" + k);
//		
//		for (int k :
//			Iterables.<Integer> concat(
//				Arrays.asList(1, 2),
//				Arrays.asList(1),
//				Iterables.empty(),
//				Iterables.singleton(23),
//				l))
//			System.out.println("" + k);
//		
//		List<Object> objs = new java.util.ArrayList<Object>();
//		objs.add(12); objs.add(5);
//		objs.add(-.45f); objs.add("youpi");
//		objs.add(12.3f); objs.add("hey ho");
//		
//		for (int i : filterClass(objs, Integer.class))
//			System.out.println("Int: " + i);
//		for (float f : filterClass(objs, Float.class))
//			System.out.println("float: " + f);
//		for (String s : filterClass(objs, String.class))
//			System.out.println("Int: " + s);
//
//	}

}