package jge;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;
import java.util.List;
import java.util.ArrayList;
import common.Lists;
import common.Java;
import syntax.Extent;
import syntax.Located;
import syntax.Production;
import syntax.Option;
import syntax.Grammar.TokenDecl;
import syntax.GrammarRule;
import syntax.Grammar;

/**
 * Parser generated by Dolmen 1.0.0
 */
public final class JGEParser extends codegen.BaseParser<JGEParser.Token> {
    
    @SuppressWarnings("javadoc")
    public static abstract class Token {
        
        public enum Kind {
            IDENT,
            ACTION,
            ARGUMENTS,
            STRING,
            EQUAL,
            LSQUARE,
            RSQUARE,
            BAR,
            DOT,
            STAR,
            SEMICOL,
            IMPORT,
            STATIC,
            PUBLIC,
            PRIVATE,
            TOKEN,
            RULE,
            EOF;
        }
        
        private Token(Kind kind)  {
            this.kind = kind;
        }
        private final Kind kind;
        
        @Override
        public abstract String toString();
        
        public final Kind getKind() { return kind; }
        
        public final static class IDENT extends Token {
            public final String value;
            
            private IDENT(String value) {
                super(Kind.IDENT);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "IDENT(" + value + ")";
            }
        }
        public static IDENT IDENT(String value) {
            return new IDENT(value);
        }
        
        public final static class ACTION extends Token {
            public final Extent value;
            
            private ACTION(Extent value) {
                super(Kind.ACTION);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ACTION(" + value + ")";
            }
        }
        public static ACTION ACTION(Extent value) {
            return new ACTION(value);
        }
        
        public final static class ARGUMENTS extends Token {
            public final Extent value;
            
            private ARGUMENTS(Extent value) {
                super(Kind.ARGUMENTS);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ARGUMENTS(" + value + ")";
            }
        }
        public static ARGUMENTS ARGUMENTS(Extent value) {
            return new ARGUMENTS(value);
        }
        
        public final static class STRING extends Token {
            public final String value;
            
            private STRING(String value) {
                super(Kind.STRING);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "STRING(" + value + ")";
            }
        }
        public static STRING STRING(String value) {
            return new STRING(value);
        }
        
        private static final class Singleton extends Token {
            private Singleton(Kind kind) { super(kind); }
            
            @Override
            public String toString() {
                return getKind().toString();
            }
        }
        
        public static final Token EQUAL = new Singleton(Kind.EQUAL);
        public static final Token LSQUARE = new Singleton(Kind.LSQUARE);
        public static final Token RSQUARE = new Singleton(Kind.RSQUARE);
        public static final Token BAR = new Singleton(Kind.BAR);
        public static final Token DOT = new Singleton(Kind.DOT);
        public static final Token STAR = new Singleton(Kind.STAR);
        public static final Token SEMICOL = new Singleton(Kind.SEMICOL);
        public static final Token IMPORT = new Singleton(Kind.IMPORT);
        public static final Token STATIC = new Singleton(Kind.STATIC);
        public static final Token PUBLIC = new Singleton(Kind.PUBLIC);
        public static final Token PRIVATE = new Singleton(Kind.PRIVATE);
        public static final Token TOKEN = new Singleton(Kind.TOKEN);
        public static final Token RULE = new Singleton(Kind.RULE);
        public static final Token EOF = new Singleton(Kind.EOF);
    }
    
    
       /**
     * Returns {@code true} if the given string contains a lower-case letter
     */
    private static boolean isLowerId(String name) {
        return name.chars().anyMatch(ch -> Character.isLowerCase(ch));
    }

    private Production.Actual actual(@Nullable Located<String> binding,
            Located<String> ident, @Nullable Extent args) {
        if (args != null && Character.isUpperCase(ident.val.charAt(0)))
            throw new ParsingException(ident.start, ident.length(),
                "Terminal " + ident.val + " does not expect arguments.");
        return new Production.Actual(binding, ident, args);
    }

    /**
     * @param t
     * @return the given value wrapped with the location of the last
     * 	consumed token
     */
    private <@NonNull T> Located<T> withLoc(T t) {
	     return Located.of(t, _jl_lastTokenStart, _jl_lastTokenEnd);
    }
    
    private String validJavaIdent(String id) {
    	if (Java.keywordSet.contains(id))
    		throw parsingError("Invalid name: reserved Java identifier");
    	return id;
    }

    /**
     * Builds a new parser based on the given lexical buffer
     * and tokenizer
     * @param lexbuf
     * @param tokens
     */
    public <T extends codegen.LexBuffer> 
        JGEParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super(lexbuf, tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = eat();
        if (kind != ctoken.getKind())
            throw tokenError(ctoken, kind);
        return ctoken;
    }
    
