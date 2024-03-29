package org.stekikun.dolmen.test.examples;
import org.eclipse.jdt.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.stekikun.dolmen.common.Lists;
import org.stekikun.dolmen.common.Maps;

/**
 * Parser generated by Dolmen 1.0.0
 */
public final class JSonParser extends org.stekikun.dolmen.codegen.BaseParser<JSonParser.Token> {
    
    @SuppressWarnings("javadoc")
    public static abstract class Token {
        
        public enum Kind {
            LBRACKET,
            RBRACKET,
            COMMA,
            COLON,
            LSQUARE,
            RSQUARE,
            TRUE,
            FALSE,
            NULL,
            STRING,
            NUMBER,
            EOF;
        }
        
        Token(Kind kind)  {
            this.kind = kind;
        }
        private final Kind kind;
        
        @Override
        public abstract String toString();
        
        public final Kind getKind() { return kind; }
        
        public final static class STRING extends Token {
            public final  String  value;
            
            private STRING( String  value) {
                super(Kind.STRING);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "STRING(" + value + ")";
            }
        }
        public static STRING STRING( String  value) {
            return new STRING(value);
        }
        
        public final static class NUMBER extends Token {
            public final  double  value;
            
            private NUMBER( double  value) {
                super(Kind.NUMBER);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "NUMBER(" + value + ")";
            }
        }
        public static NUMBER NUMBER( double  value) {
            return new NUMBER(value);
        }
        
        private static final class Singleton extends Token {
            private Singleton(Kind kind) { super(kind); }
            
            @Override
            public String toString() {
                return getKind().toString();
            }
        }
        
        public static final Token LBRACKET = new Singleton(Kind.LBRACKET);
        public static final Token RBRACKET = new Singleton(Kind.RBRACKET);
        public static final Token COMMA = new Singleton(Kind.COMMA);
        public static final Token COLON = new Singleton(Kind.COLON);
        public static final Token LSQUARE = new Singleton(Kind.LSQUARE);
        public static final Token RSQUARE = new Singleton(Kind.RSQUARE);
        public static final Token TRUE = new Singleton(Kind.TRUE);
        public static final Token FALSE = new Singleton(Kind.FALSE);
        public static final Token NULL = new Singleton(Kind.NULL);
        public static final Token EOF = new Singleton(Kind.EOF);
    }
    
    
    
	@SuppressWarnings("javadoc")
	public enum ValueKind {
		STRING, NUMBER, OBJECT, ARRAY,
		TRUE, FALSE, NULL;
	}

	@SuppressWarnings("javadoc")
	public static abstract class Value<T> {
		public final T val;
		private Value(T val) { this.val = val; }
		
		@Override public abstract String toString();
		public abstract ValueKind getKind();
	}

	@SuppressWarnings("javadoc")
	public static final class ValueString extends Value<String> {
		private ValueString(String val) {
			super(val);
		}
		
		@Override public String toString() {
			return val;
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.STRING;
		}
	}
	@SuppressWarnings("javadoc")
	public static ValueString valString(String s) {
		return new ValueString(s);
	}
	
	@SuppressWarnings("javadoc")
	public static final class ValueNumber extends Value<Double> {
		private ValueNumber(double val) {
			super(val);
		}
		
		@Override public String toString() {
			return "" + val;
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.NUMBER;
		}
	}
	@SuppressWarnings("javadoc")
	public static ValueNumber valNumber(double val) {
		return new ValueNumber(val);
	}

	@SuppressWarnings("javadoc")
	public static final class ValueObject extends Value<Map<String, Value<?>>> {
		private ValueObject(Map<String, Value<?>> val) {
			super(val);
		}
		
		@Override public String toString() {
			return "" + val;	// TODO better pprint
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.OBJECT;
		}
	}
	@SuppressWarnings("javadoc")
	public static ValueObject valObject(Map<String, Value<?>> obj) {
		return new ValueObject(obj);
	}
	
	@SuppressWarnings("javadoc")
	public static final class ValueArray extends Value<List<Value<?>>> {
		private ValueArray(List<Value<?>> val) {
			super(val);
		}
		
		@Override public String toString() {
			return "" + val;
		}
	
		@Override public ValueKind getKind() {
			return ValueKind.ARRAY;
		}
	}
	@SuppressWarnings("javadoc")
	public static ValueArray valArray(List<Value<?>> arr) {
		return new ValueArray(arr);
	}
	
	@SuppressWarnings("javadoc")
	public static final class ValueTrue extends Value<Boolean> {
		private ValueTrue() {
			super(true);
		}
		
		@Override public String toString() {
			return "true";
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.TRUE;
		}	
	}
	@SuppressWarnings("javadoc")
	public static final ValueTrue valTrue = new ValueTrue();
	
	@SuppressWarnings("javadoc")
	public static final class ValueFalse extends Value<Boolean> {
		private ValueFalse() {
			super(false);
		}
		
		@Override public String toString() {
			return "false";
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.FALSE;
		}	
	}
	@SuppressWarnings("javadoc")
	public static final ValueFalse valFalse = new ValueFalse();
	
	@SuppressWarnings("javadoc")
	public static final class ValueNull extends Value<@Nullable Object> {
		private ValueNull() {
			super(null);
		}
		
