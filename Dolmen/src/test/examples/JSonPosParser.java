package test.examples;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import common.Lists;
import common.Maps;
import codegen.LexBuffer.Position;
/**
 * Parser generated by Dolmen 
 */
@SuppressWarnings("javadoc")
@org.eclipse.jdt.annotation.NonNullByDefault({})
public final class JSonPosParser extends codegen.BaseParser.WithPositions<JSonPosParser.Token> {
    
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
            OBJECT,
            ARRAY,
            STRING,
            NUMBER,
            EOF;
        }
        
        private Token()  {
            // nothing to do
        }
        
        @Override
        public abstract String toString();
        
        public abstract Kind getKind();
        
        public final static class STRING extends Token {
            public final  String  value;
            
            private STRING( String  value) {
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "STRING(" + value + ")";
            }
            
            @Override
            public Kind getKind() {
                return Kind.STRING;
            }
        }
        public static STRING STRING( String  value) {
            return new STRING(value);
        }
        
        public final static class NUMBER extends Token {
            public final  double  value;
            
            private NUMBER( double  value) {
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "NUMBER(" + value + ")";
            }
            
            @Override
            public Kind getKind() {
                return Kind.NUMBER;
            }
        }
        public static NUMBER NUMBER( double  value) {
            return new NUMBER(value);
        }
        
        private static abstract class Singleton extends Token {
            private final Kind kind;
            private Singleton(Kind kind) { this.kind = kind; }
            
            @Override
            public String toString() {
                return kind.toString();
            }
            
            @Override
            public Kind getKind() {
                return kind;
            }
        }
        
        public static final Token LBRACKET = new Singleton(Kind.LBRACKET) {};
        public static final Token RBRACKET = new Singleton(Kind.RBRACKET) {};
        public static final Token COMMA = new Singleton(Kind.COMMA) {};
        public static final Token COLON = new Singleton(Kind.COLON) {};
        public static final Token LSQUARE = new Singleton(Kind.LSQUARE) {};
        public static final Token RSQUARE = new Singleton(Kind.RSQUARE) {};
        public static final Token TRUE = new Singleton(Kind.TRUE) {};
        public static final Token FALSE = new Singleton(Kind.FALSE) {};
        public static final Token NULL = new Singleton(Kind.NULL) {};
        public static final Token OBJECT = new Singleton(Kind.OBJECT) {};
        public static final Token ARRAY = new Singleton(Kind.ARRAY) {};
        public static final Token EOF = new Singleton(Kind.EOF) {};
    }
    
    
    
	public enum ValueKind {
		STRING, NUMBER, OBJECT, ARRAY,
		TRUE, FALSE, NULL;
	}
	
	public static class Located<T> {
	   public final Position start;
	   public final Position end;
	   public final T val;
	   
	   Located(Position start, Position end, T val) {
	       this.start = start;
	       this.end = end;
	       this.val = val;
	   }
	   
	   private static StringBuilder appendPos(StringBuilder buf, Position pos) {
	       int n = pos.filename.lastIndexOf('/');
	       String file = n == -1 ? pos.filename : pos.filename.substring(n + 1); 
           buf.append(file).append("[");
           buf.append(pos.line).append(",")
              .append(pos.bol).append("+").append(pos.offset - pos.bol);
           buf.append("]");
           return buf;
	   }
	   
	   public static String locToString(Position start, Position end) {
	       StringBuilder buf = new StringBuilder();
	       buf.append("(");
	       appendPos(buf, start).append("..");
	       appendPos(buf, end).append(")");
	       return buf.toString();
	   }
	   
	   @Override
	   public String toString() {
	       return java.util.Objects.toString(val) + " " + locToString(start, end);
	   }
	}

	public static abstract class Value<T> extends Located<T> {
		private Value(Position start, Position end, T val) {
		    super(start, end, val);
		}
		
		public abstract ValueKind getKind();
	}

	public static final class ValueString extends Value<String> {
		private ValueString(Position start, Position end, String val) {
			super(start, end, val);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.STRING;
		}
	}
	public static ValueString valString(Position start, Position end, String s) {
		return new ValueString(start, end, s);
	}

	public static final class ValueNumber extends Value<Double> {
		private ValueNumber(Position start, Position end, double val) {
			super(start, end, val);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.NUMBER;
		}
	}
	public static ValueNumber valNumber(Position start, Position end, double val) {
		return new ValueNumber(start, end, val);
	}
	
	public static final class ValueObject extends Value<Map<Located<String>, Value<?>>> {
		private ValueObject(Position start, Position end, Map<Located<String>, Value<?>> val) {
			super(start, end, val);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.OBJECT;
		}
	}
	public static ValueObject valObject(Position start, Position end,
	       Map<Located<String>, Value<?>> obj) {
		return new ValueObject(start, end, obj);
	}
	
	public static final class ValueArray extends Value<List<Value<?>>> {
		private ValueArray(Position start, Position end, List<Value<?>> val) {
			super(start, end, val);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.ARRAY;
		}
	}
	public static ValueArray valArray(Position start, Position end, List<Value<?>> arr) {
		return new ValueArray(start, end, arr);
	}
	
	public static final class ValueTrue extends Value<Boolean> {
		private ValueTrue(Position start, Position end) {
			super(start, end, true);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.TRUE;
		}	
	}
	public static ValueTrue valTrue(Position start, Position end) {
	   return new ValueTrue(start, end);
	}
	
	public static final class ValueFalse extends Value<Boolean> {
		private ValueFalse(Position start, Position end) {
			super(start, end, false);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.FALSE;
		}	
	}
	public static ValueFalse valFalse(Position start, Position end) {
	   return new ValueFalse(start, end);
	}
	
	public static final class ValueNull extends Value<Object> {
		private ValueNull(Position start, Position end) {
			super(start, end, null);
		}
		
		@Override public ValueKind getKind() {
			return ValueKind.NULL;
		}	
	}
	public static ValueNull valNull(Position start, Position end) {
	   return new ValueNull(start, end);
    }

    @SuppressWarnings("null")
    public <T extends codegen.LexBuffer>JSonPosParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super(lexbuf, tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = peek();
        if (kind != ctoken.getKind())
        throw tokenError(ctoken, kind);
        _jl_nextToken = null;
        return ctoken;
    }
    
    public  Value<?>  json() {
        
        enter(2);
        // v = value
         Value<?>  v = value();
        leave("v");
        // EOF
        eat(Token.Kind.EOF);
        shift(null);
         return v; 
    }
    
    private  Value<?>  value() {
        switch (peek().getKind()) {
            case ARRAY:
            case LSQUARE: {
                enter(2);
                // array_kwd
                array_kwd();
                leave(null);
                // a = array
                 List<Value<?>>  a = array();
                leave("a");
                 return valArray(getSymbolStartPos(), getEndPos(), a); 
            }
            case FALSE: {
                enter(1);
                // FALSE
                eat(Token.Kind.FALSE);
                shift(null);
                 return valFalse(getStartPos(), getEndPos()); 
            }
            case LBRACKET:
            case OBJECT: {
                enter(2);
                // object_kwd
                object_kwd();
                leave(null);
                // o = object
                 Map<Located<String>, Value<?>>  o = object();
                leave("o");
                 return valObject(getSymbolStartPos(), getEndPos(), o); 
            }
            case NULL: {
                enter(1);
                // NULL
                eat(Token.Kind.NULL);
                shift(null);
                 return valNull(getStartPos(), getEndPos()); 
            }
            case NUMBER: {
                enter(1);
                // n = NUMBER
                 double  n = ((Token.NUMBER) eat(Token.Kind.NUMBER)).value;
                shift("n");
                 return valNumber(getStartPos(), getEndPos(), n); 
            }
            case STRING: {
                enter(1);
                // s = STRING
                 String  s = ((Token.STRING) eat(Token.Kind.STRING)).value;
                shift("s");
                 return valString(getStartPos(), getEndPos(), s); 
            }
            case TRUE: {
                enter(1);
                // TRUE
                eat(Token.Kind.TRUE);
                shift(null);
                 return valTrue(getStartPos(), getEndPos()); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ARRAY, Token.Kind.FALSE, Token.Kind.LBRACKET, Token.Kind.LSQUARE, Token.Kind.NULL, Token.Kind.NUMBER, Token.Kind.OBJECT, Token.Kind.STRING, Token.Kind.TRUE);
            }
        }
    }
    
    private  void  array_kwd() {
        switch (peek().getKind()) {
            case ARRAY: {
                enter(1);
                // ARRAY
                eat(Token.Kind.ARRAY);
                shift(null);
                 return; 
            }
            case LSQUARE: {
                enter(0);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ARRAY, Token.Kind.LSQUARE);
            }
        }
    }
    
    private  List<Value<?>>  array() {
        
        enter(2);
        // LSQUARE
        eat(Token.Kind.LSQUARE);
        shift(null);
        // elts = elements(null)
         List<Value<?>>  elts = elements(null);
        leave("elts");
         return elts; 
    }
    
    private  List<Value<?>>  elements(List<Value<?>> elts) {
        switch (peek().getKind()) {
            case ARRAY:
            case FALSE:
            case LBRACKET:
            case LSQUARE:
            case NULL:
            case NUMBER:
            case OBJECT:
            case STRING:
            case TRUE: {
                enter(2);
                // val = value
                 Value<?>  val = value();
                leave("val");
                 List<Value<?>> acc = elts == null ? new ArrayList<>() : elts; 
                 acc.add(val); 
                // more_elements(acc)
                more_elements(acc);
                leave(null);
                 return acc; 
            }
            case RSQUARE: {
                enter(1);
                // RSQUARE
                eat(Token.Kind.RSQUARE);
                shift(null);
                 return elts == null ? Lists.empty() : elts; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ARRAY, Token.Kind.FALSE, Token.Kind.LBRACKET, Token.Kind.LSQUARE, Token.Kind.NULL, Token.Kind.NUMBER, Token.Kind.OBJECT, Token.Kind.RSQUARE, Token.Kind.STRING, Token.Kind.TRUE);
            }
        }
    }
    
    private  void  more_elements(List<Value<?>> elts) {
        switch (peek().getKind()) {
            case COMMA: {
                enter(3);
                // COMMA
                eat(Token.Kind.COMMA);
                shift(null);
                // val = value
                 Value<?>  val = value();
                leave("val");
                 elts.add(val); 
                // more_elements(elts)
                more_elements(elts);
                leave(null);
                 return; 
            }
            case RSQUARE: {
                enter(1);
                // RSQUARE
                eat(Token.Kind.RSQUARE);
                shift(null);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RSQUARE);
            }
        }
    }
    
    private  void  object_kwd() {
        switch (peek().getKind()) {
            case LBRACKET: {
                enter(0);
                 return; 
            }
            case OBJECT: {
                enter(1);
                // OBJECT
                eat(Token.Kind.OBJECT);
                shift(null);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.LBRACKET, Token.Kind.OBJECT);
            }
        }
    }
    
    private  Map<Located<String>, Value<?>>  object() {
        
        enter(2);
        // LBRACKET
        eat(Token.Kind.LBRACKET);
        shift(null);
        // members = members(null)
         Map<Located<String>, Value<?>>  members = members(null);
        leave("members");
         return members; 
    }
    
    private  Map<Located<String>, Value<?>>  members(Map<Located<String>, Value<?>> members) {
        switch (peek().getKind()) {
            case RBRACKET: {
                enter(1);
                // RBRACKET
                eat(Token.Kind.RBRACKET);
                shift(null);
                 return members == null ? Maps.empty() : members; 
            }
            case STRING: {
                enter(2);
                 Map<Located<String>, Value<?>> acc = members == null ? new LinkedHashMap<>() : members; 
                // pair(acc)
                pair(acc);
                leave(null);
                // more_members(acc)
                more_members(acc);
                leave(null);
                 return acc; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.RBRACKET, Token.Kind.STRING);
            }
        }
    }
    
    private  void  more_members(Map<Located<String>, Value<?>> members) {
        switch (peek().getKind()) {
            case COMMA: {
                enter(3);
                // COMMA
                eat(Token.Kind.COMMA);
                shift(null);
                // pair(members)
                pair(members);
                leave(null);
                // more_members(members)
                more_members(members);
                leave(null);
                 return; 
            }
            case RBRACKET: {
                enter(1);
                // RBRACKET
                eat(Token.Kind.RBRACKET);
                shift(null);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RBRACKET);
            }
        }
    }
    
    private  void  pair(Map<Located<String>, Value<?>> map) {
        
        enter(3);
        // s = STRING
         String  s = ((Token.STRING) eat(Token.Kind.STRING)).value;
        shift("s");
        // COLON
        eat(Token.Kind.COLON);
        shift(null);
        // val = value
         Value<?>  val = value();
        leave("val");
         Located<String> key = new Located<String>(getStartPos(1), getEndPos("s"), s); 
         map.put(key, val); return; 
    }
    
     
    
}
