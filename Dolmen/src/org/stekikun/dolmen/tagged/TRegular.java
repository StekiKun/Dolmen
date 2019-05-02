package org.stekikun.dolmen.tagged;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Hierarchy;

/**
 * Instances of {@link TRegular} represent tagged
 * regular expressions. 
 * <p>
 * Tagged regular expressions are an intermediate form 
 * of compiled regular expressions where bindings have
 * been replaced by pair of "tags" enclosing the bound
 * parts of regular expressions, and where character sets
 * are replaced by indices in an external map of character 
 * sets. When a bounded expression is guaranteed to match
 * only single characters, it is marked by a single starting
 * tag and has no closing tag.
 * <p>
 * In a latter phase, they can also reference 
 * <i>semantic actions</i> to be executed at the point
 * where they are encountered when matching the expression.
 * <p>
 * They are immutable objects, and have therefore 
 * value semantics.
 * 
 * @author Stéphane Lescuyer
 */
@Hierarchy("getKind")
public abstract class TRegular {

	/**
	 * Describes a <i>tag</i>, i.e. a marker
	 * used in {@linkplain TRegular tagged regular expressions}
	 * to represent the boundaries of bound sub-expressions
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TagInfo {
		/**
		 * The binding associated to this tag
		 */
		public final String id;
		/**
		 * Whether this tag marks the start or
		 * the end of the bound sub-expressions
		 */
		public final boolean start;
		/**
		 * The semantic action associated to this tag
		 */
		public final int action;
		
		/**
		 * Creates a new tag bound to name {@code id}
		 * 
		 * @param id
		 * @param start
		 * @param action
		 */
		public TagInfo(String id, boolean start, int action) {
			this.id = id;
			this.start = start;
			this.action = action;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + action;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + (start ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TagInfo other = (TagInfo) obj;
			if (action != other.action)
				return false;
			if (!id.equals(other.id))
				return false;
			if (start != other.start)
				return false;
			return true;
		}

		@Override
		public @NonNull String toString() {
			return String.format("{id=%s, start=%b, action=%d}", id, start, action);
		}
	}
	
	/**
	 * Enumeration which describes the different kinds of
	 * concrete implementations of {@link TRegular}.
	 * The field {@link #witness} describes the associated
	 * concrete class.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@SuppressWarnings("javadoc")
	public static enum Kind {
		EPSILON(Epsilon.class),
		CHARACTERS(Characters.class),
		TAG(Tag.class),
		ALTERNATE(Alternate.class), 
		SEQUENCE(Sequence.class), 
		REPETITION(Repetition.class),
		ACTION(Action.class);
		
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
	 * tags or not
	 */
	public final boolean hasTags;
	
	/**
	 * Whether this regular expressions has any 
	 * semantic actions or not
	 */
	public final boolean hasActions;

	/**
	 * Whether this tagged regular expression matches the empty string.
	 * <p>
	 * <i>Note that the end-of-file regular expression does not
	 * match the empty string, it matches a special character set 
	 * instead.</i>
	 */
	public final boolean nullable;
	
	private TRegular(Kind kind, 
			int size, boolean hasTags, 
			boolean hasActions, boolean nullable) {
		this.kind = kind;
		assert (kind.witness == this.getClass());
		this.size = size;
		this.hasTags = hasTags;
		this.hasActions = hasActions;
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
	
	@Override public abstract @NonNull String toString();
	
	/**
	 * The singleton class that stands for the empty tagged 
	 * regular expression ε, which only matches the empty string.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Epsilon extends TRegular {
		protected static final Epsilon INSTANCE = new Epsilon();
		
		private Epsilon() {
			super(Kind.EPSILON, 0, false, false, true);
		}

		@Override
		public @NonNull String toString() {
			return "ε";
		}
	}
	/**
	 * The empty tagged regular expression ε
	 */
	public static final Epsilon EPSILON = Epsilon.INSTANCE;
		
	/**
	 * Instances of regular expressions that match exactly
	 * one character amongst a set of possible characters.
	 * The character set is described by an integer index
	 * in some external character set map.
	 * 
	 * Can also match the end-of-input via a special 
	 * character set, in which case {@link TRegular#size}
	 * will be 0 and not 1.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Characters extends TRegular {
		/**
		 * The index of the character set matched by this expression
		 */
		public final int chars;
		
		/**
		 * Whether this character set matches end-of-input
		 */
		public final boolean eof;
		
		private Characters(int chars, boolean eof) {
			super(Kind.CHARACTERS, eof ? 0 : 1, false, false, false);
			this.chars = chars;
			this.eof = eof;
		}

		@Override
		public @NonNull String toString() {
			if (eof) 
				return String.format("[%dEOF]", chars);
			return String.format("[%d]", chars);
		}
	}
	/**
	 * @param cset
	 * @param eof
	 * @return a tagged regular expression which matches exactly
	 * 		one character in the character set {@code chars}
	 */
	public static TRegular chars(int cset, boolean eof) {
		return new Characters(cset, eof);
	}
	
