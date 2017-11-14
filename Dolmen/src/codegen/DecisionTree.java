package codegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import automaton.DFA.GotoAction;
import automaton.DFA.TransActions;
import common.CSet;
import common.Hierarchy;
import common.Lists;
import common.Nulls;

// WIP
@SuppressWarnings("javadoc")
@Hierarchy("getKind")
public abstract class DecisionTree {

	public static enum Kind {
		RETURN(Return.class),
		SPLIT(Split.class),
		SWITCH(Switch.class),
		TABLE(Table.class),
		IMPOSSIBLE(Impossible.class);
		
		private Kind(Class<? extends DecisionTree> clazz) { }
	}
	
	public abstract CSet getDomain();
	
	public abstract Kind getKind();
	
	public final static class Impossible extends DecisionTree {
		static final Impossible INSTANCE = new Impossible();
		private Impossible() { }
		
		@Override
		public CSet getDomain() {
			return CSet.EMPTY;
		}

		@Override
		public Kind getKind() {
			return Kind.IMPOSSIBLE;
		}
	}
	public static final Impossible IMPOSSIBLE = Impossible.INSTANCE;
	
	public final static class Return extends DecisionTree {
		public final TransActions transActions;
		
		private Return(TransActions transActions) {
			this.transActions = transActions;
		}

		@Override
		public CSet getDomain() {
			return CSet.ALL;
		}

		@Override
		public Kind getKind() {
			return Kind.RETURN;
		}
	}
	public final static Return ret(TransActions transActions) {
		return new Return(transActions);
	}
	
	public final static class Split extends DecisionTree {
		public final char pivot;
		public final DecisionTree left;
		public final DecisionTree right;
		
		private Split(char pivot, DecisionTree left, DecisionTree right) {
			if (pivot == 0xFFFF) throw new IllegalArgumentException();
			this.pivot = pivot;
			this.left = left;
			this.right = right;
		}

		@Override
		public CSet getDomain() {
			return CSet.union(
				CSet.inter(left.getDomain(), CSet.interval((char)0, pivot)),
				CSet.inter(right.getDomain(), CSet.interval((char)(pivot + 1), (char)0xFFFF)));
		}

		@Override
		public Kind getKind() {
			return Kind.SPLIT;
		}
	}
	public final static DecisionTree split(char pivot, DecisionTree left, DecisionTree right) {
		if (pivot == 0xFFFF) return left;
		if (left == IMPOSSIBLE) return right;
		if (right == IMPOSSIBLE) return left;
		
		return new Split(pivot, left, right);
	}
	
	public final static class Switch extends DecisionTree {
		public final Map<@NonNull CSet, @NonNull TransActions> table;
		
		private Switch(Map<@NonNull CSet, @NonNull TransActions> table) {
			this.table = table;
		}

		@Override
		public CSet getDomain() {
			CSet res = CSet.EMPTY;
			for (CSet cset : table.keySet())
				res = CSet.union(res, cset);
			return res;
		}

		@Override
		public Kind getKind() {
			return Kind.SWITCH;
		}
	}
	public final static DecisionTree switchTable(Map<@NonNull CSet, @NonNull TransActions> table) {
		if (table.isEmpty()) return IMPOSSIBLE;
		return new Switch(table);
	}

	public final static class Table extends DecisionTree {
		public final char base;
		public final @NonNull TransActions table[];
		
		private Table(char base, @NonNull TransActions table[]) {
			this.base = base;
			this.table = table;
		}

		@Override
		public CSet getDomain() {
			return CSet.interval(base, (char)(base + table.length));
		}

		@Override
		public Kind getKind() {
			return Kind.TABLE;
		}
	}
	public final static DecisionTree tabulated(char base, @NonNull TransActions table[]) {
		if (table.length == 0) return IMPOSSIBLE;
		return new Table(base, table);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		prettyPrint(buf, this, "");
		return buf.toString();
	}
	
