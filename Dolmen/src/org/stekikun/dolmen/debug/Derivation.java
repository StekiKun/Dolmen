package org.stekikun.dolmen.debug;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.syntax.TokenDecl;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.GrammarRule;
import org.stekikun.dolmen.unparam.Production;

/**
 * Instances of this class represent a <i>derivation</i> of some
 * {@link Grammar}, i.e. a possible complete tree built from the
 * grammar's productions and terminals.
 * <p>
 * A derivation can be {@link #display(Grammar) displayed} in its
 * raw form, or in a more user-friendly fashion using
 * {@link #append(Displayer, Appendable)} and a custom {@link Displayer}
 * implementation.
 * 
 * @see DerivationGenerator for how to produce derivations automatically
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Derivation {
	/**
	 * The ordinal of the head symbol in this derivation, i.e. either
	 * the ordinal of the produced non-terminal, or of the leaf terminal.
	 */
	public final short symbol;
	
	protected Derivation(short symbol) {
		this.symbol = symbol;
	}
	
	/**
	 * Appends the representation of the receiver into {@code buf} using {@code displayer}
	 * 
	 * @param displayer
	 * @param buf
	 * @throws IOException
	 */
	public abstract void append(Displayer displayer, Appendable buf) throws IOException;
	
	/**
	 * @return the height of the derivation
	 */
	public abstract short getHeight();
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		try {
			append(Displayer.RAW, buf);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot happen with StringBuilder");
		}
		return buf.toString();
	}
	
	/**
	 * @param grammar
	 * @return a user-friendly representation of the derivation using
	 * 	the symbols of the given grammar
	 */
	public String display(Grammar grammar) {
		StringBuilder buf = new StringBuilder();
		try {
			append(Displayer.ofGrammar(grammar, 0), buf);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot happen with StringBuilder");
		}
		return buf.toString();
	}

	/**
	 * This represents a node in a {@link Derivation} tree, i.e. the realization
	 * of some non-terminal ({@link Derivation#symbol}) in the grammar via one
	 * of its production ({@link #prod}). The items for the production rule
	 * are themselves derivation trees and can be fetched with {@link #getChildren()}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class NonTerminal extends Derivation {
		/** The index of the production used to produce this non-terminal */
		public final short prod;
		/** The derivations for the actuals of the production */
		private final List<Derivation> children;
		/** The cached height of this derivation */
		private final short height;
		
		private NonTerminal(short rule, short prod, List<Derivation> children) {
			super(rule);
			this.prod = prod;
			this.children = children;
			short hmax = 0;
			for (Derivation child : children) {
				short h = child.getHeight();
				if (h > hmax) hmax = h;
			}
			this.height = (short)(1 + hmax);
		}
		
		/**
		 * @return the derivations for the actuals of this production, in order
		 */
		public List<Derivation> getChildren() {
			return Collections.unmodifiableList(children);
		}
		
		@Override
		public short getHeight() {
			return height;
		}
		
		@Override
		public int hashCode() {
			int res = symbol << 16 | prod;
			return res * 31 + children.hashCode();
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof NonTerminal)) return false;
			NonTerminal nt = (NonTerminal) o;
			if (nt.symbol != symbol) return false;
			if (nt.prod != prod) return false;
			return nt.children.equals(children);
		}
		
		@Override
		public void append(Displayer displayer, Appendable buf) throws IOException {
			displayer.production(buf, symbol, prod);
			if (children.isEmpty()) return;
			displayer.enter(buf);
			boolean first = true;
			for (Derivation d : children) {
				if (first) first = false;
				else displayer.next(buf);
				d.append(displayer, buf);
			}
			displayer.leave(buf);
		}
	}
	
	/**
	 * @param rule
	 * @param prod
	 * @param derivations
	 * @return the derivation for the non-terminal {@code rule} using the
	 * 	production {@code prod} and the given sub-derivations
	 */
	public static NonTerminal production(short rule, short prod, List<Derivation> derivations) {
		return new NonTerminal(rule, prod, derivations);
	}
	
	/**
	 * This represents a leaf in a {@link Derivation} tree, i.e. some
	 * terminal identified by its {@link Derivation#symbol} number.
	 * <p>
	 * For efficiency reasons, maximal sharing is used so that instances
	 * of {@link Terminal} can have a perfect hashing and can be compared
	 * physically.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Terminal extends Derivation {
		private Terminal(short token) {
			super(token);
		}

		// Ensuring maximal sharing and perfect hashing
		
		private static @Nullable Terminal[] cache = new @Nullable Terminal[128];
		
		private static void growCache(short limit) {
			int k = cache.length >> 1;
			while (k <= limit) k >>= 1;
			@Nullable Terminal[] newCache = new @Nullable Terminal[k];
			System.arraycopy(cache, 0, newCache, 0, cache.length);
			cache = newCache;
		}
		
		private static Terminal get(short n) {
			@Nullable Terminal t;
			if (n >= cache.length) {
				synchronized (Terminal.class) {
					if (n >= cache.length)
						growCache(n);
				}
				t = null;
			}
			else
				t = cache[n];
			if (t == null) {
				t = new Terminal(n);
				cache[n] = t;
			}
			return t;
		}
		
		@Override
		public short getHeight() {
			return 0;
		}
		
		@Override
		public int hashCode() {
			return this.symbol;
		}
		
		@Override
		public boolean equals(@Nullable Object o) {
			return this == o;
		}
		
		@Override
		public void append(Displayer displayer, Appendable buf) throws IOException {
			displayer.terminal(buf, symbol);
		}
		
		@Override
		public String toString() {
			return "#" + symbol;
		}
	}
	
	/**
	 * @param token
	 * @return the leaf derivation representing the given token
	 */
	public static Terminal terminal(short token) {
		return Terminal.get(token);
	}
	
	/**
	 * This interface is used by {@link Derivation#append(Displayer, Appendable)} to allow
	 * the display of {@link Derivation} objects to be customized. The interface defines
	 * several callback routines which are called by the displaying engine and can be
	 * used to produce whatever structured output one wants from a derivation tree.
	 * <p>
	 * It is guaranteed that the methods in {@link Displayer} are called in a sequence
	 * {@code X} that follows these inductive rules:
	 * <ul>
	 * <li> either {@code X} is a single call to {@link #terminal(Appendable, short)};
	 * <li> or {@code X} is a call to {@link #production(Appendable, short, short)}
	 * 	    followed by either of:
	 * 		<ul>
	 * 		<li> nothing if the production was empty;
	 * 		<li> a call to {@link #enter(Appendable)} followed by any non-negative number
	 * 	         of {@code X}, each followed by {@link #next(Appendable)}, ending
	 *           with one {@code X} and a call to {@link #leave(Appendable)}.
	 * </ul>
	 * 
	 * @author Stéphane Lescuyer
	 */
	public interface Displayer {
		/**
		 * Displays a terminal with the given token ordinal
		 * 
		 * @param app
		 * @param token
		 * @throws IOException
		 */
		public void terminal(Appendable app, short token) throws IOException;

		/**
		 * Displays a production for the given rule and production ordinals
		 * 
		 * @param app
		 * @param rule
		 * @param prod
		 * @throws IOException
		 */
		public void production(Appendable app, short rule, short prod) throws IOException;
		
		/***
		 * Enters the sub-derivations of a production
		 * 
		 * @param app
		 * @throws IOException
		 */
		public void enter(Appendable app) throws IOException;

		/**
		 * Separates two sub-derivations of a production
		 * 
		 * @param app
		 * @throws IOException
		 */
		public void next(Appendable app) throws IOException;
		
		/**
		 * Completes the sub-derivations of a production
		 * 
		 * @param app
		 * @throws IOException
		 */
		public void leave(Appendable app) throws IOException;
		
		/**
		 * An implementation of {@link Displayer} which
		 * displays the derivations exactly as they are stored
		 */
		public static final Displayer RAW = new Displayer() {
			@Override
			public void terminal(Appendable app, short token) throws IOException {
				app.append('#').append(Short.toString(token));
			}

			@Override
			public void production(Appendable app, short rule, short prod) throws IOException {
				app.append('r').append(Short.toString(rule));
				app.append('#').append(Short.toString(prod));
			}

			@Override
			public void enter(Appendable app) throws IOException {
				app.append('(');
			}

			@Override
			public void next(Appendable app) throws IOException {
				app.append(',');
			}

			@Override
			public void leave(Appendable app) throws IOException {
				app.append(')');
			}
		};
		
		/**
		 * The result is printed as if starting at column {@code initialIndent}.
		 * 
		 * @param grammar
		 * @param initialIndent
		 * @return an implementation of {@link Displayer} which
		 * 	pretty-prints the derivation by interpreting it with
		 *  respect to the given {@code grammar}
		 */
		public static Displayer ofGrammar(final Grammar grammar, int initialIndent) {
			// Get the grammar's rules as an array, for easier access during display
			final GrammarRule[] rules = grammar.rules.values().toArray(new GrammarRule[0]);
			
			return new Displayer() {
				int indent = initialIndent;
				
				void indent(Appendable app) throws IOException {
					for (int i = 0; i < indent; ++i)
						app.append(' ');
				}

				@Override
				public void terminal(Appendable app, short token) throws IOException {
					if (grammar.tokenDecls.size() > token) {
						TokenDecl decl = grammar.tokenDecls.get(token);
						app.append(decl.name.val);
					}
					else {
						app.append("<Bad token " + token + ">");
					}
				}

				@Override
				public void production(Appendable app, short rule, short prod) throws IOException {
					// Find the rule
					final GrammarRule grule;
					{
						if (rule < 0 || rule >= rules.length) {
							app.append("<Bad rule " + rule + ">");
							return;
						}
						grule = rules[rule];
					}
					app.append(grule.name.val).append(" :=");
					// Find the production
					if (grule.productions.size() > prod) {
						final Production gprod = grule.productions.get(prod);
						for (Production.Actual actual : gprod.actuals())
							app.append(' ').append(actual.item.val);
						if (gprod.continuation() != null)
							app.append(" continue");
					}
					else {
						app.append(" <Bad production " + prod + ">");
					}
				}

				@Override
				public void enter(Appendable app) throws IOException {
					indent += 2;
					next(app);
				}

				@Override
				public void next(Appendable app) throws IOException {
					app.append('\n'); indent(app);
				}

				@Override
				public void leave(Appendable app) throws IOException {
					indent -= 2;
				}
			};
		}
	}
}
