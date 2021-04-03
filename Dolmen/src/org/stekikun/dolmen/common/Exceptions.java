package org.stekikun.dolmen.common;

/**
 * Custom exceptions for Dolmen
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Exceptions {

	private Exceptions() {
		// Static utility only
	}
	
	/**
	 * This exception is thrown at run-time when trying to instantiate
	 * generated lexical or syntactic analyzers in the context of a
	 * Dolmen runtime which is different from the one where they were
	 * generated.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class DolmenVersionException extends RuntimeException {
		private static final long serialVersionUID = -2539211402800773517L;
		
		@SuppressWarnings("unused")
		private final String genVersion;
		
		private DolmenVersionException(String what, String genVersion) {
			super(String.format("Dolmen version mismatch: this %s was generated "
					+ "using version %s, but this runtime has version %s.",
					what, genVersion, Constants.VERSION));
			this.genVersion = genVersion;
		}
		
		private static void check(String what, String genVersion) {
			if (Constants.VERSION.equals(genVersion)) return;
			throw new DolmenVersionException(what, genVersion);
		}
		
		/**
		 * Checks that the current runtime has the given version,
		 * and throws a {@link DolmenVersionException} otherwise.
		 * 
		 * @param genVersion
		 */
		public static void checkLexer(String genVersion) {
			check("lexer", genVersion);
		}
		/**
		 * Checks that the current runtime has the given version,
		 * and throws a {@link DolmenVersionException} otherwise.
		 * 
		 * @param genVersion
		 */		public static void checkParser(String genVersion) {
			check("parser", genVersion);
		}
	}
}
