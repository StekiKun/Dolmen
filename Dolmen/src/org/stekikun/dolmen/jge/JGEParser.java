package org.stekikun.dolmen.jge;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;
import java.util.List;
import java.util.ArrayList;
import org.stekikun.dolmen.codegen.LexBuffer;
import org.stekikun.dolmen.common.Lists;
import org.stekikun.dolmen.common.Java;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.syntax.PExtent;
import org.stekikun.dolmen.syntax.Located;
import org.stekikun.dolmen.syntax.PProduction;
import org.stekikun.dolmen.syntax.PProduction.Actual;
import org.stekikun.dolmen.syntax.PProduction.ActualExpr;
import org.stekikun.dolmen.syntax.Option;
import org.stekikun.dolmen.syntax.TokenDecl;
import org.stekikun.dolmen.syntax.PGrammarRule;
import org.stekikun.dolmen.syntax.PGrammar;

/**
 * Parser generated by Dolmen 1.0.0
 */
public final class JGEParser extends org.stekikun.dolmen.codegen.BaseParser<JGEParser.Token> {
    
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
        
        Token(Kind kind)  {
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

    private Actual actual(@Nullable Located<String> binding,
            ActualExpr aexpr, @Nullable PExtent args) {
          if (args != null && aexpr.isTerminal())
		    throw new ParsingException(aexpr.symb.start, aexpr.symb.length(),
                "Terminal " + aexpr.symb.val + " does not expect arguments.");
        return new Actual(binding, aexpr, args);
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
    
    private ParsingException expectedError(String msg) {
    	// Fetch the position of the peeked token
		LexBuffer.Position start = _jl_lexbuf.getLexemeStart();
		int length = _jl_lexbuf.getLexemeEnd().offset - start.offset;
    	return new ParsingException(start, length, msg);
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
    public <T extends org.stekikun.dolmen.codegen.LexBuffer> 
        JGEParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super("1.0.0", lexbuf, tokens);
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
    public PGrammar start() {
        // options = options(null)
        List<Option> options = options(null);
        // imports = imports(null)
        List<Located<String>> imports = imports(null);
        // tdecls = tokens(null)
         List<TokenDecl>  tdecls = tokens(null);
        // header = ACTION
        PExtent header = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        // rules = rules(null)
        List<PGrammarRule> rules = rules(null);
        // footer = actionOrErr("footer")
         PExtent  footer = actionOrErr("footer");
        // EOF
        eat(Token.Kind.EOF);
        PGrammar.Builder builder = new PGrammar.Builder(options, imports, header, footer);
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
            //$CASES-OMITTED$
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
                org.stekikun.dolmen.codegen.LexBuffer.Position start = _jl_lastTokenStart;
                // elt = import_
                String elt = import_();
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                acc.add(Located.of("import " + elt + ";", start, _jl_lastTokenEnd));
                // imports(acc)
                imports(acc);
                return acc;
            }
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IDENT, Token.Kind.TOKEN);
            }
        }
    }
    
    private List<PGrammarRule> rules(@Nullable List<PGrammarRule> rules) {
        switch (peek().getKind()) {
            case ACTION:
            case EOF: {
                return Lists.empty();
            }
            case PRIVATE:
            case PUBLIC: {
                // r = rule_
                PGrammarRule r = rule_();
                 List<PGrammarRule> acc = rules == null ? new ArrayList<>() : rules; 
                 acc.add(r); 
                // rules(acc)
                rules(acc);
                return acc;
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.EOF, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private PGrammarRule rule_() {
        // vis = visibility
         boolean  vis = visibility();
        // rtype = actionOrErr("rule's return type")
         PExtent  rtype = actionOrErr("rule's return type");
        // ruleOrErr
        ruleOrErr();
        // name = IDENT
        String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
         if (!Character.isLowerCase(name.charAt(0))) 
            throw parsingError("Rule name must start with a lower case letter: " + name); 
         Located<String> lname = withLoc(validJavaIdent(name)); 
        // params = formal_params
         List<Located<String>>  params = formal_params();
        // args = args
         @Nullable PExtent  args = args();
        // EQUAL
        eat(Token.Kind.EQUAL);
         PGrammarRule.Builder builder = new PGrammarRule.Builder(vis, rtype, lname, params, args); 
        // prod = production
         PProduction  prod = production();
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
            //$CASES-OMITTED$
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
            //$CASES-OMITTED$
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
                //$CASES-OMITTED$
                default: {
                    throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RANGLE);
                }
            }
        }
    }
    
    private  @Nullable PExtent  args() {
        switch (peek().getKind()) {
            case ACTION:
            case BAR:
            case CONTINUE:
            case EOF:
            case EQUAL:
            case IDENT:
            case PRIVATE:
            case PUBLIC:
            case SEMICOL: {
                 return null; 
            }
            case ARGUMENTS: {
                // ext = ARGUMENTS
                PExtent ext = ((Token.ARGUMENTS) eat(Token.Kind.ARGUMENTS)).value;
                 return ext; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.CONTINUE, Token.Kind.EOF, Token.Kind.EQUAL, Token.Kind.IDENT, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.SEMICOL);
            }
        }
    }
    
    private void productions(PGrammarRule.Builder builder) {
        switch (peek().getKind()) {
            case BAR: {
                // prod = production
                 PProduction  prod = production();
                 builder.addProduction(prod); 
                // productions(builder)
                productions(builder);
                 return; 
            }
            case EOF: {
                // EOF
                eat(Token.Kind.EOF);
                 throw expectedError("Unexpected end of file. Have you forgotten a semicolon?"); 
            }
            case PRIVATE:
            case PUBLIC: {
                // visibility
                visibility();
                 throw expectedError("Unexpected start of rule. Have you forgotten a semicolon?"); 
            }
            case SEMICOL: {
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                 return; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.BAR, Token.Kind.EOF, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  PProduction  production() {
        // BAR
        eat(Token.Kind.BAR);
         PProduction.Builder builder = new PProduction.Builder(); 
        // items(builder)
        items(builder);
         return builder.build(); 
    }
    
    private  void  items(PProduction.Builder builder) {
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
            case EOF:
            case PRIVATE:
            case PUBLIC:
            case SEMICOL: {
                 return ;
            }
            case CONTINUE: {
                // CONTINUE
                eat(Token.Kind.CONTINUE);
                 builder.addItem(new PProduction.Continue(withLoc("continue"))); 
                // forbid_more_items()
                forbid_more_items();
                 return; 
            }
            case IDENT: {
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // actual = actual(withLoc(id))
                 Actual  actual = actual(withLoc(id));
                 builder.addActual(actual); 
                // items(builder)
                items(builder);
                 return; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.BAR, Token.Kind.CONTINUE, Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.SEMICOL);
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
            case EOF:
            case PRIVATE:
            case PUBLIC:
            case SEMICOL: {
                 return; 
            }
            case IDENT: {
                 invalidContinuation(); 
                // IDENT
                eat(Token.Kind.IDENT);
                 return; /* dead code */ 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.BAR, Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  Actual  actual(Located<String> id) {
        switch (peek().getKind()) {
            case ACTION:
            case ARGUMENTS:
            case BAR:
            case CONTINUE:
            case EOF:
            case IDENT:
            case LANGLE:
            case PRIVATE:
            case PUBLIC:
            case SEMICOL: {
                // aexpr = actual_expr(id)
                 ActualExpr  aexpr = actual_expr(id);
                // args = args
                 @Nullable PExtent  args = args();
                 return actual(null, aexpr, args); 
            }
            case EQUAL: {
                 validJavaIdent(id.val); 
                // EQUAL
                eat(Token.Kind.EQUAL);
                // name = IDENT
                String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 Located<String> lname = withLoc(name); 
                // aexpr = actual_expr(lname)
                 ActualExpr  aexpr = actual_expr(lname);
                // args = args
                 @Nullable PExtent  args = args();
                 return actual(id, aexpr, args); 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.CONTINUE, Token.Kind.EOF, Token.Kind.EQUAL, Token.Kind.IDENT, Token.Kind.LANGLE, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  ActualExpr  actual_expr(Located<String> id) {
        switch (peek().getKind()) {
            case ACTION:
            case ARGUMENTS:
            case BAR:
            case COMMA:
            case CONTINUE:
            case EOF:
            case IDENT:
            case PRIVATE:
            case PUBLIC:
            case RANGLE:
            case SEMICOL: {
                 return new ActualExpr(id, Lists.empty()); 
            }
            case LANGLE: {
                // LANGLE
                eat(Token.Kind.LANGLE);
                // sym = IDENT
                String sym = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // param = actual_expr(withLoc(sym))
                 ActualExpr  param = actual_expr(withLoc(sym));
                 List<ActualExpr> params = new ArrayList<>();
	  params.add(param);
	
                // actual_exprs(params)
                actual_exprs(params);
                 return new ActualExpr(id, params); 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.COMMA, Token.Kind.CONTINUE, Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.LANGLE, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.RANGLE, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  void  actual_exprs(List<ActualExpr> params) {
        switch (peek().getKind()) {
            case COMMA: {
                // COMMA
                eat(Token.Kind.COMMA);
                // sym = IDENT
                String sym = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // param = actual_expr(withLoc(sym))
                 ActualExpr  param = actual_expr(withLoc(sym));
                 params.add(param); 
                // actual_exprs(params)
                actual_exprs(params);
                 return; 
            }
            case RANGLE: {
                // RANGLE
                eat(Token.Kind.RANGLE);
                 return; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RANGLE);
            }
        }
    }
    
    private  PExtent  actionOrErr(String msg) {
        switch (peek().getKind()) {
            case ACTION: {
                // a = ACTION
                PExtent a = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                 return a; 
            }
            case EOF:
            case IDENT:
            case RULE: {
                 throw expectedError("Expected Java action here. Did you forget the " + msg + "?"); 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.RULE);
            }
        }
    }
    
    private  void  ruleOrErr() {
        switch (peek().getKind()) {
            case IDENT: {
                 throw expectedError("Expected 'rule' keyword here"); 
            }
            case RULE: {
                // RULE
                eat(Token.Kind.RULE);
                 return; 
            }
            //$CASES-OMITTED$
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.RULE);
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
