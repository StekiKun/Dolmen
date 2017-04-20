package jl;

import org.eclipse.jdt.annotation.NonNull;

import syntax.Location;

@SuppressWarnings("javadoc")
public abstract class JLToken {

	public enum Kind {
		IDENT,
		LCHAR,
		LSTRING,
		ACTION,
		RULE,
		SHORTEST,
		EOF,
		AS,
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
	
	public final static class Action extends JLToken {
		public final Location value;
		
		private Action(Location value) {
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
	public static Action ACTION(Location value) {
		return new Action(value);
	}
	
	private static abstract class Singleton extends JLToken {
		private final Kind kind;
		Singleton(Kind kind) { this.kind = kind; }
		
		@SuppressWarnings("null")
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
	public static JLToken HASH= new Singleton(Kind.HASH) {};
	public static JLToken END = new Singleton(Kind.END) {};
	
}
