package tagged;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import syntax.Regulars.VarsInfo;
import tagged.TRegular.Characters;
import tagged.TRegular.Repetition;
import tagged.TRegular.Sequence;
import tagged.TRegular.Tag;
import tagged.TRegular.TagInfo;

/**
 * This class implements optimizations of 
 * {@link TRegular tagged regular exceptions}
 * in order to minimize the number of tags
 * (which have a big impact on the complexity
 * of the translation into a DFA).
 * 
 * <p>
 * The various optimisations remove tags whose 
 * positions in any successful matching can be 
 * deduced statically, with respect to either
 * the start of the matched string, its end, or 
 * any other tag in the regular expression.
 * Removed tags are recorded in an interpretation
 * map, which for each such tag gives its relative
 * position to some other marker.
 * 
 * @author Stéphane Lescuyer
 */
public final class Optimiser {

	/**
	 * Similar to {@link TagInfo}, but without the
	 * semantic action. This is used as a key in
	 * environments mapping tags to relative offsets
	 * described by {@link TagAddr}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TagKey {
		/** The name associated to the tag */
		public final String id;
		/** 
		 * Whether this tag marks the start of some
		 * bound subexpression or the end
		 */
		public final boolean start;
		
		/**
		 * @param id
		 * @param start
		 */
		public TagKey(String id, boolean start) {
			this.id = id;
			this.start = start;
		}
		/**
		 * @param info
		 */
		public TagKey(TagInfo info) {
			this(info.id, info.start);
		}
		
		@Override
		public String toString() {
			return "{id=" + id + ", start=" + start + "}";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id.hashCode();
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
			TagKey other = (TagKey) obj;
			if (!id.equals(other.id))
				return false;
			if (start != other.start)
				return false;
			return true;
		}
	}
	
	/**
	 * The relative address of a tag, either with
	 * respect to some memory cell, i.e. some actual
	 * storage for a tag, or to the {@link TagAddr#START}
	 * or {@link TagAddr#END} of the input.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TagAddr {
		/**
		 * The base address for the tag, relative to
		 * which {@link #offset} must be interpreted
		 */
		public final int base;
		/**
		 * Relative position to the {@link #base}
		 */
		public final int offset;
		
		private TagAddr(int base, int offset) {
			this.base = base;
			this.offset = offset;
		}
		
		/** The special base address representing the start of input */
		public static final int START = -1;
		/** The special base address representing the end of input */
		public static final int END = -2;
		/** 
		 * @param base
		 * @param offset
		 * @return a new tag address based on the given
		 * 		tag base and offset
		 */
		public static TagAddr of(int base, int offset) {
			if (base < -2) throw new IllegalArgumentException();
			return new TagAddr(base, offset);
		}
	}
	
	private final VarsInfo varsInfo;
	@SuppressWarnings("unused")
	private final Set<String> charVars;
	private final HashMap<TagKey, TagAddr> env;
	
	private Optimiser(VarsInfo varsInfo) {
		this.varsInfo = varsInfo;
		this.charVars = varsInfo.getCharVars();
		this.env = new HashMap<>();
	}
	
	private static final class PosTRegular {
		final int pos;
		final TRegular regular;
		
		PosTRegular(int pos, TRegular regular) {
			this.pos = pos;
			this.regular = regular;
		}
	}
	
	private void recordTagAddr(TagInfo tag, TagAddr addr) {
		env.put(new TagKey(tag), addr);
	}
	
	/**
	 * @param pr	the tagged regular exception to optimize
	 * 				along with its absolute position with
	 * 				respect to the start of the input, if any
	 * @return a tagged regular exception equivalent to
	 * 	{@code regular} but where some tags have been removed,
	 *  as their position relative to the start of the input
	 *  can be determined statically, along with the absolute
	 *  position known at the <i>end</i> of this regular expression
	 */
	private PosTRegular simpleForward(PosTRegular pr) {
		final int pos = pr.pos;
		final TRegular regular = pr.regular;
		// If no tags, we know the regexp can't change, and we know
		// its size so we can avoid the traversal
		if (!regular.hasTags) {
			int newpos = regular.size >= 0 && pos >= 0 ? regular.size + pos : -1;
			return new PosTRegular(newpos, regular);
		}
					
		switch (regular.getKind()) {
		case EPSILON:
			return pr;
		case CHARACTERS: {
			final Characters characters = (Characters) regular;
			return new PosTRegular(characters.eof ? pos : pos + 1, regular);
		}
		case TAG: {
			final Tag tag = (Tag) regular;
			if (varsInfo.dblVars.contains(tag.tag.id))
				return pr;
			recordTagAddr(tag.tag, TagAddr.of(TagAddr.START, pos));
			return new PosTRegular(pos, TRegular.EPSILON);
		}
		case ALTERNATE: {
			if (pos < 0) return pr;
			if (regular.size < 0) return new PosTRegular(-1, regular);
			return new PosTRegular(pos + regular.size, regular);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			PosTRegular pr1 = simpleForward(new PosTRegular(pos, sequence.first));
			if (pr1.pos < 0)
				return new PosTRegular(-1, 
							TRegular.seq(pr1.regular, sequence.second));
			PosTRegular pr2 = simpleForward(new PosTRegular(pr1.pos, sequence.second));
			return new PosTRegular(pr2.pos, TRegular.seq(pr1.regular, pr2.regular));
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			if (pos < 0) return pr;
			return new PosTRegular(-1, repetition);
		}
		case ACTION: {
			throw new IllegalArgumentException();
		}
		}
		throw new IllegalStateException();
	}
	
	protected TRegular simpleForward(TRegular r) {
		if (!r.hasTags) return r;
		return simpleForward(new PosTRegular(0, r)).regular;
	}
}