	/**
	 * Instances of tagged regular expressions that represent
	 * a tag, i.e. either the beginning or the end of some
	 * bound sub-expression
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Tag extends TRegular {
		/**
		 * Describes the tag associated to this regular expression
		 */
		public final TagInfo tag;
		
		private Tag(TagInfo tag) {
			super(Kind.TAG, 0, true, false, true);
			this.tag = tag;
		}
		
		@Override
		public @NonNull String toString() {
			return tag.toString();
		}
	}
	/**
	 * @param id
	 * @param start
	 * @param action
	 * @return the regular expression consisting of the
	 * 	tag described by the given parameters
	 * @see TagInfo#TagInfo(String, boolean, int)
	 */
	public static Tag tag(String id, boolean start, int action) {
		return new Tag(new TagInfo(id, start, action));
	}
	
	/**
	 * Instances of tagged regular expressions that represent a 
	 * choice {@code a | b} between two regular expressions.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Alternate extends TRegular {
		/** The left-hand side of the choice operator */
		public final TRegular lhs;
		/** The right-hand side of the choice operator */
		public final TRegular rhs;
		
		private Alternate(TRegular lhs, TRegular rhs) {
			super(Kind.ALTERNATE,
					(lhs.size == rhs.size && lhs.size >= 0) ?
					lhs.size : -1,
				  lhs.hasTags || rhs.hasTags,
				  lhs.hasActions || rhs.hasActions,
				  lhs.nullable || rhs.nullable);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public @NonNull String toString() {
			return "(" + lhs.toString() + "|" + rhs.toString() + ")";
		}
	}
	/**
	 * @param lhs
	 * @param rhs
	 * @return the tagged regular expression representing the
	 * 		alternative of {@code lhs} and {@code rhs},
	 * 		in this order
	 */
	public static TRegular or(TRegular lhs, TRegular rhs) {
		if (lhs == rhs) return lhs;
		return new Alternate(lhs, rhs);
	}

	/**
	 * Instances of tagged regular expressions that represent the
	 * concatenation {@code ab} of two regular expressions.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Sequence extends TRegular {
		/** First part of the concatenation */
		public final TRegular first;
		/** Second part of the concatenation */
		public final TRegular second;
		
		private Sequence(TRegular first, TRegular second) {
			super(Kind.SEQUENCE,
					first.size < 0 || second.size < 0 ? - 1 :
					first.size + second.size,
				  first.hasTags || second.hasTags,
				  first.hasActions || second.hasActions,
				  first.nullable && second.nullable);
			this.first = first;
			this.second = second;
		}

		@Override
		public @NonNull String toString() {
			return first.toString() + second.toString();
		}
	}
	/**
	 * @param first
	 * @param second
	 * @return the tagged regular expression representing the
	 * 		concatenation of {@code first} and {@code second},
	 * 		in this order
	 */
	public static TRegular seq(TRegular first, TRegular second) {
		if (first == EPSILON) return second;
		if (second == EPSILON) return first;
		return new Sequence(first, second);
	}
	
	/**
	 * Instances of tagged regular expressions that represent the 
	 * Kleene closure {@code r*} of some regular expression {@code r},
	 * i.e. matching zero, one or more repetitions of {@code r}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Repetition extends TRegular {
		/** The regular expression to repeat */
		public final TRegular reg;
		
		private Repetition(TRegular reg) {
			super(Kind.REPETITION, -1, reg.hasTags, reg.hasActions, true);
			this.reg = reg;
		}

		@Override
		public @NonNull String toString() {
			return "(" + reg.toString() + ")*";
		}
	}
	/**
	 * @param reg
	 * @return the tagged regular expression representing any
	 * 		number of repetitions of {@code reg}
	 */
	public static TRegular star(TRegular reg) {
		if (reg == EPSILON) return EPSILON;
		if (reg instanceof Repetition) return reg;
		return new Repetition(reg);
	}
	
	/**
	 * Stands for a semantic action to be executed
	 * when matching reaching this regular expression.
	 * Semantic actions are indexed by an integer.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Action extends TRegular {
		/**
		 * The index of the semantic action
		 */
		public final int action;
		
		private Action(int action) {
			super(Kind.ACTION, 0, false, true, true);
			this.action = action;
		}
		
		@Override
		public @NonNull String toString() {
			return "{" + action + "}";
		}
	}
	/**
	 * @param action
	 * @return a regular expression consisting of
	 * 	the semantic action with the specified index
	 */
	public static Action action(int action) {
		return new Action(action);
	}
}