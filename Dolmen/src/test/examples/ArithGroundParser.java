package test.examples;
/**
 * Parser generated by Dolmen 
 */
@SuppressWarnings("javadoc")
@org.eclipse.jdt.annotation.NonNullByDefault({})
public final class ArithGroundParser extends codegen.BaseParser<ArithGroundParser.Token> {
    
    public static abstract class Token {
        
        public enum Kind {
            INT,
            PLUS,
            MINUS,
            TIMES,
            EOF;
        }
        
        private Token()  {
            // nothing to do
        }
        
        @Override
        public abstract String toString();
        
        public abstract Kind getKind();
        
        public final static class INT extends Token {
            public final int value;
            
            private INT(int value) {
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "INT(" + value + ")";
            }
            
            @Override
            public Kind getKind() {
                return Kind.INT;
            }
        }
        public static INT INT(int value) {
            return new INT(value);
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
        
        public static final Token PLUS = new Singleton(Kind.PLUS) {};
        public static final Token MINUS = new Singleton(Kind.MINUS) {};
        public static final Token TIMES = new Singleton(Kind.TIMES) {};
        public static final Token EOF = new Singleton(Kind.EOF) {};
    }
    
    
    
    @SuppressWarnings("null")
    public <T extends codegen.LexBuffer>ArithGroundParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super(lexbuf, tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = eat();
        if (kind != ctoken.getKind())
            throw tokenError(ctoken, kind);
        return ctoken;
    }
    
    public int start() {
        
        // n = exp
        int n = exp();
        // EOF
        eat(Token.Kind.EOF);
        return n;
    }
    
    private int exp() {
        
        // n1 = factor
        int n1 = factor();
        // n2 = exp_rhs
        int n2 = exp_rhs();
        return n1 + n2;
    }
    
    private int exp_rhs() {
        switch (peek().getKind()) {
            case EOF: {
                return 0;
            }
            case MINUS: {
                // MINUS
                eat(Token.Kind.MINUS);
                // n = exp
                int n = exp();
                return -n;
            }
            case PLUS: {
                // PLUS
                eat(Token.Kind.PLUS);
                // n = exp
                int n = exp();
                return n;
            }
            default: {
                throw tokenError(peek(), Token.Kind.EOF, Token.Kind.MINUS, Token.Kind.PLUS);
            }
        }
    }
    
    private int factor() {
        
        // n1 = atomic
        int n1 = atomic();
        // n2 = factor_rhs
        int n2 = factor_rhs();
        return n1 * n2;
    }
    
    private int factor_rhs() {
        switch (peek().getKind()) {
            case EOF:
            case MINUS:
            case PLUS: {
                return 1;
            }
            case TIMES: {
                // TIMES
                eat(Token.Kind.TIMES);
                // n = factor
                int n = factor();
                return n;
            }
            default: {
                throw tokenError(peek(), Token.Kind.EOF, Token.Kind.MINUS, Token.Kind.PLUS, Token.Kind.TIMES);
            }
        }
    }
    
    private int atomic() {
        switch (peek().getKind()) {
            case INT: {
                // n = INT
                int n = ((Token.INT) eat(Token.Kind.INT)).value;
                return n;
            }
            case MINUS: {
                // MINUS
                eat(Token.Kind.MINUS);
                // n = atomic
                int n = atomic();
                return -n;
            }
            default: {
                throw tokenError(peek(), Token.Kind.INT, Token.Kind.MINUS);
            }
        }
    }
    
    /**
     * Testing this parser
     */
    public static void main(String[] args) throws java.io.IOException {
		String prompt;
       while ((prompt = common.Prompt.getInputLine(">")) != null) {
			try {
				ArithGroundLexer lexer = new ArithGroundLexer("-",
					new java.io.StringReader(prompt));
				ArithGroundParser parser = new ArithGroundParser(lexer, ArithGroundLexer::main);
				int e = parser.exp();
				System.out.println(e);
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
	}

    
}
