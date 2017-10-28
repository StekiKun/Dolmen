package syntax;

import java.util.Random;

import org.eclipse.jdt.annotation.NonNull;

import common.CSet;
import common.Generator;
import common.Hierarchy;

/**
 * Instances of {@link Regular} represent concrete
 * regular expressions. They are immutable objects,
 * and have therefore value semantics.
 * 
 * @author Stéphane Lescuyer
 */
@Hierarchy("getKind")
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
	 * <i>Only there for the quick-assist based on {@link Hierarchy}.
	 * 	You should probably use the field {@link #kind} directly.</i>
	 * 
	 * @return {@link #kind}
	 */
	public final Kind getKind() {
		return kind;
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

	@Override public abstract @NonNull String toString();
	
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

		@Override
		public @NonNull String toString() {
			return "ε";
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

		@Override
		public @NonNull String toString() {
			return "EOF";
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

		@Override
		public @NonNull String toString() {
			return chars.toString();
		}
	}
	/**
	 * @param chars
	 * @return a regular expression which matches exactly
	 * 		one character in the character set {@code chars}
	 */
	public static Regular chars(CSet chars) {
		return new Characters(chars);
	}
	/**
	 * @param s
	 * @return a regular expression matching exactly
	 * 	the given string {@code s}
	 */
	public static Regular string(String s) {
		Regular[] chars = new Regular[s.length()];
		for (int i = 0; i < s.length(); ++i)
			chars[i] = chars(CSet.singleton(s.charAt(i)));
		// All elements have been initialized to a non-null value
		@SuppressWarnings("null")
		@NonNull Regular[] allChars = (@NonNull Regular[]) chars;
		return seq(allChars);
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

		@Override
		public @NonNull String toString() {
			return "(" + lhs.toString() + "|" + rhs.toString() + ")";
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
		if (lhs instanceof Characters &&
			rhs instanceof Characters) {
			return chars(CSet.union(
					((Characters) lhs).chars, 
					((Characters) rhs).chars));
		}
		return new Alternate(lhs, rhs);
	}
	
	/**
	 * @param reg1
	 * @param regs
	 * @return the regular expression 
	 *   {@code (((reg1 || regs[0]) || regs[1]) ... || regs[n-1]}
	 */
	public static Regular or(Regular reg1, @NonNull Regular... regs) {
		Regular res = reg1;
		for (int i = 0; i < regs.length; ++i)
			res = or(res, regs[i]);
		return res;
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

		@Override
		public @NonNull String toString() {
			return first.toString() + second.toString();
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
	 * @param regs
	 * @return the concatenation of all regular expressions
	 * 	in {@code regs}, in order
	 */
	public static Regular seq(@NonNull Regular... regs) {
		Regular res = EPSILON;
		for (int i = 0; i < regs.length; ++i)
			res = seq(res, regs[i]);
		return res;
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

		@Override
		public @NonNull String toString() {
			return "(" + reg.toString() + ")*";
		}
	}
	/**
	 * @param reg
	 * @return the regular expression representing any
	 * 		number of repetitions of {@code reg}
	 */
	public static Regular star(Regular reg) {
		if (reg == EPSILON) return EPSILON;
		 if (reg instanceof Repetition) return reg;
		return new Repetition(reg);
	}
	
	/**
	 * @param reg
	 * @return the regular expression one or more
	 * 	repetitions of {@code reg}
	 */
	public static Regular plus(Regular reg) {
		return seq(reg, star(reg));
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
		public final Located<String> name;
		
		private Binding(Regular reg, Located<String> name) {
			super(Kind.BINDING, reg.size, true, reg.nullable);
			this.reg = reg;
			this.name = name;
		}

		@Override
		public <V> V fold(Folder<V> folder) {
			return folder.binding(this);
		}

		@Override
		public @NonNull String toString() {
			return "(" + reg.toString() + " as " + name.val + ")";
		}
	}
	/**
	 * @param reg
	 * @param name
	 * @return a regular expression with a binding to {@code name}
	 */
	public static Binding binding(Regular reg, Located<String> name) {
		return new Binding(reg, name);
	}
	
	/*
	 * Generating random regular expressions
	 */
	
	/**
	 * Generates random regular expressions based on a
	 * probability {@link Config configuration}
	 * and a {@link Random random number generator}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Gen implements Generator<Regular> {
		
		/**
		 * Configuration of the generator
		 * 
		 * <i> Probabilities specified for each kind of
		 * regular expressions are cumulative, can be set
		 * individually but must be increasing, since they
		 * are tested in the order of definition. </i>
		 * 
		 * @author Stéphane Lescuyer
		 */
		public static final class Config {
			/** Probability to generate {@link Regular#EPSILON} */
			public float epsilon = 0.05f;
			/** Probability to generate {@link Regular#EOF} */
			public float eof = 0.10f;
			/** Probability to generate a regexp matching some character set */
			public float chars = 0.40f;
			/** Probability to generate a regular expression of the form "a | b" */
			public float alternate = 0.60f;
			/** Probability to generate a regular expression of the form "ab" */
			public float sequence = 0.80f;
			/** Probability to generate a regular expression of the form "a*" */
			public float repetition = 0.92f;
			
			/** Maximum depth of a generated regular expression */
			public int maxDepth = 8;
			/** Configuration used to generate the random character sets */
			public CSet.Gen.Config csConfig = new CSet.Gen.Config();
		}
		
		private final Random random;
		private final Config config;
		private final CSet.Gen csetGenerator;
		
		/**
		 * Creates a regular expression generator based on the
		 * given random-number generator and configuration
		 * 
		 * @param random
		 * @param config
		 */
		public Gen(Random random, Config config) {
			this.random = random;
			this.config = config;
			// Use easy characters
			config.csConfig.minChar='A';
			config.csConfig.maxChar='z';
			this.csetGenerator = new CSet.Gen(random, config.csConfig);
		}
		
		private Gen() {
			this(new Random(), new Config());
		}

		@Override
		public @NonNull String name() {
			return "Regular expression generation";
		}

		private Regular genAux(int curDepth) {
			// Force a leaf (in the form of a character set) if at max depth
			if (config.maxDepth == curDepth)
				return chars(csetGenerator.generate());
			
			float f = random.nextFloat();
			if (f < config.epsilon) return EPSILON;
			if (f < config.eof) return EOF;
			if (f < config.chars) return chars(csetGenerator.generate());
			if (f < config.alternate) {
				return or(genAux(curDepth + 1), genAux(curDepth + 1));
			}
			if (f < config.sequence) {
				return seq(genAux(curDepth + 1), genAux(curDepth + 1));
			}
			if (f < config.repetition) {
				return star(genAux(curDepth + 1));
			}
			// Otherwise generate a binding regular expression
			char c1 = (char) ('a' + random.nextInt(4));
			String name = "" + c1;
			// I am reducing the number of possible names to increase
			// the probability of capture/shadowing
//			if (random.nextBoolean())
//				name += (char) ('a' + random.nextInt(4));
			return binding(genAux(curDepth + 1), Located.dummy(name));
		}
		
		@Override
		public @NonNull Regular generate() {
			return genAux(0);
		}
		
	}
	/**
	 * @return a regular expression generator random
	 * 		generator based on a default configuration
	 */
	public static Gen generator() {
		return new Gen();
	}
}