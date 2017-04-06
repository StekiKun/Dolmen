package common;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface of (lazy) generators
 * 
 * @author Stéphane Lescuyer
 *
 * @param <T> the type of values generated by the generator
 */
public interface Generator<T> extends Supplier<T> {

	/**
	 * @return a user-friendly description of the generator
	 */
	public String name();
	
	/**
	 * @return the next generated value
	 * @throws NoSuchElementException if no more values can be generated
	 */
	public T generate();
	
	@Override
	default public T get() {
		return generate();
	}
	
	/**
	 * @return a Java 8 {@link Stream} based on this generator
	 */
	default public Stream<T> toStream() {
		@SuppressWarnings("null")
		@NonNull Stream<T> res = Stream.generate(this);
		return res;
	}
	
	/**
	 * Presents the values generated by this generator
	 * on standard output, running the provided consumer
	 * on each generated value
	 * @param consumer
	 */
	default public void present(Consumer<T> consumer) {
		System.out.println("Generator: " + name());
		System.out.println("<Hit Enter to keep generating, type a number to generate");
		System.out.println(" many samples at once, and type q[uit] to stop>");
		int i = 0;
		String line;
		present:
		while ((line = Prompt.getInputLine("")) != null) {
			if (line.equals("q") || line.equals("quit")) break;
			int nsamples = 1;
			if (!line.isEmpty()) {
				try (Scanner sc = new Scanner(line)) {
					if (!sc.hasNextInt()) continue;
					nsamples = sc.nextInt();
					if (sc.hasNext()) continue;
				}
			}
			for (int j = 0; j < nsamples; ++j) {
				try {
					T t = generate();
					System.out.println("[" + i++ + "] " + t.toString());
					consumer.accept(t);
				} catch (NoSuchElementException e) {
					break present;
				}
			}
		}
	}
	
	/**
	 * Presents the values generated by this generator
	 * on standard output
	 */
	default public void present() {
		present(t -> {});
	}
	
	/**
	 * @param name
	 * @param it
	 * @return a generator with the given name which
	 * 		generates the elements of {@code it}, in order,
	 * 		and stops generating once {@code it} is exhausted
	 */
	public static <T> Generator<T>
		ofIterable(String name, Iterable<? extends T> it) {
		final Iterator<? extends T> iterator = it.iterator();
		return new Generator<T>() {
			@Override
			public @NonNull String name() {
				return name;
			}

			@Override
			public T generate() {
				return iterator.next();
			}
		};
	}
}