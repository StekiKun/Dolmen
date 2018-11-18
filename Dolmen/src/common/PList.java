package common;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Nullable;

/**
 * This class implements polymorphic <i>persistent lists</i>. 
 * Instances of persistent lists are built mainly by
 * adding an element in front of an existing list with {@link #cons(Object, PList)},
 * starting from the empty list {@link #empty()}. They are immutable
 * and allow (thread-)safe physical sharing of suffixes.
 * <p>
 * This implementation allows {@code null} elements, in other
 * words the type of elements {@code A} can be {@link Nullable}.
 * 
 * @author Stéphane Lescuyer
 *
 * @param <A>	the type of elements stored in the list
 */
public final class PList<A> implements Iterable<A> {
	
	private final int size;
	private final PList<A> tail;
	private final A head;
	
	private static final PList<Object> NIL = new PList<Object>();
	
	@SuppressWarnings("unchecked")
	private PList() {
		this.size = 0;
		// Using NIL and some dummy object to get around the nullable analysis
		// without having to make the [tail] and [head] fields nullable
		this.tail = (PList<A>) NIL;
		this.head = (A) new Object();
	}
	
	private PList(A head, PList<A> tail) {
		this.size = tail.size + 1;
		this.tail = tail;
		this.head = head;
	}
	
	/**
	 * @return the empty list
	 */
	@SuppressWarnings("unchecked")
	public static <A> PList<A> empty() {
		return (PList<A>) NIL;
	}

	/**
	 * @param head
	 * @param tail
	 * @return the list obtained by adding {@code head}
	 * 	in front of {@code tail}
	 */
	public static <A> PList<A> cons(A head, PList<A> tail) {
		return new PList<A>(head, tail);
	}
	
	/**
	 * @param elt
	 * @return the singleton list consisting of {@code elt} alone
	 */
	public static <A> PList<A> singleton(A elt) {
		return cons(elt, empty());
	}
	
	/**
	 * @return the head of this list
	 * @throws NoSuchElementException if the list is empty
	 */
	public A hd() {
		if (this == NIL) throw new NoSuchElementException();
		return head;
	}
	
	/**
	 * @return the tail of this list
	 * @throws NoSuchElementException if the list is empty
	 */
	public PList<A> tl() {
		if (this == NIL) throw new NoSuchElementException();
		return tail;
	}
	
	/**
	 * @return the length of this list, i.e. the number 
	 * 	of elements in the list
	 */
	public int length() {
		return size;
	}
	
	/**
	 * @return {@code true} if this list is empty
	 */
	public boolean isEmpty() {
		return this == NIL;
	}
	
	/**
	 * <i>This works in constant stack size.</i>
	 * 
	 * @param l1
	 * @param l2
	 * @return the list obtained by adding all the elements
	 * 	of {@code l1}, <b>in reverse order</b>, in front of the
	 * 	elements of {@code l2}
	 */
	public static <A> PList<A> revAppend(PList<A> l1, PList<A> l2) {
		PList<A> cur1 = l1;
		PList<A> res = l2;
		while (cur1 != NIL) {
			res = cons(cur1.head, res);
			cur1 = cur1.tail;
		}
		return res;
	}
	
	/**
	 * <i>This works in constant stack size.</i>
	 *
	 * @param l
	 * @return the list obtained by reversing the elements of {@code l}
	 */
	public static <A> PList<A> rev(PList<A> l) {
		return revAppend(l, empty());
	}
	
	/**
	 * <i>This works in constant stack size.</i>
	 * 
	 * @param l1
	 * @param l2
	 * @return the list obtained by adding all the elements of
	 * 	{@code l1}, in order, in front of the elements of {@code l2}
	 */
	public static <A> PList<A> concat(PList<A> l1, PList<A> l2) {
		return revAppend(rev(l1), l2);
	}
	
	@Override
	public int hashCode() {
		if (this == NIL) return 1;
		int result = 1;
		PList<A> cur = this;
		while (cur != NIL) {
			result = 31 * result + Objects.hashCode(cur.head);
			cur = cur.tail;
		}
		return result;
	}
	
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (this == NIL) return false;
		if (!(o instanceof PList<?>)) return false;
		PList<?> plist = (PList<?>) o;
		if (this.size != plist.size) return false;
		
		PList<A> cur = this;
		PList<?> ocur = plist;
		while (cur != NIL) {
			if (!Objects.equals(cur.head, ocur.head))
				return false;
			cur = cur.tail;
			ocur = ocur.tail;
		}
		return true;
	}
	
	@Override
	public String toString() {
		if (this == NIL) return "[]";
		StringBuilder buf = new StringBuilder();
		buf.append("[");
		PList<A> cur = this;
		boolean first = true;
		while (cur != NIL) {
			if (first) first = false;
			else buf.append(", ");
			buf.append(Objects.toString(cur.head));
			cur = cur.tail;
		}
		buf.append("]");
		return buf.toString();
	}
	
	/**
	 * Implements an {@link Iterator} for persistent lists
	 * 
	 * @author Stéphane Lescuyer
	 *
	 * @param <A>
	 */
	private final static class PListIterator<A> implements Iterator<A> {
		private PList<A> current;
		
		PListIterator(PList<A> plist) {
			this.current = plist;
		}

		@Override
		public boolean hasNext() {
			return current != NIL;
		}

		@Override
		public A next() {
			if (current == NIL) throw new NoSuchElementException();
			A res = current.head;
			current = current.tail;
			return res;
		}
	}

	@Override
	public Iterator<A> iterator() {
		if (this == NIL) return Collections.emptyIterator();
		return new PListIterator<A>(this);
	}
	
	@Override
	public void forEach(@Nullable Consumer<? super A> consumer) {
		if (consumer == null) 
			throw new NullPointerException();
		PList<A> cur = this;
		while (cur != NIL) {
			consumer.accept(cur.head);
			cur = cur.tail;
		}
	}
	
	/**
	 * Simple tests for {@link PList}
	 * 
	 * @param args
	 */
	public static final void main(String[] args) {
		PList<Object> nil = NIL;
		
		System.out.println(nil);
		System.out.println(cons(1, cons(2, empty())));
		System.out.println(cons(2, nil));
		
		for (int k : cons(1, cons(3, cons(4, empty())))) {
			System.out.println("" + k);
		}
		
		System.out.println(rev(cons(1, cons(2, cons(4, empty())))));
		System.out.println(concat(
			cons(1, cons(2, cons(4, empty()))),
			cons(5, cons(6, empty()))));
	}
}
