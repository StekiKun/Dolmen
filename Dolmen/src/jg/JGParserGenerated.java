package jg;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;
import java.util.List;
import java.util.ArrayList;
import common.Lists;
import syntax.Location;
import syntax.Production;
import syntax.Grammar.TokenDecl;
import syntax.GrammarRule;
import syntax.Grammar;
/**
 * Parser generated by Dolmen 
 */
@SuppressWarnings("javadoc")
@org.eclipse.jdt.annotation.NonNullByDefault({})
public final class JGParserGenerated extends codegen.BaseParser<JGParserGenerated.Token> {
    
    public static abstract class Token {
        
        public enum Kind {
            IDENT,
            ACTION,
            ARGUMENTS,
            EQUAL,
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
        
        private Token()  {
            // nothing to do
        }
        
        @Override
        public abstract String toString();
        
        public abstract Kind getKind();
        
        public final static class IDENT extends Token {
            public final @NonNull String value;
            
            private IDENT(@NonNull String value) {
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "IDENT(" + value + ")";
            }
            
            @Override
            public Kind getKind() {
                return Kind.IDENT;
            }
        }
        public static IDENT IDENT(@NonNull String value) {
            return new IDENT(value);
        }
        
        public final static class ACTION extends Token {
            public final @NonNull Location value;
            
            private ACTION(@NonNull Location value) {
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ACTION(" + value + ")";
            }
            
            @Override
            public Kind getKind() {
                return Kind.ACTION;
            }
        }
        public static ACTION ACTION(@NonNull Location value) {
            return new ACTION(value);
        }
        
        public final static class ARGUMENTS extends Token {
            public final @NonNull Location value;
            
            private ARGUMENTS(@NonNull Location value) {
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "ARGUMENTS(" + value + ")";
            }
            
            @Override
            public Kind getKind() {
                return Kind.ARGUMENTS;
            }
        }
        public static ARGUMENTS ARGUMENTS(@NonNull Location value) {
            return new ARGUMENTS(value);
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
        
        public static Token EQUAL = new Singleton(Kind.EQUAL) {};
        public static Token BAR = new Singleton(Kind.BAR) {};
        public static Token DOT = new Singleton(Kind.DOT) {};
        public static Token STAR = new Singleton(Kind.STAR) {};
        public static Token SEMICOL = new Singleton(Kind.SEMICOL) {};
        public static Token IMPORT = new Singleton(Kind.IMPORT) {};
        public static Token STATIC = new Singleton(Kind.STATIC) {};
        public static Token PUBLIC = new Singleton(Kind.PUBLIC) {};
        public static Token PRIVATE = new Singleton(Kind.PRIVATE) {};
        public static Token TOKEN = new Singleton(Kind.TOKEN) {};
        public static Token RULE = new Singleton(Kind.RULE) {};
        public static Token EOF = new Singleton(Kind.EOF) {};
    }
    
    
    /**
     * Returns {@code true} if the given string contains a lower-case letter
     */
     private static boolean isLowerId(String name) {
         return name.chars().anyMatch(ch -> Character.isLowerCase(ch));
     }

