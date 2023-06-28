package org.stekikun.dolmen.unparam;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.syntax.CExtent;
import org.stekikun.dolmen.syntax.Located;

/**
 * Describes a grammar rule for a non-terminal in a grammar
 * description. The rule can be {@linkplain #visibility public or private}
 * which will impact the visibility of the corresponding generated
 * method, it specifies the {@linkplain #returnType return type} of the
 * associated semantic actions and can have some
 * {@linkplain #args formal arguments}.
 * <p>
 * The relative order of {@linkplain #productions productions} for this
 * grammar rule is the one from the original source but is not
 * semantically relevant for the produced grammar.
 * 
 * @author Stéphane Lescuyer
 */
public final class GrammarRule {

	/** Whether this grammar rule is public or not */
	public final boolean visibility;
	/** This rule's return type */
	public final CExtent returnType;
	/** The name of this rule */
	public final Located<String> name;
	/** The formal arguments for this rule, if any */
	public final @Nullable CExtent args;
	/** The productions for this rule */
	public final List<org.stekikun.dolmen.unparam.Production> productions;
	
	/**
	 * Builds a grammar rule from the given parameters
	 * @param visibility
	 * @param returnType
	 * @param name
	 * @param args
	 * @param productions
	 */
	public GrammarRule(boolean visibility, CExtent returnType,
			Located<String> name, @Nullable CExtent args, List<Production> productions) {
		if (!Character.isLowerCase(name.val.charAt(0)))
			throw new IllegalArgumentException("Rule name should start with a lower case");
		this.visibility = visibility;
		this.returnType = returnType;
		this.name = name;
		this.args = args;
		assert (!productions.isEmpty());
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
		@Nullable CExtent args_ = args;
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
	 * A builder class for {@linkplain GrammarRule grammar rules}, 
	 * where productions can be added incrementally
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Builder {
		private final boolean visibility;
		private final CExtent returnType;
		private final Located<String> name;
		private final @Nullable CExtent args;
		private final List<Production> productions;
		
		/**
		 * Returns a fresh builder with the given parameters
		 * @param visibility
		 * @param returnType
		 * @param name
		 * @param args
		 */
		public Builder(boolean visibility, CExtent returnType,
			Located<String> name, @Nullable CExtent args) {
			this.visibility = visibility;
			this.returnType = returnType;
			this.name = name;
			this.args = args;
			this.productions = new ArrayList<>();
		}
		
		/**
		 * Adds the given production to the builder
		 * @param prod
		 * @return the new state of the builder
		 */
		public Builder addProduction(Production prod) {
			this.productions.add(prod);
			return this;
		}
		
		/**
		 * @return the grammar rule from this builder
		 */
		public GrammarRule build() {
			return new GrammarRule(visibility, returnType, 
				name, args, productions);
		}
	}
}