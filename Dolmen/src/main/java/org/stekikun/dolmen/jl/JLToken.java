package org.stekikun.dolmen.jl;

import org.eclipse.jdt.annotation.NonNull;
import org.stekikun.dolmen.syntax.Extent;

@SuppressWarnings("javadoc")
public abstract class JLToken {

	public enum Kind {
		IDENT,
		LCHAR,
		LSTRING,
		INTEGER,
		ACTION,
		RULE,
		SHORTEST,
		PUBLIC,
		PRIVATE,
		EOF,
		AS,
		ORELSE,
		IMPORT,
		STATIC,
		UNDERSCORE,
		EQUAL,
		OR,
		LBRACKET,
		RBRACKET,
		STAR,
		MAYBE,
		PLUS,
		LPAREN,
		RPAREN,
		CARET,
		DASH,
		HASH,
		DOT,
		LANGLE,
		RANGLE,
		COMMA,
		SEMICOL,
		END
	}
	
	private JLToken() {
		// nothing to do (later, positions)
	}

	@Override
	public abstract String toString();
	
	public abstract Kind getKind();
	
	public final static class LString extends JLToken {
		public final String value;
		
		private LString(String value) {
			this.value = value;
		}

		@Override
		public @NonNull String toString() {
			return "LSTRING(" + value + ")";
		}

		@Override
		public Kind getKind() {
			return Kind.LSTRING;
		}
	}
	public static LString LSTRING(String value) {
		return new LString(value);
	}
	
	public final static class LChar extends JLToken {
		public final char value;
		
		private LChar(char value) {
			this.value = value;
		}
		
		@Override
		public @NonNull String toString() {
			return "LCHAR(" + value + ")";
		}
		
		@Override
		public Kind getKind() {
			return Kind.LCHAR;
		}
	}
	public static LChar LCHAR(char value) {
		return new LChar(value);
	}
	
	public final static class Ident extends JLToken {
		public final String value;
		
		private Ident(String value) {
			this.value = value;
		}

		@Override
		public @NonNull String toString() {
			return "IDENT(" + value + ")";
		}

		@Override
		public Kind getKind() {
			return Kind.IDENT;
		}		
	}
	public static Ident IDENT(String value) {
		return new Ident(value);
	}

	public final static class INTEGER extends JLToken {
		public final int value;
		
		private INTEGER(int value) {
			this.value = value;
		}

		@Override
		public @NonNull String toString() {
			return "INTEGER(" + value + ")";
		}

		@Override
		public Kind getKind() {
			return Kind.INTEGER;
		}		
	}
	public static INTEGER INTEGER(int value) {
		return new INTEGER(value);
	}
	
	public final static class Action extends JLToken {
		public final Extent value;
		
		private Action(Extent value) {
			this.value = value;
		}
		
		@Override
		public @NonNull String toString() {
			return "ACTION(" + value + ")";
		}
		
		@Override
		public Kind getKind() {
			return Kind.ACTION;
		}
	}
	public static Action ACTION(Extent value) {
		return new Action(value);
	}
	
	private static abstract class Singleton extends JLToken {
		private final Kind kind;
		Singleton(Kind kind) { this.kind = kind; }
		
		@Override
		public @NonNull String toString() {
			return kind.toString();
		}
		
		@Override
		public @NonNull Kind getKind() {
			return kind;
		}
	}
	
	public static JLToken RULE = new Singleton(Kind.RULE) {};
	public static JLToken SHORTEST = new Singleton(Kind.SHORTEST) {};
	public static JLToken EOF = new Singleton(Kind.EOF) {};
	public static JLToken AS = new Singleton(Kind.AS) {};
	public static JLToken ORELSE = new Singleton(Kind.ORELSE) {};
	public static JLToken IMPORT = new Singleton(Kind.IMPORT) {};
	public static JLToken STATIC = new Singleton(Kind.STATIC) {};
	public static JLToken PUBLIC = new Singleton(Kind.PUBLIC) {};
	public static JLToken PRIVATE = new Singleton(Kind.PRIVATE) {};
	public static JLToken UNDERSCORE = new Singleton(Kind.UNDERSCORE) {};
	public static JLToken EQUAL = new Singleton(Kind.EQUAL) {};
	public static JLToken OR = new Singleton(Kind.OR) {};
	public static JLToken LBRACKET = new Singleton(Kind.LBRACKET) {};
	public static JLToken RBRACKET = new Singleton(Kind.RBRACKET) {};
	public static JLToken STAR = new Singleton(Kind.STAR) {};
	public static JLToken MAYBE = new Singleton(Kind.MAYBE) {};
	public static JLToken PLUS = new Singleton(Kind.PLUS) {};
	public static JLToken LPAREN = new Singleton(Kind.LPAREN) {};
	public static JLToken RPAREN = new Singleton(Kind.RPAREN) {};
	public static JLToken CARET = new Singleton(Kind.CARET) {};
	public static JLToken DASH = new Singleton(Kind.DASH) {};
	public static JLToken HASH = new Singleton(Kind.HASH) {};
	public static JLToken DOT = new Singleton(Kind.DOT) {};
	public static JLToken LANGLE = new Singleton(Kind.LANGLE) {};
	public static JLToken RANGLE = new Singleton(Kind.RANGLE) {};
	public static JLToken COMMA = new Singleton(Kind.COMMA) {};
	public static JLToken SEMICOL = new Singleton(Kind.SEMICOL) {};
	public static JLToken END = new Singleton(Kind.END) {};
	
}