    @SuppressWarnings("null")
    public JGParserGenerated(java.util.function.Supplier<Token> tokens) {
        super(tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = peek();
        if (kind != ctoken.getKind())
        throw tokenError(ctoken, kind);
        _jl_nextToken = null;
        return ctoken;
    }
    
    public @NonNull Grammar start() {
        switch (peek().getKind()) {
            case IMPORT: {
                // imports = imports(null)
                @NonNull List<@NonNull String> imports = imports(null);
                // tdecls = tokens(null)
                @NonNull List<@NonNull TokenDecl> tdecls = tokens(null);
                // header = ACTION
                @NonNull Location header = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // rules = rules(null)
                @NonNull List<@NonNull GrammarRule> rules = rules(null);
                // footer = ACTION
                @NonNull Location footer = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // EOF
                eat(Token.Kind.EOF);
                Grammar.Builder builder = new Grammar.Builder(imports, header, footer);
                tdecls.forEach(tdecl -> builder.addToken(tdecl));
                rules.forEach(rule -> builder.addRule(rule));
                return builder.build();
            }
            case ACTION: {
                // imports = imports(null)
                @NonNull List<@NonNull String> imports = imports(null);
                // tdecls = tokens(null)
                @NonNull List<@NonNull TokenDecl> tdecls = tokens(null);
                // header = ACTION
                @NonNull Location header = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // rules = rules(null)
                @NonNull List<@NonNull GrammarRule> rules = rules(null);
                // footer = ACTION
                @NonNull Location footer = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // EOF
                eat(Token.Kind.EOF);
                Grammar.Builder builder = new Grammar.Builder(imports, header, footer);
                tdecls.forEach(tdecl -> builder.addToken(tdecl));
                rules.forEach(rule -> builder.addRule(rule));
                return builder.build();
            }
            case TOKEN: {
                // imports = imports(null)
                @NonNull List<@NonNull String> imports = imports(null);
                // tdecls = tokens(null)
                @NonNull List<@NonNull TokenDecl> tdecls = tokens(null);
                // header = ACTION
                @NonNull Location header = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // rules = rules(null)
                @NonNull List<@NonNull GrammarRule> rules = rules(null);
                // footer = ACTION
                @NonNull Location footer = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // EOF
                eat(Token.Kind.EOF);
                Grammar.Builder builder = new Grammar.Builder(imports, header, footer);
                tdecls.forEach(tdecl -> builder.addToken(tdecl));
                rules.forEach(rule -> builder.addRule(rule));
                return builder.build();
            }
            default: {
                throw tokenError(peek(), Token.Kind.IMPORT, Token.Kind.ACTION, Token.Kind.TOKEN);
            }
        }
    }
    
    private @NonNull List<@NonNull String> imports(@Nullable List<@NonNull String> imp) {
        switch (peek().getKind()) {
            case IMPORT: {
                @NonNull List<@NonNull String> acc = imp == null ? new ArrayList<>() : imp;
                // IMPORT
                eat(Token.Kind.IMPORT);
                // elt = import_
                String elt = import_();
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                acc.add("import " + elt + ";");
                // imports(acc)
                imports(acc);
                return acc;
            }
            case ACTION: {
                return imp == null ? Lists.empty() : imp;
            }
            case TOKEN: {
                return imp == null ? Lists.empty() : imp;
            }
            default: {
                throw tokenError(peek(), Token.Kind.IMPORT, Token.Kind.ACTION, Token.Kind.TOKEN);
            }
        }
    }
    
    private String import_() {
        switch (peek().getKind()) {
            case IDENT: {
                // id = IDENT
                @NonNull String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // tn = typename
                String tn = typename();
                return id + tn;
            }
            case STATIC: {
                // STATIC
                eat(Token.Kind.STATIC);
                // id = IDENT
                @NonNull String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
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
            case SEMICOL: {
                return "";
            }
            case DOT: {
                // DOT
                eat(Token.Kind.DOT);
                // ty = typename0
                String ty = typename0();
                return "." + ty;
            }
            default: {
                throw tokenError(peek(), Token.Kind.SEMICOL, Token.Kind.DOT);
            }
        }
    }
    
    private String typename0() {
        switch (peek().getKind()) {
            case STAR: {
                // STAR
                eat(Token.Kind.STAR);
                return "*";
            }
            case IDENT: {
                // id = IDENT
                @NonNull String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // ty = typename
                String ty = typename();
                return id + ty;
            }
            default: {
                throw tokenError(peek(), Token.Kind.STAR, Token.Kind.IDENT);
            }
        }
    }
    
    private @NonNull List<@NonNull TokenDecl> tokens(@Nullable List<@NonNull TokenDecl> tokens) {
        switch (peek().getKind()) {
            case ACTION: {
                return Lists.empty();
            }
            case TOKEN: {
                @NonNull List<@NonNull TokenDecl> acc = tokens == null ? new ArrayList<>() : tokens;
                // TOKEN
                eat(Token.Kind.TOKEN);
                // tok = token
                @NonNull TokenDecl tok = token();
                acc.add(tok);
                // tokens(acc)
                tokens(acc);
                return acc;
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.TOKEN);
            }
        }
    }
    
    private @NonNull TokenDecl token() {
        switch (peek().getKind()) {
            case IDENT: {
                // id = IDENT
                @NonNull String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                if (isLowerId(id))
                   throw new ParsingException("Token name should be all uppercase: " + id);
                return new TokenDecl(id, null);
            }
            case ACTION: {
                // val = ACTION
                @NonNull Location val = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // id = IDENT
                @NonNull String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                if (isLowerId(id))
                    throw new ParsingException("Token name should be all uppercase: " + id);
                return new TokenDecl(id, val);
            }
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.ACTION);
            }
        }
    }
    
    private @NonNull List<@NonNull GrammarRule> rules(@Nullable List<@NonNull GrammarRule> rules) {
        switch (peek().getKind()) {
            case ACTION: {
                return Lists.empty();
            }
            case PUBLIC: {
                // rule = rule
                @NonNull GrammarRule rule = rule();
                @NonNull List<@NonNull GrammarRule> acc = rules == null ? new ArrayList<>() : rules;
                acc.add(rule);
                // rules(acc)
                rules(acc);
                return acc;
            }
            case PRIVATE: {
                // rule = rule
                @NonNull GrammarRule rule = rule();
                @NonNull List<@NonNull GrammarRule> acc = rules == null ? new ArrayList<>() : rules;
                acc.add(rule);
                // rules(acc)
                rules(acc);
                return acc;
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.PUBLIC, Token.Kind.PRIVATE);
            }
        }
    }
    