	private static void prettyPrint(StringBuilder buf, DecisionTree tree, String prefix) {
		switch (tree.getKind()) {
		case RETURN:
			Return ret = (Return) tree;
			buf.append("RETURN ").append(ret.transActions).append("\n");
			return;
		case SPLIT:
			Split split = (Split) tree;
			buf.append(String.format("+-c <= %04x-- ", (int)split.pivot));
			prettyPrint(buf, split.left, prefix + "|             ");
			buf.append(prefix).append(String.format("`-c >  %04x-- ", (int)split.pivot));
			prettyPrint(buf, split.right, prefix + "              ");
			return;
		case SWITCH:
			Switch switch_ = (Switch) tree;
			buf.append("SWITCH: {\n");
			switch_.table.forEach((cset, trans) -> {
				buf.append(prefix).append("  ");
				buf.append(cset).append(" -> ").append(trans);
				buf.append("\n");
			});
			buf.append(prefix).append("}\n");
			return;
		case TABLE:
			Table table = (Table) tree;
			buf.append("TABLE (offset=").append(table.base).append("): [\n");
			for (int i = 0; i < table.table.length; ++i) {
				buf.append(prefix).append("  ");
				buf.append(i + table.base).append(" -> ").append(table.table[i]);
				buf.append("\n");
			}
			buf.append(prefix).append("]\n");
			return;
		case IMPOSSIBLE:
			buf.append("IMPOSSIBLE\n");
			return;
		}
		throw new IllegalStateException("Unexpected DecisionTree.Kind: " + tree.getKind());
	}
	
	private static DecisionTree clamp(DecisionTree tree, char min, char max) {
		if (max < min)
			throw new IllegalArgumentException("min: " + min + ", max: " + max);
		switch (tree.getKind()) {
		case RETURN:
			return tree;
		case SPLIT:
			Split split = (Split) tree;
			char pivot = split.pivot;
			if (pivot >= max)
				return clamp(split.left, min, max);
			else if (pivot < min)
				return clamp(split.right, min, max);
			return split(pivot, 
						clamp(split.left, min, pivot), 
						clamp(split.right, (char)(pivot + 1), max));
		case SWITCH: {
			Switch switch_ = (Switch) tree;
			CSet total = CSet.interval(min, max);
			if (CSet.included(switch_.getDomain(), total))
				return switch_;
			Map<@NonNull CSet, @NonNull TransActions> clamped = new HashMap<>(switch_.table.size());
			switch_.table.forEach((cset, trans) -> {
				CSet key = CSet.inter(cset, total);
				if (key.isEmpty()) return;
				clamped.put(key, trans);
			});
			return switchTable(clamped);
		}
		case TABLE:
			Table table = (Table) tree;
			int tmax = table.base + table.table.length - 1;
			if (min > tmax || max < table.base)
				return IMPOSSIBLE;
			
			int cmin = Math.max(table.base, min);
			int cmax = Math.min(tmax, max);
			if (cmin == table.base && cmax == tmax)
				return table;
			
			if (cmax - cmin == 0)
				return ret(table.table[cmin - table.base]);
			if (cmax < cmin) {
				throw new IllegalStateException("cmax: " + cmax + ", cmin:" + cmin);
			}
			TransActions clamped[] = new TransActions[cmax - cmin + 1];
			for (int i = cmin; i <= cmax; ++i)
				clamped[i - cmin] = table.table[i - table.base];
			// At that point, all cells in clamped are initialized
			return tabulated((char) cmin, Nulls.arrayOk(clamped));
		case IMPOSSIBLE:
			return IMPOSSIBLE;
		}
		throw new IllegalStateException("Unexpected DecisionTree.Kind: " + tree.getKind());
	}
	
	public static DecisionTree simplify(DecisionTree tree) {
		// return clamp(tree, (char)49, (char)49);
		return clamp(tree, (char)0, (char)0xFFFF);
	}
	
	private static final class Compiling {
		static final class Segment {
			final char first;
			final char last;
			final TransActions trans;
			
			Segment(char first, char last, TransActions trans) {
				if (last < first) throw new IllegalArgumentException();
				this.first = first;
				this.last = last;
				this.trans = trans;
			}
			
			@Override
			public String toString() {
				return String.format("%s -> %s", CSet.interval(first, last), trans);
			}
			
			/**
			 * A {@link Comparator} instance for segments which orders them
			 * totally with respect to the segment's first character.
			 * <p>
			 * <b>This order is generally inconsistent with {@link #equals(Object)}
			 * 	and only makes sense when dealing with collections of segments which
			 * 	are known to be pairwise disjoint, as is the case when compiling
			 * 	partition maps.
			 * </b>
			 */
			final static Comparator<Segment> COMPARATOR = new Comparator<Segment>() {
				@Override
				public int compare(Segment o1, Segment o2) {
					return o1.first - o2.first;
				}
			};
		}
		
