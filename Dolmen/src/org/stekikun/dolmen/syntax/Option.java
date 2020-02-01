package org.stekikun.dolmen.syntax;

/**
 * An {@link Option} instance represents a configuration
 * option as set in a lexer or parser description file.
 * It binds some {@link String} {@linkplain #value value}
 * to a {@linkplain #value key}.
 * 
 * @author Stéphane Lescuyer
 */
public final class Option {
	
	/** The key identifying this option */
	public final Located<String> key;
	
	/** The value that is associated to the {@linkplain #key key} */
	public final Located<String> value;
	
	private Option(Located<String> key, Located<String> value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * <i>This represents a syntactic option, there is no guarantee that
	 * 	the {@code key} corresponds to any of the option that Dolmen
	 *  actually understands, or that the value makes any sense.</i>
	 * 
	 * @param key
	 * @param value
	 * @return an option associating the given {@code value} to the given {@code key}
	 */
	public static Option of(Located<String> key, Located<String> value) {
		if (key.val.isEmpty())
			throw new IllegalArgumentException("Cannot build option with empty key");
		return new Option(key, value);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(key.val).append(" = ").append(value.val);
		return buf.toString();
	}
}
