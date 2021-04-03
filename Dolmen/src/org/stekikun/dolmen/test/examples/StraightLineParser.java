package org.stekikun.dolmen.test.examples;

/**
 * Parser generated by Dolmen 1.0.0
 */
public final class StraightLineParser extends org.stekikun.dolmen.codegen.BaseParser<StraightLineParser.Token> {
    
    @SuppressWarnings("javadoc")
    public static abstract class Token {
        
        public enum Kind {
            INT,
            ID,
            PLUS,
            MINUS,
            TIMES,
            DIV,
            SEMICOLON,
            ASSIGN,
            PRINT,
            LPAREN,
            RPAREN,
            COMMA,
            EOF;
        }
        
        Token(Kind kind)  {
            this.kind = kind;
        }
        private final Kind kind;
        
        @Override
        public abstract String toString();
        
        public final Kind getKind() { return kind; }
        
        public final static class INT extends Token {
            public final int value;
            
            private INT(int value) {
                super(Kind.INT);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "INT(" + value + ")";
            }
        }
        public static INT INT(int value) {
            return new INT(value);
        }
        
        public final static class ID extends Token {
            public final String value;
            
            private ID(String value) {
                super(Kind.ID);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ID(" + value + ")";
            }
        }
        public static ID ID(String value) {
            return new ID(value);
        }
        
        private static final class Singleton extends Token {
            private Singleton(Kind kind) { super(kind); }
            
            @Override
            public String toString() {
                return getKind().toString();
            }
        }
        
        public static final Token PLUS = new Singleton(Kind.PLUS);
        public static final Token MINUS = new Singleton(Kind.MINUS);
        public static final Token TIMES = new Singleton(Kind.TIMES);
        public static final Token DIV = new Singleton(Kind.DIV);
        public static final Token SEMICOLON = new Singleton(Kind.SEMICOLON);
        public static final Token ASSIGN = new Singleton(Kind.ASSIGN);
        public static final Token PRINT = new Singleton(Kind.PRINT);
        public static final Token LPAREN = new Singleton(Kind.LPAREN);
        public static final Token RPAREN = new Singleton(Kind.RPAREN);
        public static final Token COMMA = new Singleton(Kind.COMMA);
        public static final Token EOF = new Singleton(Kind.EOF);
    }
    
    
        private java.util.Map<String, Integer> env =
        new java.util.HashMap<>();
    
    private void update(String id, int n) {
        env.put(id, n);
    }
    
    private int lookup(String id) {
        Integer val = env.get(id);
        if (val == null)
            throw parsingError("Undefined identifier: " + id);
        return val;
    }


    /**
     * Builds a new parser based on the given lexical buffer
     * and tokenizer
     * @param lexbuf
     * @param tokens
     */
    public <T extends org.stekikun.dolmen.codegen.LexBuffer> 
        StraightLineParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super("1.0.0", lexbuf, tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = eat();
        if (kind != ctoken.getKind())
            throw tokenError(ctoken, kind);
        return ctoken;
    }
    
    /**
     * Entry point for the non-terminal program
     */
    public void program() {
        // stmts
        stmts();
        // EOF
        eat(Token.Kind.EOF);
        return;
    }
    
    private void stmts() {
        // stmt
        stmt();
        // stmts_rhs
        stmts_rhs();
        return;
    }
    
    private void stmts_rhs() {
        switch (peek().getKind()) {
            case EOF: {
                return;
            }
            case SEMICOLON: {
                // SEMICOLON
                eat(Token.Kind.SEMICOLON);
                // stmts
                stmts();
                return;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.EOF, Token.Kind.SEMICOLON);
            }
        }
    }
    
    private void stmt() {
        switch (peek().getKind()) {
            case ID: {
                // id = ID
                String id = ((Token.ID) eat(Token.Kind.ID)).value;
                // ASSIGN
                eat(Token.Kind.ASSIGN);
                // n = expr
                int n = expr();
                update(id, n); return;
            }
            case PRINT: {
                // PRINT
                eat(Token.Kind.PRINT);
                // LPAREN
                eat(Token.Kind.LPAREN);
                // pexpr
                pexpr();
                // pexprs
                pexprs();
                // RPAREN
                eat(Token.Kind.RPAREN);
                return;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ID, Token.Kind.PRINT);
            }
        }
    }
    
    private void pexprs() {
        switch (peek().getKind()) {
            case COMMA: {
                // COMMA
                eat(Token.Kind.COMMA);
                // pexpr
                pexpr();
                // pexprs
                pexprs();
                return;
            }
            case RPAREN: {
                return;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RPAREN);
            }
        }
    }
    
    private void pexpr() {
        // e = expr
        int e = expr();
        System.out.println(e); return;
    }
    
    private int expr() {
        // n = term
        int n = term();
        return n;
    }
    
    private int term() {
        // n1 = factor
        int n1 = factor();
        // res = term_rhs(n1)
        int res = term_rhs(n1);
        return res;
    }
    
    private int term_rhs(int lhs) {
        switch (peek().getKind()) {
            case COMMA:
            case EOF:
            case RPAREN:
            case SEMICOLON: {
                return lhs;
            }
            case MINUS: {
                // MINUS
                eat(Token.Kind.MINUS);
                // n = term
                int n = term();
                return lhs - n;
            }
            case PLUS: {
                // PLUS
                eat(Token.Kind.PLUS);
                // n = term
                int n = term();
                return lhs + n;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.EOF, Token.Kind.MINUS, Token.Kind.PLUS, Token.Kind.RPAREN, Token.Kind.SEMICOLON);
            }
        }
    }
    
    private int factor() {
        // n1 = atomic
        int n1 = atomic();
        // res = factor_rhs(n1)
        int res = factor_rhs(n1);
        return res;
    }
    
    private int factor_rhs(int lhs) {
        switch (peek().getKind()) {
            case COMMA:
            case EOF:
            case MINUS:
            case PLUS:
            case RPAREN:
            case SEMICOLON: {
                return lhs;
            }
            case DIV: {
                // DIV
                eat(Token.Kind.DIV);
                // n = factor
                int n = factor();
                return lhs / n;
            }
            case TIMES: {
                // TIMES
                eat(Token.Kind.TIMES);
                // n = factor
                int n = factor();
                return lhs * n;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.DIV, Token.Kind.EOF, Token.Kind.MINUS, Token.Kind.PLUS, Token.Kind.RPAREN, Token.Kind.SEMICOLON, Token.Kind.TIMES);
            }
        }
    }
    
    private int atomic() {
        switch (peek().getKind()) {
            case ID: {
                // id = ID
                String id = ((Token.ID) eat(Token.Kind.ID)).value;
                return lookup(id);
            }
            case INT: {
                // n = INT
                int n = ((Token.INT) eat(Token.Kind.INT)).value;
                return n;
            }
            case LPAREN: {
                // LPAREN
                eat(Token.Kind.LPAREN);
                // e = expr
                int e = expr();
                // RPAREN
                eat(Token.Kind.RPAREN);
                return e;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ID, Token.Kind.INT, Token.Kind.LPAREN);
            }
        }
    }
    
    /**
     * Testing this parser
     */
    public static void main(String[] args) throws java.io.IOException {
		String prompt;
       while ((prompt = org.stekikun.dolmen.common.Prompt.getInputLine(">")) != null) {
			try {
				StraightLineLexer lexer = new StraightLineLexer("-",
					new java.io.StringReader(prompt));
				StraightLineParser parser = new StraightLineParser(lexer, StraightLineLexer::main);
				parser.program();
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
	}

    
}