		// partition.keySet() must be a partition of CSet.ALL
		private static List<@NonNull Segment> 
			segments(Map<@NonNull CSet, @NonNull TransActions> partition) {
			final ArrayList<@NonNull Segment> segments = new ArrayList<>(partition.size());
			// Add all segments for each partition entry
			partition.forEach((cset, trans) -> {
				cset.forEachInterval((first, last) ->
					segments.add(new Segment(first, last, trans)));
			});
			// Segments from the same partition entry were visited and
			// entered in increasing order, but segments from different
			// entries may be intertwined so we generally need to sort the
			// segments array
			segments.sort(Segment.COMPARATOR);
			return segments;
		}
		
		private static CSet largestClass(Iterable<@NonNull CSet> it) {
			int max = -1;
			CSet found = CSet.EMPTY; // irrelevant but non-null
			for (CSet cs : it) {
				if (cs.cardinal() > max) {
					max = cs.cardinal();
					found = cs;
				}
			}
			return found;
		}
		
		private static int sizeForSwitch(Iterable<@NonNull CSet> it) {
			CSet largest = largestClass(it);
			int allNonDefault = 0;
			for (CSet cs : it) {
				if (cs != largest)
					allNonDefault += cs.cardinal();
			}
			return allNonDefault;
		}

		private static Map<@NonNull CSet, @NonNull TransActions>
			partitionOf(List<@NonNull Segment> segments, int from, int length) {
			// First regroup all segments which lead to the same transition actions
			Map<@NonNull TransActions, @NonNull CSet> inverse = new IdentityHashMap<>(length);
			for (int i = from; i < from + length; ++i) {
				Segment segi = segments.get(i);
				CSet csi = CSet.interval(segi.first, segi.last);
				@Nullable CSet already = inverse.get(segi.trans);
				if (already == null)
					inverse.put(segi.trans, csi);
				else
					inverse.put(segi.trans, CSet.union(csi, already));
			}
			// Then invert the map, which must be bijective because we have
			// built the values by merging segments that were all disjoint initially
			Map<@NonNull CSet, @NonNull TransActions> partition = new HashMap<>(inverse.size());
			inverse.forEach((trans, cset) -> partition.put(cset, trans));
			return partition;
		}
		
		private static final int SWITCH_LIMIT = 64;	// enough for [_0-9a-zA-Z]
		private static final int SPLIT_LIMIT = 4;
		
		static DecisionTree compile(Map<@NonNull CSet, @NonNull TransActions> partition) {
			// Strategy: try to minimize some notion of 'cost' which will account
			//		both for nlocs of generated code and the supposed efficiency
			//      (i.e. number of character comparisons performed on average input)
			// Maybe later we'll let the user configure the different weights to prioritize
			// conciseness over efficiency or the other way around.
			
			// A switch-case encoding has the following characteristics:
			// - there will be one case for each character which is *not*
			//   in the largest CSet in the partition (for the latter, the 'default'
			//   clause can be used)
			// - as far as efficiency is concerned, there is no guarantee as to what
			//   the Java compiler will do with the switch, it may use a lookupswitch
			//   or a tableswitch. If there aren't too many gaps in the chars, it will probably
			//   use a table switch [NB: FOR THAT REASON I WON'T USE DecisionTree.Table
			//   FOR THE MOMENT]. Lookupswitch may be executed as an if cascade or something
			//   better but I won't rely on it and large switches should be implemented as
			//   balanced decision trees.
			
			// Strategy for now : evaluate the length of a potential switch-block for
			//	the current partition, and if it exceeds a certain number (say 64 or 128)
			//  transform the partition in a segments list, split it roughly in half and proceed 
			//  recursively. Highly frequent rules which only match small ranges of characters
			//  in the ASCII range should end in switches which in turn hopefully result in 
			//  tableswitches.
			
			int allNonDefault = sizeForSwitch(partition.keySet());
			if (allNonDefault <= SWITCH_LIMIT)
				return switchTable(partition);
			List<Segment> segments = segments(partition);
			return balance(0, segments(partition), 0, segments.size());
		}
		