    /**
     * Entry point for the non-terminal start
     */
    public Grammar start() {
        // options = options(null)
        List<Option> options = options(null);
        // imports = imports(null)
        List<Located<String>> imports = imports(null);
        // tdecls = tokens(null)
        List<TokenDecl> tdecls = tokens(null);
        // header = ACTION
        Extent header = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // rules = rules(null)
        List<GrammarRule> rules = rules(null);
        // footer = ACTION
        Extent footer = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // EOF
        eat(Token.Kind.EOF);
        Grammar.Builder builder = new Grammar.Builder(options, imports, header, footer);
        tdecls.forEach(tdecl -> builder.addToken(tdecl));
        rules.forEach(rule -> builder.addRule(rule));
        return builder.build();
    }
    
    private List<Option> options(@Nullable List<Option> opts) {
        options:
        while (true) {
            switch (peek().getKind()) {
                case ACTION:
                case IMPORT:
                case TOKEN: {
                     return opts == null ? Lists.empty() : opts; 
                }
                case LSQUARE: {
                    // LSQUARE
                    eat(Token.Kind.LSQUARE);
                     List<Option> acc = opts == null ? new ArrayList<>() : opts; 
                    // key = IDENT
                    String key = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                     Located<String> lkey = withLoc(key); 
                    // EQUAL
                    eat(Token.Kind.EQUAL);
                    // value = STRING
                    String value = ((Token.STRING) eat(Token.Kind.STRING)).value;
                     Located<String> lvalue = withLoc(value); 
                    // RSQUARE
                    eat(Token.Kind.RSQUARE);
                     acc.add(Option.of(lkey, lvalue)); 
                    // options(acc)
                    options(acc);
                     return acc; 
                }
                default: {
                    break options;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IMPORT, Token.Kind.LSQUARE, Token.Kind.TOKEN);
    }
    
    private List<Located<String>> imports(@Nullable List<Located<String>> imp) {
        imports:
        while (true) {
            switch (peek().getKind()) {
                case ACTION:
                case TOKEN: {
                    return imp == null ? Lists.empty() : imp;
                }
                case IMPORT: {
                    List<Located<String>> acc = imp == null ? new ArrayList<>() : imp;
                    // IMPORT
                    eat(Token.Kind.IMPORT);
                    codegen.LexBuffer.Position start = _jl_lastTokenStart;
                    // elt = import_
                    String elt = import_();
                    // SEMICOL
                    eat(Token.Kind.SEMICOL);
                    acc.add(Located.of("import " + elt + ";", start, _jl_lastTokenEnd));
                    // imports(acc)
                    imports(acc);
                    return acc;
                }
                default: {
                    break imports;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IMPORT, Token.Kind.TOKEN);
    }
    
    private String import_() {
        import_:
        while (true) {
            switch (peek().getKind()) {
                case IDENT: {
                    // id = IDENT
                    String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                    // tn = typename
                    String tn = typename();
                    return id + tn;
                }
                case STATIC: {
                    // STATIC
                    eat(Token.Kind.STATIC);
                    // id = IDENT
                    String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                    // tn = typename
                    String tn = typename();
                    return "static " + id + tn;
                }
                default: {
                    break import_;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.STATIC);
    }
    
    private String typename() {
        typename:
        while (true) {
            switch (peek().getKind()) {
                case DOT: {
                    // DOT
                    eat(Token.Kind.DOT);
                    // ty = typename0
                    String ty = typename0();
                    return "." + ty;
                }
                case SEMICOL: {
                    return "";
                }
                default: {
                    break typename;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.DOT, Token.Kind.SEMICOL);
    }
    
    private String typename0() {
        typename0:
        while (true) {
            switch (peek().getKind()) {
                case IDENT: {
                    // id = IDENT
                    String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                    // ty = typename
                    String ty = typename();
                    return id + ty;
                }
                case STAR: {
                    // STAR
                    eat(Token.Kind.STAR);
                    return "*";
                }
                default: {
                    break typename0;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.STAR);
    }
    
    private List<TokenDecl> tokens(@Nullable List<TokenDecl> tokens) {
        tokens:
        while (true) {
            switch (peek().getKind()) {
                case ACTION: {
                    return Lists.empty();
                }
                case TOKEN: {
                    List<TokenDecl> acc = tokens == null ? new ArrayList<>() : tokens;
                    // TOKEN
                    eat(Token.Kind.TOKEN);
                    // tok = token_
                    TokenDecl tok = token_();
                    acc.add(tok);
                    // tokens(acc)
                    tokens(acc);
                    return acc;
                }
                default: {
                    break tokens;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.TOKEN);
    }
    
    private TokenDecl token_() {
        token_:
        while (true) {
            switch (peek().getKind()) {
                case ACTION: {
                    // val = ACTION
                    Extent val = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                    // id = IDENT
                    String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                     if (isLowerId(id)) throw parsingError("Token name should be all uppercase: " + id); 
                     return new TokenDecl(withLoc(id), val); 
                }
                case IDENT: {
                    // id = IDENT
                    String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                     if (isLowerId(id)) throw parsingError("Token name should be all uppercase: " + id); 
                     return new TokenDecl(withLoc(id), null); 
                }
                default: {
                    break token_;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IDENT);
    }
    
    private List<GrammarRule> rules(@Nullable List<GrammarRule> rules) {
        rules:
        while (true) {
            switch (peek().getKind()) {
                case ACTION: {
                    return Lists.empty();
                }
                case PRIVATE:
                case PUBLIC: {
                    // r = rule_
                    GrammarRule r = rule_();
                     List<GrammarRule> acc = rules == null ? new ArrayList<>() : rules; 
                     acc.add(r); 
                    // rules(acc)
                    rules(acc);
                    return acc;
                }
                default: {
                    break rules;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
    }
    
    private GrammarRule rule_() {
        // vis = visibility
        boolean vis = visibility();
        // rtype = ACTION
        Extent rtype = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // RULE
        eat(Token.Kind.RULE);
        // name = IDENT
        String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
         if (!Character.isLowerCase(name.charAt(0))) 
            throw parsingError("Rule name must start with a lower case letter: " + name); 
         Located<String> lname = withLoc(validJavaIdent(name)); 
        // args = args
        @Nullable Extent args = args();
        // EQUAL
        eat(Token.Kind.EQUAL);
         GrammarRule.Builder builder = new GrammarRule.Builder(vis, rtype, lname, args); 
        // prod = production
        Production prod = production();
         builder.addProduction(prod); 
        // productions(builder)
        productions(builder);
         return builder.build(); 
    }
    
    private boolean visibility() {
        visibility:
        while (true) {
            switch (peek().getKind()) {
                case PRIVATE: {
                    // PRIVATE
                    eat(Token.Kind.PRIVATE);
                    return false;
                }
                case PUBLIC: {
                    // PUBLIC
                    eat(Token.Kind.PUBLIC);
                    return true;
                }
                default: {
                    break visibility;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.PRIVATE, Token.Kind.PUBLIC);
    }
    
    private @Nullable Extent args() {
        args:
        while (true) {
            switch (peek().getKind()) {
                case ACTION:
                case BAR:
                case EQUAL:
                case IDENT:
                case SEMICOL: {
                    return null;
                }
                case ARGUMENTS: {
                    // ext = ARGUMENTS
                    Extent ext = ((Token.ARGUMENTS) eat(Token.Kind.ARGUMENTS)).value;
                    return ext;
                }
                default: {
                    break args;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.EQUAL, Token.Kind.IDENT, Token.Kind.SEMICOL);
    }
    
    private void productions(GrammarRule.Builder builder) {
        productions:
        while (true) {
            switch (peek().getKind()) {
                case BAR: {
                    // prod = production
                    Production prod = production();
                    builder.addProduction(prod);
                    // productions(builder)
                    productions(builder);
                    return;
                }
                case SEMICOL: {
                    // SEMICOL
                    eat(Token.Kind.SEMICOL);
                    return;
                }
                default: {
                    break productions;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.BAR, Token.Kind.SEMICOL);
    }
    
    private Production production() {
        // BAR
        eat(Token.Kind.BAR);
        Production.Builder builder = new Production.Builder();
        // items(builder)
        items(builder);
        return builder.build();
    }
    
    private void items(Production.Builder builder) {
        items:
        while (true) {
            switch (peek().getKind()) {
                case ACTION: {
                    // ext = ACTION
                    Extent ext = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                    builder.addAction(ext);
                    // items(builder)
                    items(builder);
                    return;
                }
                case BAR:
                case SEMICOL: {
                    return;
                }
                case IDENT: {
                    // id = IDENT
                    String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                    // actual = actual(withLoc(id))
                    Production.Actual actual = actual(withLoc(id));
                    builder.addActual(actual);
                    // items(builder)
                    items(builder);
                    return;
                }
                default: {
                    break items;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.BAR, Token.Kind.IDENT, Token.Kind.SEMICOL);
    }
    
    private Production.Actual actual(Located<String> id) {
        actual:
        while (true) {
            switch (peek().getKind()) {
                case ACTION:
                case ARGUMENTS:
                case BAR:
                case IDENT:
                case SEMICOL: {
                    // args = args
                    @Nullable Extent args = args();
                    return actual(null, id, args);
                }
                case EQUAL: {
                     validJavaIdent(id.val); 
                    // EQUAL
                    eat(Token.Kind.EQUAL);
                    // name = IDENT
                    String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                    Located<String> lname = withLoc(name);
                    // args = args
                    @Nullable Extent args = args();
                    return actual(id, lname, args);
                }
                default: {
                    break actual;
                }
            }
        }
        throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.EQUAL, Token.Kind.IDENT, Token.Kind.SEMICOL);
    }
    
       /**
     * Testing this parser
     */
    public static void main(String[] args) throws java.io.IOException {
		String prompt;
       while ((prompt = common.Prompt.getInputLine(">")) != null) {
			try {
				JGELexer lexer = new JGELexer("-",
					new java.io.StringReader(prompt));
				JGEParser parser = new JGEParser(lexer, JGELexer::main);
				System.out.println(parser.start());
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
	}

    
}