    private @NonNull GrammarRule rule() {
        switch (peek().getKind()) {
            case PUBLIC: {
                // vis = visibility
                boolean vis = visibility();
                // rtype = ACTION
                @NonNull Location rtype = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // RULE
                eat(Token.Kind.RULE);
                // name = IDENT
                @NonNull String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                if (!Character.isLowerCase(name.charAt(0)))
                    throw new ParsingException("Rule name must start with a lower case letter: " + name);
                // args = args
                @Nullable Location args = args();
                // EQUAL
                eat(Token.Kind.EQUAL);
                GrammarRule.@NonNull Builder builder =
                	new GrammarRule.Builder(vis, rtype, name, args);
                // prod = production
                @NonNull Production prod = production();
                builder.addProduction(prod);
                // productions(builder)
                productions(builder);
                return builder.build();
            }
            case PRIVATE: {
                // vis = visibility
                boolean vis = visibility();
                // rtype = ACTION
                @NonNull Location rtype = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                // RULE
                eat(Token.Kind.RULE);
                // name = IDENT
                @NonNull String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                if (!Character.isLowerCase(name.charAt(0)))
                    throw new ParsingException("Rule name must start with a lower case letter: " + name);
                // args = args
                @Nullable Location args = args();
                // EQUAL
                eat(Token.Kind.EQUAL);
                GrammarRule.@NonNull Builder builder =
                	new GrammarRule.Builder(vis, rtype, name, args);
                // prod = production
                @NonNull Production prod = production();
                builder.addProduction(prod);
                // productions(builder)
                productions(builder);
                return builder.build();
            }
            default: {
                throw tokenError(peek(), Token.Kind.PUBLIC, Token.Kind.PRIVATE);
            }
        }
    }
    
    private boolean visibility() {
        switch (peek().getKind()) {
            case PUBLIC: {
                // PUBLIC
                eat(Token.Kind.PUBLIC);
                return true;
            }
            case PRIVATE: {
                // PRIVATE
                eat(Token.Kind.PRIVATE);
                return false;
            }
            default: {
                throw tokenError(peek(), Token.Kind.PUBLIC, Token.Kind.PRIVATE);
            }
        }
    }
    
    private @Nullable Location args() {
        switch (peek().getKind()) {
            case SEMICOL: {
                return null;
            }
            case ARGUMENTS: {
                // loc = ARGUMENTS
                @NonNull Location loc = ((Token.ARGUMENTS) eat(Token.Kind.ARGUMENTS)).value;
                return loc;
            }
            case BAR: {
                return null;
            }
            case ACTION: {
                return null;
            }
            case IDENT: {
                return null;
            }
            case EQUAL: {
                return null;
            }
            default: {
                throw tokenError(peek(), Token.Kind.SEMICOL, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.ACTION, Token.Kind.IDENT, Token.Kind.EQUAL);
            }
        }
    }
    
    private void productions(GrammarRule.@NonNull Builder builder) {
        switch (peek().getKind()) {
            case SEMICOL: {
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                return;
            }
            case BAR: {
                // prod = production
                @NonNull Production prod = production();
                builder.addProduction(prod);
                // productions(builder)
                productions(builder);
                return;
            }
            default: {
                throw tokenError(peek(), Token.Kind.SEMICOL, Token.Kind.BAR);
            }
        }
    }
    
    private @NonNull Production production() {
        
        // BAR
        eat(Token.Kind.BAR);
        Production.Builder builder = new Production.Builder();
        // items(builder)
        items(builder);
        return builder.build();
    }
    
    private void items(Production.Builder builder) {
        switch (peek().getKind()) {
            case SEMICOL: {
                return;
            }
            case BAR: {
                return;
            }
            case ACTION: {
                // loc = ACTION
                @NonNull Location loc = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                builder.addAction(loc);
                // items(builder)
                items(builder);
                return;
            }
            case IDENT: {
                // id = IDENT
                @NonNull String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // actual = actual(id)
                Production.@NonNull Actual actual = actual(id);
                builder.addActual(actual);
                // items(builder)
                items(builder);
                return;
            }
            default: {
                throw tokenError(peek(), Token.Kind.SEMICOL, Token.Kind.BAR, Token.Kind.ACTION, Token.Kind.IDENT);
            }
        }
    }
    
    private Production.@NonNull Actual actual(@NonNull String id) {
        switch (peek().getKind()) {
            case SEMICOL: {
                // args = args
                @Nullable Location args = args();
                return new Production.Actual(null, id, args);
            }
            case ARGUMENTS: {
                // args = args
                @Nullable Location args = args();
                return new Production.Actual(null, id, args);
            }
            case BAR: {
                // args = args
                @Nullable Location args = args();
                return new Production.Actual(null, id, args);
            }
            case ACTION: {
                // args = args
                @Nullable Location args = args();
                return new Production.Actual(null, id, args);
            }
            case IDENT: {
                // args = args
                @Nullable Location args = args();
                return new Production.Actual(null, id, args);
            }
            case EQUAL: {
                // EQUAL
                eat(Token.Kind.EQUAL);
                // name = IDENT
                @NonNull String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                // args = args
                @Nullable Location args = args();
                return new Production.Actual(id, name, args);
            }
            default: {
                throw tokenError(peek(), Token.Kind.SEMICOL, Token.Kind.ARGUMENTS, Token.Kind.BAR, Token.Kind.ACTION, Token.Kind.IDENT, Token.Kind.EQUAL);
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
				JGLexer lexer = new JGLexer("-",
					new java.io.StringReader(prompt));
				JGParserGenerated parser = new JGParserGenerated(lexer::main);
				System.out.println(parser.start());
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
	}

    
}
