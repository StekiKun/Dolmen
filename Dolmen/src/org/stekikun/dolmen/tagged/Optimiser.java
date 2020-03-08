package org.stekikun.dolmen.tagged;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.syntax.Regulars.VarsInfo;
import org.stekikun.dolmen.tagged.TRegular.Alternate;
import org.stekikun.dolmen.tagged.TRegular.Repetition;
import org.stekikun.dolmen.tagged.TRegular.Sequence;
import org.stekikun.dolmen.tagged.TRegular.Tag;
import org.stekikun.dolmen.tagged.TRegular.TagInfo;

/**
 * This class implements optimisations of 
 * {@linkplain TRegular tagged regular exceptions}
 * in order to minimise the number of tags
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
		
		@Override
		public String toString() {
			if (base == START) return "Sum(Start, " + offset + ")";
			if (base == END) return "Sum(End, " + offset + ")";
			return "Sum(" + base + ", " + offset + ")";
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
		
		/**
		 * @param addr
		 * @param offset
		 * @return {@code null} if {@code addr} is null or
		 * 	offset is negative, or an address with the same base 
		 *  as {@code addr} but an offset shifted by 
		 *  {@code offset} otherwise
		 */
		static @Nullable TagAddr
			shift(@Nullable TagAddr addr, int offset) {
			if (addr == null || offset < 0) return null;
			if (offset == 0) return addr;
			return of(addr.base, addr.offset + offset);
		}
	}
	
	private final boolean optimisation;
	private final VarsInfo varsInfo;
	private final Set<String> charVars;
	private final HashMap<TagKey, TagAddr> env;
	private int nextCell;
	
	private Optimiser(VarsInfo varsInfo, boolean optimisation) {
		this.optimisation = optimisation;
		this.varsInfo = varsInfo;
		this.charVars = varsInfo.getCharVars();
		this.env = new HashMap<>();
		this.nextCell = 0;
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
	
	private TagAddr getTagAddr(TagKey key) {
		@Nullable TagAddr res = Maps.get(env, key);
		if (res != null) return res;
		// Allocate a new memory cell for this tag key
		TagAddr newAddr = TagAddr.of(nextCell, 0);
		++nextCell;
		env.put(key, newAddr);
		return newAddr;
	}
	
	/**
	 * @return an unmodifiable view of the environment
	 * 	mapping tags that were optimised away by this instance
	 * 	to their relative addresses
	 */
	public Map<TagKey, TagAddr> getTagEnvironment() {
		return Collections.unmodifiableMap(env);
	}
	
	/**
	 * <i>Should only be applied to regular expressions
	 * 	  without semantic action nodes.</i>
	 * 
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
		if (pos < 0) throw new IllegalArgumentException();
		final TRegular regular = pr.regular;
		// If no tags, we know the regexp can't change, and we know
		// its size so we can avoid the traversal
		if (!regular.hasTags) {
			int newpos = regular.size >= 0 && pos >= 0 ? regular.size + pos : -1;
			if (newpos == pos) return pr;
			return new PosTRegular(newpos, regular);
		}
					
		switch (regular.getKind()) {
		case EPSILON:
		case CHARACTERS:
			// Already handled before the switch
			throw new IllegalStateException();
		case TAG: {
			final Tag tag = (Tag) regular;
			if (varsInfo.dblVars.contains(tag.tag.id))
				return pr;
			recordTagAddr(tag.tag, TagAddr.of(TagAddr.START, pos));
			return new PosTRegular(pos, TRegular.EPSILON);
		}
		case ALTERNATE: {
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
	
	/**
	 * <i>Should only be applied to regular expressions
	 * 	  without semantic action nodes.</i>
	 * <b>Positions are non-negative but they are to be
	 * 	interpreted negatively in this method, that is a position
	 * 	of {@code n} means {@code n} characters before the end
	 *  of the input string.</b>
	 *
	 * @param pr	the tagged regular exception to optimize
	 * 				along with its absolute position with
	 * 				respect to the end of the input, if any
	 * @return a tagged regular exception equivalent to
	 * 	{@code regular} but where some tags have been removed,
	 *  as their position relative to the end of the input
	 *  can be determined statically, along with the absolute
	 *  position known at the <i>start</i> of this regular expression
	 */
	private PosTRegular simpleBackward(PosTRegular pr) {
		final int pos = pr.pos;
		if (pos < 0) throw new IllegalArgumentException();
		final TRegular regular = pr.regular;
		// If no tags, we know the regexp can't change, and we know
		// its size so we can avoid the traversal
		if (!regular.hasTags) {
			int newpos = regular.size >= 0 && pos >= 0 ? regular.size + pos : -1;
			if (newpos == pos) return pr;
			return new PosTRegular(newpos, regular);
		}
					
		switch (regular.getKind()) {
		case EPSILON:
		case CHARACTERS:
			// Already handled before the switch
			throw new IllegalStateException();
		case TAG: {
			final Tag tag = (Tag) regular;
			if (varsInfo.dblVars.contains(tag.tag.id))
				return pr;
			// The offset in the tag address will be negative
			recordTagAddr(tag.tag, TagAddr.of(TagAddr.END, -pos));
			return new PosTRegular(pos, TRegular.EPSILON);
		}
		case ALTERNATE: {
			if (regular.size < 0) return new PosTRegular(-1, regular);
			return new PosTRegular(pos + regular.size, regular);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			PosTRegular pr2 = simpleBackward(new PosTRegular(pos, sequence.second));
			if (pr2.pos < 0)
				return new PosTRegular(-1,
							TRegular.seq(sequence.first, pr2.regular));
			PosTRegular pr1 = simpleBackward(new PosTRegular(pr2.pos, sequence.first));
			return new PosTRegular(pr1.pos, TRegular.seq(pr1.regular, pr2.regular));
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
	
	protected TRegular simpleBackward(TRegular r) {
		if (!r.hasTags) return r;
		return simpleBackward(new PosTRegular(0, r)).regular;
	}
	
	private static final class AddrTRegular {
		final @Nullable TagAddr addr;
		final TRegular regular;
		
		AddrTRegular(@Nullable TagAddr addr, TRegular regular) {
			this.addr = addr;
			this.regular = regular;
		}
	}
	
	/**
	 * @param pr
	 * @return a transformed tagged regular expressions where
	 * 	some tags have been replaced by offsets relative to
	 * 	other tags, when statically possible. After this procedure,
	 * 	all tags (removed or not) are associated to a number of
	 * 	unique memory cells in the environment {@link #env}.
	 */
	private AddrTRegular allocateAddresses(AddrTRegular pr) {
		final @Nullable TagAddr addr = pr.addr;
		final TRegular regular = pr.regular;
		// If no tags to allocate/replace, we can return the
		// given regexp as is, and we know its size if any so 
		// we can avoid the traversal
		if (!regular.hasTags) {
			if (addr == null) return pr;
			return new AddrTRegular(TagAddr.shift(addr, regular.size), regular);
		}
		
		switch (regular.getKind()) {
		case EPSILON:
		case CHARACTERS:
			// Already handled before the switch
			throw new IllegalStateException();
		case TAG: {
			final Tag tag = (Tag) regular;
			if (varsInfo.dblVars.contains(tag.tag.id))
				return pr;
			// If relative address to some formerly allocated tag
			// is known, record it and remove the tag. Otherwise
			// allocate a new cell and proceed with this new relative
			// position.
			if (addr != null) {
				env.put(new TagKey(tag.tag), addr);
				return new AddrTRegular(addr, TRegular.EPSILON);
			}
			else {
				TagAddr a = getTagAddr(new TagKey(tag.tag));
				return new AddrTRegular(a, tag);
			}
		}
		case ALTERNATE: {
			final Alternate alternate = (Alternate) regular;
			return new AddrTRegular(TagAddr.shift(addr, alternate.size), alternate);
		}
		case SEQUENCE: {
			final Sequence sequence = (Sequence) regular;
			AddrTRegular pr1 =
				allocateAddresses(new AddrTRegular(addr, sequence.first));
			AddrTRegular pr2 =
				allocateAddresses(new AddrTRegular(pr1.addr, sequence.second));
			return new AddrTRegular(pr2.addr, TRegular.seq(pr1.regular, pr2.regular));
		}
		case REPETITION: {
			final Repetition repetition = (Repetition) regular;
			if (addr == null) return pr;
			return new AddrTRegular(null, repetition);
		}
		case ACTION:
			throw new IllegalArgumentException();
		}
		throw new IllegalStateException();
	}
	
	/**
	 * Structure which represents how an indentifier in
	 * a regular expression is realized in optimised tagged
	 * regular expressions, i.e. provides {@linkplain TagAddr tag
	 * addresses} for the start and end of the expression bound
	 * to the identifier, and whether it is optional or not.
	 * <p>
	 * When the identifier is statically known to bind a
	 * single character only, the end tag address is omitted.
	 *
	 * @author Stéphane Lescuyer
	 */
	public static final class IdentInfo {
		/** Whether the identifier is only optionally bound */
		public final boolean optional;
		/** The tag address of the start of the bound sub-expression */
		public final TagAddr start;
		/** 
		 * The tag address of the end of the bound sub-expression,
		 * or {@code null} if the expression is guaranteed to be
		 * of size 1
		 */
		public final @Nullable TagAddr end;
		
		private IdentInfo(boolean optional,	TagAddr start, @Nullable TagAddr end) {
			this.optional = optional;
			this.start = start;
			this.end = end;
		}
		
		@Override
		public String toString() {
			String res = "{optional=" + optional + ", start=" + start;
			if (end == null) return res + "}";
			return res + ", end=" + end + "}";
		}
	}
	
	/**
	 * This structure represents the result of optimising a tagged
	 * regular expression: it packs together the resulting tagged
	 * expression, the list of {@link IdentInfo} for each possible
	 * bound variable in the original regular expression, and the
	 * overall number of different memory cells needed to store
	 * all markers at run-time.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Allocated {
		/** The optimised regular expression */
		public final TRegular regular;
		/** Tag addresses for each binding variable's boundaries */
		public final Map<String, @NonNull IdentInfo> identInfos;
		/** Total number of memory cells for tags */
		public final int numCells;
		
		/** Necessary? Environment from tags to memory cells */
		public final Map<@NonNull TagKey, @NonNull TagAddr> env;
		
		private Allocated(TRegular regular, Map<String, IdentInfo> identInfos, 
				int numCells, Map<TagKey, TagAddr> env) {
			this.regular = regular;
			this.identInfos = identInfos;
			this.numCells = numCells;
			this.env = env;
		}
		
		@Override
		public String toString() {
			return "{regular=" + regular +", identInfos=" 
				+ identInfos + ", numCells=" + numCells 
				+ ", env=" + env + "}";
		}
	}
	
	/**
	 * @param regular
	 * @return an optimised tagged regular expression
	 * @see #getTagEnvironment()
	 */
	private Allocated optimise(TRegular regular) {
		TRegular opt =
			optimisation ? simpleBackward(simpleForward(regular)) : regular;
		TRegular allocatedOpt = allocateAddresses(new AddrTRegular(null, opt)).regular;
		
		// Map all binding names to allocated addresses
		Map<String, IdentInfo> idents = new TreeMap<>();
		for (String name : varsInfo.allVars) {
			boolean optional = varsInfo.optVars.contains(name);
			TagAddr tstart = getTagAddr(new TagKey(name, true));
			TagAddr tend = null;
			if (!charVars.contains(name))
				tend = getTagAddr(new TagKey(name, false));
			idents.put(name, new IdentInfo(optional, tstart, tend));
		}
		
		return new Allocated(allocatedOpt, idents, nextCell, env);
	}
	
	/**
	 * @param varsInfo	variable analysis for {@code regular}
	 * @param optimisation 
	 * 		whether optimisation of tags with respect to the 
	 * 		start or end of input should be applied
	 * @param regular
	 * @return an optimised tagged regular expression along
	 * 		with the environment that maps every binding's
	 * 		boundaries to locations relative to memory cells
	 * @see Allocated
	 */
	public static Allocated optimise(
			VarsInfo varsInfo, boolean optimisation, TRegular regular) {
		return new Optimiser(varsInfo, optimisation).optimise(regular);
	}
	
}