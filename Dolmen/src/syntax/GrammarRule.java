package syntax;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes a grammar rule for a non-terminal in a grammar
 * description. The rule can be {@link #visibility public or private}
 * which will impact the visibility of the corresponding generated
 * method, it specifies the {@link #returnType return type} of the
 * associated semantic actions and can have some {@link #args formal arguments}.
 * <p>
 * The relative order of {@link #productions productions} for this grammar rule
 * is the one from the original source but is not semantically relevant for
 * the produced grammar.
 * 
 * @author Stéphane Lescuyer
 */
public final class GrammarRule {

	/** Whether this grammar rule is public or not */
	public final boolean visibility;
	/** This rule's return type */
	public final Location returnType;
	/** The name of this rule */
	public final String name;
	/** The formal arguments for this rule, if any */
	public final @Nullable Location args;
	/** The productions for this rule */
	public final List<@NonNull Production> productions;
	
	/**
	 * Builds a grammar rule from the given parameters
	 * @param visibility
	 * @param returnType
	 * @param name
	 * @param args
	 * @param productions
	 */
	public GrammarRule(boolean visibility, Location returnType,
			String name, @Nullable Location args, List<Production> productions) {
		this.visibility = visibility;
		this.returnType = returnType;
		this.name = name;
		this.args = args;
		assert (!productions.isEmpty());
		this.productions = productions;
	}
	
	StringBuilder append(StringBuilder buf) {
		buf.append(visibility ? "public " : "private ");
		buf.append(name);
		Location args_ = args;
		if (args_ == null) buf.append("()");
		else buf.append("(").append(args_.find()).append(")");
		buf.append(" : ").append(returnType.find());
		buf.append(" = ");
		productions.forEach(prod -> {
			buf.append("\n| ").append(prod);
		});
		return buf;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		append(buf);
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}
	
}