		@Override public String toString() {
			return "null";
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.NULL;
		}	
	}
	@SuppressWarnings("javadoc")
	public static final ValueNull valNull = new ValueNull();

    /**
     * Builds a new parser based on the given lexical buffer
     * and tokenizer
     * @param lexbuf
     * @param tokens
     */
    public <T extends org.stekikun.dolmen.codegen.LexBuffer> 
        JSonParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super("1.0.0", lexbuf, tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = eat();
        if (kind != ctoken.getKind())
            throw tokenError(ctoken, kind);
        return ctoken;
    }
    
    /**
     * Entry point for the non-terminal json
     */
    public  Value<?>  json() {
        // v = value
         Value<?>  v = value();
        // EOF
        eat(Token.Kind.EOF);
         return v; 
    }
    
    private  Value<?>  value() {
        switch (peek().getKind()) {
            case FALSE: {
                // FALSE
                eat(Token.Kind.FALSE);
                 return valFalse; 
            }
            case LBRACKET: {
                // o = object
                 Map<String, Value<?>>  o = object();
                 return valObject(o); 
            }
            case LSQUARE: {
                // a = array
                 List<Value<?>>  a = array();
                 return valArray(a); 
            }
            case NULL: {
                // NULL
                eat(Token.Kind.NULL);
                 return valNull; 
            }
            case NUMBER: {
                // n = NUMBER
                 double  n = ((Token.NUMBER) eat(Token.Kind.NUMBER)).value;
                 return valNumber(n); 
            }
            case STRING: {
                // s = STRING
                 String  s = ((Token.STRING) eat(Token.Kind.STRING)).value;
                 return valString(s); 
            }
            case TRUE: {
                // TRUE
                eat(Token.Kind.TRUE);
                 return valTrue; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.FALSE, Token.Kind.LBRACKET, Token.Kind.LSQUARE, Token.Kind.NULL, Token.Kind.NUMBER, Token.Kind.STRING, Token.Kind.TRUE);
            }
        }
    }
    
    private  List<Value<?>>  array() {
        // LSQUARE
        eat(Token.Kind.LSQUARE);
        // elts = elements(null)
         List<Value<?>>  elts = elements(null);
         return elts; 
    }
    
    private  List<Value<?>>  elements(@Nullable List<Value<?>> elts) {
        switch (peek().getKind()) {
            case FALSE:
            case LBRACKET:
            case LSQUARE:
            case NULL:
            case NUMBER:
            case STRING:
            case TRUE: {
                // val = value
                 Value<?>  val = value();
                 List<Value<?>> acc = elts == null ? new ArrayList<>() : elts; 
                 acc.add(val); 
                // more_elements(acc)
                more_elements(acc);
                 return acc; 
            }
            case RSQUARE: {
                // RSQUARE
                eat(Token.Kind.RSQUARE);
                 return elts == null ? Lists.empty() : elts; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.FALSE, Token.Kind.LBRACKET, Token.Kind.LSQUARE, Token.Kind.NULL, Token.Kind.NUMBER, Token.Kind.RSQUARE, Token.Kind.STRING, Token.Kind.TRUE);
            }
        }
    }
    
    private  void  more_elements(List<Value<?>> elts) {
        more_elements:
        while (true) {
            switch (peek().getKind()) {
                case COMMA: {
                    // COMMA
                    eat(Token.Kind.COMMA);
                    // val = value
                     Value<?>  val = value();
                     elts.add(val); 
                    continue more_elements;
                }
                case RSQUARE: {
                    // RSQUARE
                    eat(Token.Kind.RSQUARE);
                     return; 
                }
                //$CASES-OMITTED$
                default: {
                    throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RSQUARE);
                }
            }
        }
    }
    
    private  Map<String, Value<?>>  object() {
        // LBRACKET
        eat(Token.Kind.LBRACKET);
        // members = members(null)
         Map<String, Value<?>>  members = members(null);
         return members; 
    }
    
    private  Map<String, Value<?>>  members(@Nullable Map<String, Value<?>> members) {
        switch (peek().getKind()) {
            case RBRACKET: {
                // RBRACKET
                eat(Token.Kind.RBRACKET);
                 return members == null ? Maps.empty() : members; 
            }
            case STRING: {
                 Map<String, Value<?>> acc = members == null ? new HashMap<>() : members; 
                // pair(acc)
                pair(acc);
                // more_members(acc)
                more_members(acc);
                 return acc; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.RBRACKET, Token.Kind.STRING);
            }
        }
    }
    
    private  void  more_members(Map<String, Value<?>> members) {
        more_members:
        while (true) {
            switch (peek().getKind()) {
                case COMMA: {
                    // COMMA
                    eat(Token.Kind.COMMA);
                    // pair(members)
                    pair(members);
                    continue more_members;
                }
                case RBRACKET: {
                    // RBRACKET
                    eat(Token.Kind.RBRACKET);
                     return; 
                }
                //$CASES-OMITTED$
                default: {
                    throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RBRACKET);
                }
            }
        }
    }
    
    private  void  pair(Map<String, Value<?>> map) {
        // s = STRING
         String  s = ((Token.STRING) eat(Token.Kind.STRING)).value;
        // COLON
        eat(Token.Kind.COLON);
        // val = value
         Value<?>  val = value();
         map.put(s, val); return; 
    }
    
     
    
}
