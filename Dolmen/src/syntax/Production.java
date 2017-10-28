package syntax;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Hierarchy;
import common.Iterables;

/**
 * A grammar <i>production</i> consists of 
 * a sequence of {@link #items production items}
 * some of them being <i>actual</i> grammar items
 * (terminals or non-terminals) and others being
 * simply <i>semantic actions</i>.
 * <p>
 * Actual items can be bound to some identifiers which
 * can be used in the semantic actions.
 * 
 * @author Stéphane Lescuyer
 */
public final class Production {

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
		ACTION(ActionItem.class);
		
		public final Class<?> witness;
		private ItemKind(Class<?> witness) {
			this.witness = witness;
		}
	}
	
	/**
	 * The base class for production items. Items can
	 * either be {@link Actual actuals} when they 
	 * reference a terminal or non-terminal of the
	 * grammar, or {@link ActionItem semantic actions}
	 * which are propagated in the generated parser code.
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
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class ActionItem extends Item {
		
		/** The in-source extent for this semantic action */
		public final Extent extent;
		
		/**
		 * Builds a semantic action item from the given extent
		 * @param extent
		 */
		public ActionItem(Extent extent) {
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
	 * Represents an <i>actual production item</i>, i.e.
	 * a terminal or a non-terminal {@link #item}.
	 * <p>
	 * The production item can optionally be
	 * bound to some Java {@link #binding identifier},
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
		public final @Nullable String binding;
		/**
		 * The name of the production item, i.e. either
		 * the name of a non-terminal or a terminal of the
		 * grammar, depending on whether the identifier starts
		 * with a lowercase or uppercase character.
		 */
		public final String item;
		/**
		 * The Java arguments to the production item, if any.
		 * Arguments only make sense if {@link #item} is
		 * a non-terminal.
		 */
		public final @Nullable Extent args;
		
		/**
		 * Builds a production item based on the given parameters
		 * @param binding
		 * @param item
		 * @param args
		 */
		public Actual(@Nullable String binding,
				String item, @Nullable Extent args) {
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
			return Character.isUpperCase(item.charAt(0));
		}
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			if (binding != null)
				buf.append(binding).append(" = ");
			buf.append(item);
			@Nullable Extent args_ = args;
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
	 */
	public Production(List<Item> items) {
		this.items = items;
	}
	
	/**
	 * @return the list of actual production items
	 * 	in this production rule, in the same order
	 *  as they appear in {@link #items}
	 */
	public Iterable<@NonNull Actual> actuals() {
		return Iterables.filterClass(items, Actual.class);
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
	 * {@link Item production items} incrementally
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
		public Builder addAction(Extent extent) {
			items.add(new ActionItem(extent));
			return this;
		}
		
		/**
		 * @return a production based on the registered
		 * 	items and semantic actions
		 */
		public Production build() {
			return new Production(items);
		}
	}
}