package jge;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;
import java.util.List;
import java.util.ArrayList;
import common.Lists;
import common.Java;
import syntax.Extent;
import syntax.PExtent;
import syntax.Located;
import syntax.Production;
import syntax.Option;
import syntax.TokenDecl;
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
            MLSTRING,
            EQUAL,
            LSQUARE,
            RSQUARE,
            LANGLE,
            RANGLE,
            BAR,
            DOT,
            STAR,
            SEMICOL,
            COMMA,
            IMPORT,
            STATIC,
            PUBLIC,
            PRIVATE,
            TOKEN,
            RULE,
            CONTINUE,
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
            public final PExtent value;
            
            private ACTION(PExtent value) {
                super(Kind.ACTION);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ACTION(" + value + ")";
            }
        }
        public static ACTION ACTION(PExtent value) {
            return new ACTION(value);
        }
        
        public final static class ARGUMENTS extends Token {
            public final PExtent value;
            
            private ARGUMENTS(PExtent value) {
                super(Kind.ARGUMENTS);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ARGUMENTS(" + value + ")";
            }
        }
        public static ARGUMENTS ARGUMENTS(PExtent value) {
            return new ARGUMENTS(value);
        }
        
        public final static class MLSTRING extends Token {
            public final String value;
            
            private MLSTRING(String value) {
                super(Kind.MLSTRING);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "MLSTRING(" + value + ")";
            }
        }
        public static MLSTRING MLSTRING(String value) {
            return new MLSTRING(value);
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
        public static final Token LANGLE = new Singleton(Kind.LANGLE);
        public static final Token RANGLE = new Singleton(Kind.RANGLE);
        public static final Token BAR = new Singleton(Kind.BAR);
        public static final Token DOT = new Singleton(Kind.DOT);
        public static final Token STAR = new Singleton(Kind.STAR);
        public static final Token SEMICOL = new Singleton(Kind.SEMICOL);
        public static final Token COMMA = new Singleton(Kind.COMMA);
        public static final Token IMPORT = new Singleton(Kind.IMPORT);
        public static final Token STATIC = new Singleton(Kind.STATIC);
        public static final Token PUBLIC = new Singleton(Kind.PUBLIC);
        public static final Token PRIVATE = new Singleton(Kind.PRIVATE);
        public static final Token TOKEN = new Singleton(Kind.TOKEN);
        public static final Token RULE = new Singleton(Kind.RULE);
        public static final Token CONTINUE = new Singleton(Kind.CONTINUE);
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
    
    private void invalidContinuation() {
    	throw parsingError("Continuation must appear last in a production rule");
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
         List<TokenDecl>  tdecls = tokens(null);
        // header = ACTION
        PExtent header = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // rules = rules(null)
        List<GrammarRule> rules = rules(null);
        // footer = ACTION
        PExtent footer = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // EOF
        eat(Token.Kind.EOF);
        Grammar.Builder builder = new Grammar.Builder(options, imports, header, footer);
        tdecls.forEach(tdecl -> builder.addToken(tdecl));
        rules.forEach(rule -> builder.addRule(rule));
        return builder.build();
    }
    
    private List<Option> options(@Nullable List<Option> opts) {
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
                // value = MLSTRING
                String value = ((Token.MLSTRING) eat(Token.Kind.MLSTRING)).value;
                 Located<String> lvalue = withLoc(value); 
                // RSQUARE
                eat(Token.Kind.RSQUARE);
                 acc.add(Option.of(lkey, lvalue)); 
                // options(acc)
                options(acc);
                 return acc; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IMPORT, Token.Kind.LSQUARE, Token.Kind.TOKEN);
            }
        }
    }
    
    private List<Located<String>> imports(@Nullable List<Located<String>> imp) {
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
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IMPORT, Token.Kind.TOKEN);
            }
        }
    }
    
    private String import_() {
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
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.STATIC);
            }
        }
    }
    
    private String typename() {
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
                throw tokenError(peek(), Token.Kind.DOT, Token.Kind.SEMICOL);
            }
        }
    }
    
    private String typename0() {
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
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.STAR);
            }
        }
    }
    
    private  List<TokenDecl>  tokens(@Nullable List<TokenDecl> tokens) {
        switch (peek().getKind()) {
            case ACTION: {
                 return Lists.empty(); 
            }
            case TOKEN: {
                 List<TokenDecl> acc = tokens == null ? new ArrayList<>() : tokens; 
                // TOKEN
                eat(Token.Kind.TOKEN);
                // token_decls(acc)
                token_decls(acc);
                // tokens(acc)
                tokens(acc);
                 return acc; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.TOKEN);
            }
        }
    }
    
    private  void  token_decls(List<TokenDecl> tokens) {
        // value = token_value()
         @Nullable Extent  value = token_value();
        // id = IDENT
        String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
         if (isLowerId(id)) throw parsingError("Token name should be all uppercase: " + id); 
         tokens.add(new TokenDecl(withLoc(id), value)); 
        // more_token_decls(tokens, value)
        more_token_decls(tokens, value);
         return; 
    }
    
    private  @Nullable Extent  token_value() {
        switch (peek().getKind()) {
            case ACTION: {
                // val = ACTION
                PExtent val = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                 return val; 
            }
            case IDENT: {
                 return null; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IDENT);
            }
        }
    }
    
    private  void  more_token_decls(List<TokenDecl> tokens, @Nullable Extent value) {
        switch (peek().getKind()) {
            case ACTION:
            case TOKEN: {
                 return; 
            }
            case IDENT: {
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 if (isLowerId(id)) throw parsingError("Token name should be all uppercase: " + id); 
                 tokens.add(new TokenDecl(withLoc(id), value)); 
                // more_token_decls(tokens, value)
                more_token_decls(tokens, value);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IDENT, Token.Kind.TOKEN);
            }
        }
    }
    
    private List<GrammarRule> rules(@Nullable List<GrammarRule> rules) {
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
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private GrammarRule rule_() {
        // vis = visibility
         boolean  vis = visibility();
        // rtype = ACTION
        PExtent rtype = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // RULE
        eat(Token.Kind.RULE);
        // name = IDENT
        String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
         if (!Character.isLowerCase(name.charAt(0))) 
            throw parsingError("Rule name must start with a lower case letter: " + name); 
         Located<String> lname = withLoc(validJavaIdent(name)); 
        // params = formal_params
         List<Located<String>>  params = formal_params();
        // args = args
         @Nullable Extent  args = args();
        // EQUAL
        eat(Token.Kind.EQUAL);
         GrammarRule.Builder builder = new GrammarRule.Builder(vis, rtype, lname, args); 
        // prod = production
         Production  prod = production();
         builder.addProduction(prod); 
        // productions(builder)
        productions(builder);
         return builder.build(); 
    }
    
    private  boolean  visibility() {
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
                throw tokenError(peek(), Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private  List<Located<String>>  formal_params() {
        switch (peek().getKind()) {
            case ARGUMENTS:
            case EQUAL: {
                 return Lists.empty(); 
            }
            case LANGLE: {
                // LANGLE
                eat(Token.Kind.LANGLE);
                // name = IDENT
                String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 if (!Character.isLowerCase(name.charAt(0)))
		throw parsingError("Rule parameter must start with a lower case letter: " + name); 
                 Located<String> lname = withLoc(name);
	  List<Located<String>> params = new ArrayList<>();
	  params.add(lname);
	
                // more_formal_params(params)
                more_formal_params(params);
                 return params; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ARGUMENTS, Token.Kind.EQUAL, Token.Kind.LANGLE);
            }
        }
    }
    
    private  void  more_formal_params(List<Located<String>> params) {
        more_formal_params:
        while (true) {
            switch (peek().getKind()) {
                case COMMA: {
                    // COMMA
                    eat(Token.Kind.COMMA);
                    // name = IDENT
                    String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                     if (!Character.isLowerCase(name.charAt(0)))
		throw parsingError("Rule parameter must start with a lower case letter: " + name); 
                     Located<String> lname = withLoc(name);
	  params.add(lname); 
	
                    continue more_formal_params;
                }
                case RANGLE: {
                    // RANGLE
                    eat(Token.Kind.RANGLE);
                     return; 
                }
                default: {
                    throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RANGLE);
                }
            }
        }
    }
    
    private  @Nullable Extent  args() {
        switch (peek().getKind()) {
            case ACTION:
            case BAR:
            case CONTINUE:
            case EQUAL:
            case IDENT:
            case SEMICOL: {
                 return null; 
            }
            case ARGUMENTS: {
                // ext = ARGUMENTS
                PExtent ext = ((Token.ARGUMENTS) eat(Token.Kind.ARGUMENTS)).value;
                 return ext; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.CONTINUE, Token.Kind.EQUAL, Token.Kind.IDENT, Token.Kind.SEMICOL);
            }
        }
    }
    
    private void productions(GrammarRule.Builder builder) {
        switch (peek().getKind()) {
            case BAR: {
                // prod = production
                 Production  prod = production();
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
                throw tokenError(peek(), Token.Kind.BAR, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  Production  production() {
        // BAR
        eat(Token.Kind.BAR);
         Production.Builder builder = new Production.Builder(); 
        // items(builder)
        items(builder);
         return builder.build(); 
    }
    
    private  void  items(Production.Builder builder) {
        switch (peek().getKind()) {
            case ACTION: {
                // ext = ACTION
                PExtent ext = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                 builder.addAction(ext); 
                // items(builder)
                items(builder);
                 return; 
            }
            case BAR:
            case SEMICOL: {
                 return ;
            }
            case CONTINUE: {
                // CONTINUE
                eat(Token.Kind.CONTINUE);
                 builder.addItem(new Production.Continue(withLoc("continue"))); 
                // forbid_more_items()
                forbid_more_items();
                 return; 
            }
            case IDENT: {
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // actual = actual(withLoc(id))
                 Production.Actual  actual = actual(withLoc(id));
                 builder.addActual(actual); 
                // items(builder)
                items(builder);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.BAR, Token.Kind.CONTINUE, Token.Kind.IDENT, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  void  forbid_more_items() {
        switch (peek().getKind()) {
            case ACTION: {
                 invalidContinuation(); 
                // ACTION
                eat(Token.Kind.ACTION);
                 return; /* dead code */ 
            }
            case BAR:
            case SEMICOL: {
                 return; 
            }
            case IDENT: {
                 invalidContinuation(); 
                // IDENT
                eat(Token.Kind.IDENT);
                 return; /* dead code */ 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.BAR, Token.Kind.IDENT, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  Production.Actual  actual(Located<String> id) {
        switch (peek().getKind()) {
            case ACTION:
            case ARGUMENTS:
            case BAR:
            case CONTINUE:
            case IDENT:
            case SEMICOL: {
                // args = args
                 @Nullable Extent  args = args();
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
                 @Nullable Extent  args = args();
                 return actual(id, lname, args); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.CONTINUE, Token.Kind.EQUAL, Token.Kind.IDENT, Token.Kind.SEMICOL);
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