		private static DecisionTree balance(int depth, List<Segment> segments, int from, int length) {
			if (from < 0 || from >= segments.size())
				throw new IllegalArgumentException(
					String.format("Invalid index %d (size %d)", from, segments.size()));
			if (length <= 0 || from + length > segments.size())
				throw new IllegalArgumentException(
					String.format("Invalid range %d (size %d, from %d)", length, segments.size(), from));
			
			if (length == 1)
				// If the remainder of the partition is just one segment, we're done
				return ret(segments.get(from).trans);
			
			// If the classes left are amenable for a switch-block, let's do it
			// We divide the maximum size for a switch by 2^depth for the worst case
			// where every leaf would contain a big switch
			Map<@NonNull CSet, @NonNull TransActions> part = partitionOf(segments, from, length);
			int allNonDefault = sizeForSwitch(part.keySet());
			if (allNonDefault <= Math.max(SPLIT_LIMIT, SWITCH_LIMIT >> depth))
				return switchTable(part);
			
			// Otherwise let us look for a pivot. It is not important that the total
			// cardinality of the segments be balanced across the pivot, but instead we
			// rather split the list of segments roughly in half.
			int half = length / 2;	// >= 1
			Segment pivotSegment = segments.get(from + half - 1);
			DecisionTree leftTree = balance(depth + 1, segments, from, half);
			DecisionTree rightTree = balance(depth + 1, segments, from + half, length - half);
			return split(pivotSegment.last, leftTree, rightTree);
		}
	}
	
	public static DecisionTree compile(Map<@NonNull CSet, @NonNull TransActions> partition) {
		DecisionTree tree = Compiling.compile(partition);
		tree = simplify(tree);
//		if (tree.getKind() == Kind.SPLIT) {
//			System.out.println("Initial partition: " + partition);
//			System.out.println("Decision tree:\n" + tree);
//			System.out.println("Simplified tree:\n" + tree);
//		}
		return tree;
	}
	
	
	// ========================================================
	//   LOCAL TESTING
	// --------------------------------------------------------
	
	private static void test(DecisionTree tree) {
		System.out.println("-----------------------------------------");
		System.out.println(tree);
		System.out.println("This tree has domain: " + tree.getDomain());
		System.out.println(simplify(tree));
	}
	
	public static void main(String[] args) {
		Return rewind = ret(TransActions.BACKTRACK);
		Return r1 = ret(new TransActions(GotoAction.Goto(1), Lists.empty()));
		Return r2 = ret(new TransActions(GotoAction.Goto(2), Lists.empty()));
		
		Map<@NonNull CSet, @NonNull TransActions> map = new HashMap<>();
		map.put(CSet.interval('a', 'z'), r1.transActions);
		map.put(CSet.interval('0', '9'), r2.transActions);
		DecisionTree sw = switchTable(map);
		
		@SuppressWarnings("null")
		@NonNull TransActions[] ttable =
			{ r1.transActions, TransActions.BACKTRACK, r2.transActions };
		DecisionTree tt = tabulated('0', ttable);
		
		DecisionTree t1 = split('a', rewind, r1);
		DecisionTree t2 = split('@', t1, r2);
		DecisionTree t3 = split('µ', tt, t2);
		DecisionTree t4 = split('<', sw, t3);
		
		test(r1);
		test(tt);
		test(t1);
		test(t2);
		test(t3);
		test(t4);
		
		compile(map);

		Map<@NonNull CSet, @NonNull TransActions> map2 = new HashMap<>(map);
		map2.put(CSet.interval((char)500, (char)3000),
				new TransActions(GotoAction.Goto(3), Lists.empty()));
		map2.put(CSet.interval((char)0, (char)10),
				new TransActions(GotoAction.Goto(4), Lists.empty()));
		map2.put(CSet.interval((char)4000, (char)4100),
				new TransActions(GotoAction.Goto(5), Lists.empty()));
		map2.put(CSet.interval((char)6000, (char)12100),
				new TransActions(GotoAction.Goto(6), Lists.empty()));
		compile(map2);
		
		Map<@NonNull CSet, @NonNull TransActions> map3 = new HashMap<>(map2);
		CSet others = CSet.union(CSet.EOF,
			CSet.complement(map2.keySet().stream().reduce(CSet.EMPTY, CSet::union)));
		map3.put(others, rewind.transActions);
		compile(map3);
	}
}
