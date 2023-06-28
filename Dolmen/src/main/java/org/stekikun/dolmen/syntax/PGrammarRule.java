package org.stekikun.dolmen.syntax;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes a (potentially parametric) grammar rule for a 
 * non-terminal in a parametric grammar description. The rule 
 * can be {@linkplain #visibility public or private} which will 
 * impact the visibility of the corresponding generated method, 
 * it specifies the  {@linkplain #returnType return type} of the
 * associated semantic actions and can have some
 * {@linkplain #args formal arguments}.
 * <p>
 * The rule can be ground or be parameterized by a number of
 * formal {@linkplain #params parameters}. A parameterized rule
 * represents a generic rule from which ground rules can be obtained
 * by simply instantiating the formal parameters with terminals or
 * (instantiated) non-terminals of the grammar. A rule's parameter
 * can be used in its production items, as a regular symbol of the
 * grammar, and also as {@linkplain PExtent.Hole holes} in semantic
 * actions where they stand for the return type associated to the
 * formal parameter.
 * <b>Public rules cannot be parametric as public rules act as 
 * 	monomorphic entry points allowing the monomorphization of a
 *  parametric grammar.</b>
 * <p>
 * The relative order of {@linkplain #productions productions} for this
 * grammar rule is the one from the original source but is not
 * semantically relevant for the produced grammar.
 * 
 * @author Stéphane Lescuyer
 */
public final class PGrammarRule {

	/** Whether this grammar rule is public or not */
	public final boolean visibility;
	/** This rule's return type */
	public final PExtent returnType;
	/** The name of this rule */
	public final Located<String> name;
	/** The generic parameters of the rule */
	public final List<Located<String>> params;
	/** The formal arguments for this rule, if any */
	public final @Nullable PExtent args;
	/** The productions for this rule */
	public final List<@NonNull PProduction> productions;
	
	/**
	 * Builds a grammar rule from the given parameters
	 * @param visibility
	 * @param returnType
	 * @param name
	 * @param params
	 * @param args
	 * @param productions
	 */
	public PGrammarRule(boolean visibility, PExtent returnType,
			Located<String> name, List<Located<String>> params, 
			@Nullable PExtent args, List<PProduction> productions) {
		if (!Character.isLowerCase(name.val.charAt(0)))
			throw new IllegalArgumentException("Rule name should start with a lower case");
		this.visibility = visibility;
		this.returnType = returnType;
		this.name = name;
		this.params = params;
		this.args = args;
		if (productions.isEmpty())
			throw new IllegalArgumentException("Rule should have at least one production");
		this.productions = productions;
	}
	
	/**
	 * @return {@code true} if one of the rule's productions at least
	 * 	has a continuation
	 */
	public boolean hasContinuation() {
		return productions.stream().anyMatch(prod -> prod.continuation() != null);
	}
	
	StringBuilder append(StringBuilder buf) {
		buf.append(visibility ? "public " : "private ");
		buf.append("{").append(returnType.find()).append("}");
		buf.append(" rule ").append(name.val);
		if (!params.isEmpty()) {
			buf.append("<");
			boolean first = true;
			for (Located<String> param : params) {
				if (first) first = false;
				else buf.append(", ");
				buf.append(param.val);
			}
			buf.append(">");
		}
		Extent args_ = args;
		if (args_ != null)
			buf.append("(").append(args_.find()).append(")");
		buf.append(" = ");
		productions.forEach(prod -> {
			buf.append("\n| ").append(prod);
		});
		buf.append(";");
		return buf;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		append(buf);
		return buf.toString();
	}
	
	/**
	 * A builder class for {@linkplain PGrammarRule grammar rules}, 
	 * where productions can be added incrementally
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Builder {
		private final boolean visibility;
		private final PExtent returnType;
		private final Located<String> name;
		private final List<Located<String>> params;
		private final @Nullable PExtent args;
		private final List<PProduction> productions;
		
		/**
		 * Returns a fresh builder with the given parameters
		 * @param visibility
		 * @param returnType
		 * @param name
		 * @param args
		 */
		public Builder(boolean visibility, PExtent returnType,
			Located<String> name, List<Located<String>> params, 
			@Nullable PExtent args) {
			this.visibility = visibility;
			this.returnType = returnType;
			this.name = name;
			this.params = params;
			this.args = args;
			this.productions = new ArrayList<>();
		}
		
		/**
		 * Adds the given production to the builder
		 * @param prod
		 * @return the new state of the builder
		 */
		public Builder addProduction(PProduction prod) {
			this.productions.add(prod);
			return this;
		}
		
		/**
		 * @return the grammar rule from this builder
		 */
		public PGrammarRule build() {
			return new PGrammarRule(visibility, returnType, 
				name, params, args, productions);
		}
	}
}