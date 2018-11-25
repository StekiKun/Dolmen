package syntax;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A token declaration describes the token
 * {@linkplain #name name}, which is the name
 * used in grammar rules to denote that terminal,
 * and the potential {@linkplain #valueType value type}
 * associated to these tokens.
 * 
 * @author Stéphane Lescuyer
 */
public final class TokenDecl {
	
	/** The name of this token */
	public final Located<String> name;
	/**
	 * If non-null, the extent of the type of Java values
	 * associated to this token at run-time
	 */
	public final @Nullable Extent valueType;
	
	/**
	 * Builds the token declaration with the given
	 * name and value type
	 * @param name
	 * @param valueType
	 */
	public TokenDecl(Located<String> name, @Nullable Extent valueType) {
		if (name.val.chars().anyMatch(ch -> Character.isLowerCase(ch)))
			throw new IllegalArgumentException("Token name should not contain lower case");

		this.name = name;
		this.valueType = valueType;
	}
	
	/**
	 * @return {@code true} iff tokens of this type bear
	 * 	a semantic value at run-time
	 */
	public boolean isValued() {
		return valueType != null;
	}
	
	@Override
	public String toString() {
		@Nullable Extent valueType_ = valueType;
		return "token " +
				(valueType_ == null ? "" : "{" + valueType_.find() + "} ") +
				name.val;
	}
}