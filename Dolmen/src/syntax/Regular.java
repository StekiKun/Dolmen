package syntax;

import common.CSet;

/**
 * Instances of {@link Regular} represent concrete
 * regular expressions. They are immutable objects,
 * and have therefore value semantics.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Regular {

	/**
	 * Enumeration which describes the different kinds of
	 * concrete implementations of {@link Regular}.
	 * The field {@link #witness} describes the associated
	 * concrete class.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@SuppressWarnings("javadoc")
	public static enum Kind {
		EPSILON(Epsilon.class),
		EOF(Eof.class),
		CHARACTERS(Characters.class), 
		ALTERNATE(Alternate.class), 
		SEQUENCE(Sequence.class), 
		REPETITION(Repetition.class),
		BINDING(Binding.class);
		
		public final Class<?> witness;
		private Kind(Class<?> clazz) {
			this.witness = clazz;
		}
	}
	
	/**
	 * Specifies what kind of regular expression {@code this} is
	 */
	public final Kind kind;
	
	/**
	 * If {@code size} >= 0, all strings matching this
	 * regular expression have the specified size.
	 * If {@code size} is negative, then no size is
	 * guaranteed.
	 */
	public final int size;
	
	/**
	 * Whether this regular expressions has any 
	 * bound submatching group or not
	 */
	public final boolean hasBindings;
	
	/**
	 * Whether this regular expression matches the empty string.
	 * <p>
	 * <i>Note that the end-of-file regular expression does not
	 * match the empty string, it matches a special character 
	 * instead.</i>
	 */
	public final boolean nullable;
	
	private Regular(Kind kind, 
			int size, boolean hasBindings, boolean nullable) {
		this.kind = kind;
		assert (kind.witness == this.getClass());
		this.size = size;
		this.hasBindings = hasBindings;
		this.nullable = nullable;
	}
	
	/**
	 * An instance of {@link Folder} is a set of methods to
	 * apply to the various cases of a regular expression, depending
	 * on its form, as part of a (possibly) recursive traversal of 
	 * the regular expression structure. It is the natural 
	 * <i>catamorphism</i> of the tree-like structure of regular
	 * expressions, and a generalization of the traditional 
	 * Java <i>visitor</i>.
	 * 
	 * @author Stéphane Lescuyer
	 *
	 * @param <V> the resulting type of applying this folder
	 */
	public static interface Folder<V> {
		/** Applying this on {@link Regular#EPSILON} */
		public V epsilon();
		/** Applying this on {@link Regular#EOF} */
		public V eof();
		/** Applying this on a {@link Characters} regular expression */
		public V chars(Characters chars);
		/** Applying this on an {@link Alternate} regular expression */
		public V alternate(Alternate alt);
		/** Applying this on a {@link Sequence} regular expression */
		public V sequence(Sequence seq);
		/** Applying this on a {@link Repetition} regular expression */
		public V repetition(Repetition rep);
		/** Applying this on a {@link Binding} regular expression */
		public V binding(Binding binding);
	}
	
	/**
	 * @param folder
	 * @return the result of applying the provided {@link Folder}
	 * 		to the receiver
	 */
	public abstract <V> V fold(Folder<V> folder);

	/**
	 * The singleton class that stands for the empty 
	 * regular expression ε, which only matches the empty string.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Epsilon extends Regular {
		protected static final Epsilon INSTANCE = new Epsilon();
		
		private Epsilon() {
			super(Kind.EPSILON, 0, false, true);
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.epsilon();
		}
	}
	/**
	 * The empty regular expression ε
	 */
	public static final Epsilon EPSILON = Epsilon.INSTANCE;
	
	/**
	 * The singleton class that stands for the regular expression 
	 * matching the "end of input", or EOF, which only matches
	 * when the input string has been entirely consumed.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Eof extends Regular {
		protected static final Eof INSTANCE = new Eof();
		
		private Eof() {
			super(Kind.EOF, 0, false, false);
		}
		
		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.eof();
		}
	}
	/**
	 * The regular expression matching the end-of-file/end-of-input
	 */
	public static final Eof EOF = Eof.INSTANCE;
	
	/**
	 * Instances of regular expressions that match exactly
	 * one character amongst a set of possible characters
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Characters extends Regular {
		/**
		 * The character set matched by this expression
		 */
		public final CSet chars;
		
		private Characters(CSet chars) {
			super(Kind.CHARACTERS, 1, false, false);
			this.chars = chars;
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.chars(this);
		}
	}
	/**
	 * @param chars
	 * @return a regular expression which matches exactly
	 * 		one character in the character set {@code chars}
	 */
	public static Regular chars(CSet chars) {
		if (chars.isEmpty()) return EPSILON;
		return new Characters(chars);
	}
	
	/**
	 * Instances of regular expressions that represent a choice
	 * {@code a | b} between two regular expressions.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Alternate extends Regular {
		/** The left-hand side of the choice operator */
		public final Regular lhs;
		/** The right-hand side of the choice operator */
		public final Regular rhs;
		
		private Alternate(Regular lhs, Regular rhs) {
			super(Kind.ALTERNATE,
					(lhs.size == rhs.size && lhs.size >= 0) ?
					lhs.size : -1,
				  lhs.hasBindings || rhs.hasBindings,
				  lhs.nullable || rhs.nullable);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.alternate(this);
		}
	}
	/**
	 * @param lhs
	 * @param rhs
	 * @return the regular expression representing the
	 * 		alternative of {@code lhs} and {@code rhs},
	 * 		in this order
	 */
	public static Regular or(Regular lhs, Regular rhs) {
		if (lhs == rhs) return lhs;
		return new Alternate(lhs, rhs);
	}

	/**
	 * Instances of regular expressions that represent the
	 * concatenation {@code ab} of two regular expressions.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Sequence extends Regular {
		/** First part of the concatenation */
		public final Regular first;
		/** Second part of the concatenation */
		public final Regular second;
		
		private Sequence(Regular first, Regular second) {
			super(Kind.SEQUENCE,
					first.size < 0 || second.size < 0 ? - 1 :
					first.size + second.size,
				  first.hasBindings || second.hasBindings,
				  first.nullable && second.nullable);
			this.first = first;
			this.second = second;
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.sequence(this);
		}
	}
	/**
	 * @param first
	 * @param second
	 * @return the regular expression representing the
	 * 		concatenation of {@code first} and {@code second},
	 * 		in this order
	 */
	public static Regular seq(Regular first, Regular second) {
		if (first == EPSILON) return second;
		if (second == EPSILON) return first;
		return new Sequence(first, second);
	}
	
	/**
	 * Instances of regular expressions that represent the Kleene
	 * closure {@code r*} of some regular expression {@code r},
	 * i.e. matching zero, one or more repetitions of {@code r}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Repetition extends Regular {
		/** The regular expression to repeat */
		public final Regular reg;
		
		private Repetition(Regular reg) {
			super(Kind.REPETITION, -1, reg.hasBindings, true);
			this.reg = reg;
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.repetition(this);
		}
	}
	/**
	 * @param reg
	 * @return the regular expression representing any
	 * 		number of repetitions of {@code reg}
	 */
	public static Regular star(Regular reg) {
		if (reg == EPSILON) return EPSILON;
		return new Repetition(reg);
	}
	
	/**
	 * Instances of regular expressions that represent
	 * expressions whose matchers will be bound to
	 * a specified name
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Binding extends Regular {
		/** The bound regular expression */
		public final Regular reg;
		/** 
		 * The name to which the string matched by
		 * this regular expression should be bound
		 */
		public final String name;
		/** The location of {@link #name} */
		public final Location loc;
		
		private Binding(Regular reg, String name, Location loc) {
			super(Kind.BINDING, reg.size, true, reg.nullable);
			this.reg = reg;
			this.name = name;
			this.loc = loc;
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.binding(this);
		}
	}
	/**
	 * @param reg
	 * @param name
	 * @param loc
	 * @return a regular expression with a binding to {@code name}
	 */
	public static Binding binding(Regular reg, String name, Location loc) {
		return new Binding(reg, name, loc);
	}
}