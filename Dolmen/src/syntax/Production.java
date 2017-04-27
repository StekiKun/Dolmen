package syntax;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A grammar <i>production</i> consists of 
 * a sequence of {@link #items production items}
 * and an associated {@link #action semantic action}.
 * <p>
 * Items can be bound to some identifiers which can
 * be used in the semantic action.
 * 
 * @author Stéphane Lescuyer
 */
public final class Production {

	/**
	 * Represents a <i>production item</i>, i.e.
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
	public static final class Item {
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
		 * Builds a production item based on the given parameters
		 * @param binding
		 * @param item
		 */
		public Item(@Nullable String binding, String item) {
			this.binding = binding;
			this.item = item;
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
			return (binding == null ? "" : binding + " = ") + item;
		}
	}
	
	/** The list of production items in this production, in order */
	public final List<Item> items;
	/** The semantic action associated to this production */
	public final Location action;
	
	/**
	 * Builds a grammar production based on the given parameters
	 * @param items
	 * @param action
	 */
	public Production(List<Item> items, Location action) {
		this.items = items;
		this.action = action;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (Item item : items)
			buf.append(item);
		buf.append(action.toString());
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}

	/**
	 * Builder class for productions, allows adding
	 * {@link Item production items} incrementally
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Builder {
		private final Location action;
		private final List<Item> items;
		
		/**
		 * Returns a new builder with the given
		 * semantic action
		 * @param action
		 */
		public Builder(Location action) {
			this.action = action;
			this.items = new ArrayList<>();
		}
		
		/**
		 * Adds a production item
		 * @param item
		 */
		public void addItem(Item item) {
			items.add(item);
		}
		
		/**
		 * @return a production based on the registered
		 * 	items and semantic action
		 */
		public Production build() {
			return new Production(items, action);
		}
	}
}