package syntax;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Hierarchy;
import common.Iterables;

/**
 * A <i>parameterized grammar production</i> consists of 
 * a sequence of {@linkplain #items production items}
 * some of them being <i>actual</i> grammar items
 * (terminals or non-terminals, the latter potentially
 * applied recursively to other actuals) and others being
 * simply <i>semantic actions</i>.
 * <p>
 * Actual items can be bound to some identifiers which
 * can be used in the semantic actions. Their general
 * form follows the following grammar:
 * <pre>
 * 	actual := TERMINAL
 * 			| param
 * 			| nterm[&lt;actual, ...&gt;]
 * </pre>
 * where the terminals are the tokens declared in the
 * parameterized grammar, the non-terminals are (possibly
 * parameterized) rules of the grammar, and {@code param}
 * refers to the formal parameters available in the rule
 * the production is associated to. In particular, parameters
 * can only be used as leaves and are thus not second-order;
 * also each parameterized non-terminal must be fully applied. 
 * 
 * @author Stéphane Lescuyer
 */
public final class PProduction {

	/**
	 * Enumeration which describes the different kinds of
	 * implementations of {@link Item}.
	 * The field {@link #witness} describes the associated
	 * concrete class.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@SuppressWarnings("javadoc")
	public static enum ItemKind {
		ACTUAL(Actual.class), 
		ACTION(ActionItem.class),
		CONTINUE(Continue.class);
		
		public final Class<?> witness;
		private ItemKind(Class<?> witness) {
			this.witness = witness;
		}
	}
	
	/**
	 * The base class for production items. Items can
	 * either be {@linkplain Actual actuals} when they 
	 * reference an expression made of terminals and non-terminals
	 * of the grammar, or {@linkplain ActionItem semantic actions}
	 * which are propagated in the generated parser code.
	 * <p>
	 * An extra special kind of item is the 
	 * {@linkplain Continue continuation} which allows
	 * to reenter the current rule and can only be used
	 * last in a production.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@Hierarchy("getKind")
	public static abstract class Item {
		/** Describes the kind of item {@code this} is */
		public abstract ItemKind getKind();
	}
	
	/**
	 * Represents a semantic action item, i.e. a semantic
	 * action which is performed by the generated parser
	 * when the enclosing production rule is executed.
	 * <p>
	 * Semantic action items are executed in the order
	 * in which they appear in productions, relatively 
	 * to actuals and other semantic actions. 
	 * <p>
	 * A semantic action in a parameterized production can
	 * contain {@linkplain PExtent.Hole holes} referring to
	 * the parameters of the rule containing the production.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class ActionItem extends Item {
		
		/** The in-source extent for this semantic action */
		public final PExtent extent;
		
		/**
		 * Builds a semantic action item from the given extent
		 * @param extent
		 */
		public ActionItem(PExtent extent) {
			this.extent = extent;
		}
		
		@Override
		public final ItemKind getKind() {
			return ItemKind.ACTION;
		}
		
		@Override
		public String toString() {
			return "{" + extent.find() + "}";
		}
	}
	
	/**
	 * Represents a continuation of the current rule,
	 * and unlike the equivalent {@linkplain Actual actual},
	 * this allows the generator to produce an optimized
	 * tail-recursive call.
	 * <br>
	 * A continuation can only be used last in a production
	 * rule, and for now can only reenter the current rule 
	 * with the same original arguments and parameters.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Continue extends Item {
		
		/** The location of the item (for error reporting) */
		public final Located<String> cont;
		
		/**
		 * Builds a continuation item at the given location
		 * @param cont
		 */
		public Continue(Located<String> cont) {
			this.cont = cont;
		}
		
		@Override
		public final ItemKind getKind() {
			return ItemKind.CONTINUE;
		}
		
		@Override
		public String toString() {
			return "continue";
		}
	}
	
	/**
	 * An expression representing the application of some grammar rule,
	 * potentially via applications of parameterized rules of the grammar
	 * to other symbols or expressions.
	 * <p>
	 * The expression is characterized by its head {@linkplain #symb symbol}
	 * and its possibly empty list of {@linkplain #params sub-expressions}, 
	 * which act as effective parameters to the head symbol.
	 * <p>
	 * Expression leaves are terminals, rule parameters,
	 * and non-parameterized non-terminals.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class ActualExpr {
		/** 
		 * The name of the production item, i.e. either
		 * the name of a non-terminal or a terminal of the
		 * grammar, depending on whether the identifier starts
		 * with a lowercase or uppercase character.
		 */
		public final Located<String> symb;
		
		/**
		 * The (possibly empty) list of actual items which
		 * should be passed as {@link #symb}'s parameters.
		 * There should be exactly as many parameters as
		 * declared in the declaration of {@link #symb}. In
		 * particular, it should be empty if {@link #symb}
		 * is a terminal or a parameter.
		 */
		public final List<ActualExpr> params;
		
		/**
		 * Returns an actual expression made of applying 
		 * {@code symb} to the actuals given in {@code params}
		 * 
		 * @param symb
		 * @param params
		 */
		public ActualExpr(Located<String> symb, List<ActualExpr> params) {
			this.symb = symb;
			this.params = params;
		}

		/**
		 * @return {@code true} is this expression is a terminal
		 */
		public boolean isTerminal() {
			return Character.isUpperCase(symb.val.charAt(0));
		}
		
		@Override
		public int hashCode() {
			return 31 * symb.hashCode() + params.hashCode();
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof ActualExpr)) return false;
			ActualExpr ae = (ActualExpr) o;
			if (!symb.equals(ae.symb)) return false;
			if (!params.equals(ae.params)) return false;
			return true;
		}
		
		private StringBuilder append(StringBuilder buf) {
			buf.append(symb.val);
			if (params.isEmpty()) return buf;
			buf.append('<');
			boolean first = true;
			for (ActualExpr ae : params) {
				if (first) first = false;
				else buf.append(", ");
				ae.append(buf);
			}
			buf.append('>');
			return buf;
		}
		
		@Override
		public String toString() {
			return append(new StringBuilder()).toString();
		}
	}
	
	/**
	 * Represents an <i>actual production item</i>, i.e.
	 * an expression representing a rule of the grammar via
	 * a terminal, a parameter of the production's rule, 
	 * or a non-terminal potentially applied recursively
	 * to such expressions.
	 * <p>
	 * The rule described by the item can optionally be applied
	 * to some {@linkplain #args arguments}, as expected by
	 * the item's head-symbol when it is a non-terminal.
	 * <p>
	 * The production item can optionally be
	 * bound to some Java {@linkplain #binding identifier},
	 * in which case the associated value is made
	 * available in the enclosing production's
	 * semantic action.
	 * 
	 * @author Stéphane Lescuyer
	 * @see #isTerminal()
	 * @see #isBound()
	 */
	public static final class Actual extends Item {
		/**
		 * The name to which this item is bound in
		 * the associated semantic action, if non-{@code null}
		 */
		public final @Nullable Located<String> binding;
		/**
		 * The actual expression representing the rule or
		 * token to be used as this production item.
		 */
		public final ActualExpr item;
		/**
		 * The Java arguments to the production item, if any.
		 * Arguments only make sense if {@link #item} is
		 * a non-terminal (or a parameter which will only be
		 * instantiated by non-terminals expecting parameters).
		 * <p>
		 * This Java extent can contain holes referring to
		 * the formal parameters available in the rule this
		 * appears in.
		 */
		public final @Nullable PExtent args;
		
		/**
		 * Builds a production item based on the given parameters
		 * @param binding
		 * @param item
		 * @param args
		 */
		public Actual(@Nullable Located<String> binding,
				ActualExpr item, @Nullable PExtent args) {
			this.binding = binding;
			this.item = item;
			this.args = args;
			if (isTerminal() && args != null)
				throw new IllegalArgumentException();
		}
		
		@Override
		public final ItemKind getKind() {
			return ItemKind.ACTUAL;
		}
		
		/**
		 * @return {@code true} is this item is bound to some identifier
		 */
		public boolean isBound() {
			return binding != null;
		}
		
		/**
		 * @return {@code true} is this item is a terminal
		 */
		public boolean isTerminal() {
			return item.isTerminal();
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			Located<String> bind = binding;
			if (bind != null)
				buf.append(bind.val).append(" = ");
			item.append(buf);
			@Nullable PExtent args_ = args;
			if (args_ != null)
				buf.append("(").append(args_.find()).append(")");
			
			return buf.toString();
		}
	}
	
	/** The list of production items in this production, in order */
	public final List<@NonNull Item> items;
	
	/**
	 * Builds a grammar production based on the given parameters
	 * @param items
	 * @throws IllegalArgumentException if {@code items} contains
	 * 	a {@linkplain Continue} elsewhere than as the last item
	 */
	public PProduction(List<Item> items) {
		this.items = items;
	}
	
	/**
	 * <i>This does not include potential continuations, although
	 *  on many aspects they act as actuals.</i>
	 * 
	 * @return the list of actual production items
	 * 	in this production rule, in the same order
	 *  as they appear in {@link #items}
	 */
	public Iterable<@NonNull Actual> actuals() {
		return Iterables.filterClass(items, Actual.class);
	}
	
	/**
	 * @return the continuation at the end of this production
	 * 	rule if any, and {@code null} otherwise
	 */
	public @Nullable Continue continuation() {
		if (items.isEmpty()) return null;
		Item item = items.get(items.size() - 1);
		if (item.getKind() == ItemKind.CONTINUE)
			return (Continue) item;
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (Item item : items)
			buf.append(" ").append(item);
		return buf.toString();
	}

	/**
	 * Builder class for productions, allows adding
	 * {@linkplain Item production items} incrementally
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Builder {
		private final List<Item> items;
		
		/**
		 * Returns a new builder for a production rule
		 */
		public Builder() {
			this.items = new ArrayList<>();
		}
		
		/**
		 * Adds a production item
		 * @param item
		 */
		public Builder addItem(Item item) {
			items.add(item);
			return this;
		}
		
		/**
		 * Adds an actual production item
		 * @param actual
		 */
		public Builder addActual(Actual actual) {
			items.add(actual);
			return this;
		}
		
		/**
		 * Adds a semantic action item
		 * @param extent
		 */
		public Builder addAction(PExtent extent) {
			items.add(new ActionItem(extent));
			return this;
		}
		
		/**
		 * @return a production based on the registered
		 * 	items and semantic actions
		 */
		public PProduction build() {
			return new PProduction(items);
		}
	}
}