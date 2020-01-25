package org.stekikun.dolmen.jle;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import org.stekikun.dolmen.common.CSet;
import org.stekikun.dolmen.common.Lists;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.common.Java;
import org.stekikun.dolmen.codegen.LexBuffer.Position;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.syntax.Located;
import org.stekikun.dolmen.syntax.Regular;
import org.stekikun.dolmen.syntax.Regular.Characters;
import org.stekikun.dolmen.syntax.Regulars;
import org.stekikun.dolmen.syntax.Lexer;
import org.stekikun.dolmen.syntax.Option;

/**
 * Parser generated by Dolmen 1.0.0
 */
public final class JLEParser extends org.stekikun.dolmen.codegen.BaseParser<JLEParser.Token> {
    
    @SuppressWarnings("javadoc")
    public static abstract class Token {
        
        public enum Kind {
            IDENT,
            LSTRING,
            MLSTRING,
            LCHAR,
            INTEGER,
            ACTION,
            RULE,
            SHORTEST,
            EOF,
            AS,
            ORELSE,
            IMPORT,
            STATIC,
            PUBLIC,
            PRIVATE,
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
            END;
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
        
        public final static class LSTRING extends Token {
            public final String value;
            
            private LSTRING(String value) {
                super(Kind.LSTRING);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "LSTRING(" + value + ")";
            }
        }
        public static LSTRING LSTRING(String value) {
            return new LSTRING(value);
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
        
        public final static class LCHAR extends Token {
            public final char value;
            
            private LCHAR(char value) {
                super(Kind.LCHAR);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "LCHAR(" + value + ")";
            }
        }
        public static LCHAR LCHAR(char value) {
            return new LCHAR(value);
        }
        
        public final static class INTEGER extends Token {
            public final int value;
            
            private INTEGER(int value) {
                super(Kind.INTEGER);
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "INTEGER(" + value + ")";
            }
        }
        public static INTEGER INTEGER(int value) {
            return new INTEGER(value);
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
        
        private static final class Singleton extends Token {
            private Singleton(Kind kind) { super(kind); }
            
            @Override
            public String toString() {
                return getKind().toString();
            }
        }
        
        public static final Token RULE = new Singleton(Kind.RULE);
        public static final Token SHORTEST = new Singleton(Kind.SHORTEST);
        public static final Token EOF = new Singleton(Kind.EOF);
        public static final Token AS = new Singleton(Kind.AS);
        public static final Token ORELSE = new Singleton(Kind.ORELSE);
        public static final Token IMPORT = new Singleton(Kind.IMPORT);
        public static final Token STATIC = new Singleton(Kind.STATIC);
        public static final Token PUBLIC = new Singleton(Kind.PUBLIC);
        public static final Token PRIVATE = new Singleton(Kind.PRIVATE);
        public static final Token UNDERSCORE = new Singleton(Kind.UNDERSCORE);
        public static final Token EQUAL = new Singleton(Kind.EQUAL);
        public static final Token OR = new Singleton(Kind.OR);
        public static final Token LBRACKET = new Singleton(Kind.LBRACKET);
        public static final Token RBRACKET = new Singleton(Kind.RBRACKET);
        public static final Token STAR = new Singleton(Kind.STAR);
        public static final Token MAYBE = new Singleton(Kind.MAYBE);
        public static final Token PLUS = new Singleton(Kind.PLUS);
        public static final Token LPAREN = new Singleton(Kind.LPAREN);
        public static final Token RPAREN = new Singleton(Kind.RPAREN);
        public static final Token CARET = new Singleton(Kind.CARET);
        public static final Token DASH = new Singleton(Kind.DASH);
        public static final Token HASH = new Singleton(Kind.HASH);
        public static final Token DOT = new Singleton(Kind.DOT);
        public static final Token LANGLE = new Singleton(Kind.LANGLE);
        public static final Token RANGLE = new Singleton(Kind.RANGLE);
        public static final Token COMMA = new Singleton(Kind.COMMA);
        public static final Token SEMICOL = new Singleton(Kind.SEMICOL);
        public static final Token END = new Singleton(Kind.END);
    }
    
    
    
	private final Map<Located<String>, Regular> definitions = new LinkedHashMap<>();

	/**
	 * @param reg
	 * @return the character set that this regexp corresponds to
	 * 	and throws a parsing error if {@code reg} does not correspond
	 *  to a character set
	 */
	private CSet asCSet(Regular reg) {
		switch (reg.getKind()) {
		case EPSILON:
		case EOF:
		case ALTERNATE:
		case SEQUENCE:
		case REPETITION:
		case BINDING:
			throw parsingError("Regular expression " + reg + " is not a character set.");
		case CHARACTERS: {
			final Characters characters = (Characters) reg;
			return characters.chars;
		}
		}
		throw new IllegalStateException();
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
		Position start = _jl_lexbuf.getLexemeStart();
		int length = _jl_lexbuf.getLexemeEnd().offset - start.offset;
    	return new ParsingException(start, length, msg);
    }

    /**
     * Builds a new parser based on the given lexical buffer
     * and tokenizer
     * @param lexbuf
     * @param tokens
     */
    public <T extends org.stekikun.dolmen.codegen.LexBuffer> 
        JLEParser(T lexbuf, java.util.function.Function<T, Token> tokens) {
        super(lexbuf, tokens);
    }
    
    private Token eat(Token.Kind kind) {
        Token ctoken = eat();
        if (kind != ctoken.getKind())
            throw tokenError(ctoken, kind);
        return ctoken;
    }
    
    /**
     * Entry point for the non-terminal lexer
     */
    public  Lexer  lexer() {
        // options = options(null)
         List<Option>  options = options(null);
        // imports = imports(null)
         List<Located<String>>  imports = imports(null);
        // header = actionOrErr("header")
         Extent  header = actionOrErr("header");
        // definitions()
        definitions();
        // entries = entries()
         List<Lexer.Entry>  entries = entries();
        // footer = actionOrErr("footer")
         Extent  footer = actionOrErr("footer");
        // END
        eat(Token.Kind.END);
        
		Lexer.Builder builder =
			new Lexer.Builder(options, imports, header, definitions, footer);
		for (Lexer.Entry entry : entries)
			builder.addEntry(entry);
		return builder.build();
	
    }
    
    private  List<Option>  options(@Nullable List<Option> opts) {
        switch (peek().getKind()) {
            case ACTION:
            case IDENT:
            case IMPORT:
            case PRIVATE:
            case PUBLIC: {
                 return opts == null ? Lists.empty() : opts; 
            }
            case LBRACKET: {
                // LBRACKET
                eat(Token.Kind.LBRACKET);
                 List<Option> acc = opts == null ? new ArrayList<>() : opts; 
                // key = IDENT
                String key = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 Located<String> lkey = withLoc(key); 
                // EQUAL
                eat(Token.Kind.EQUAL);
                // value = string(true)
                 String  value = string(true);
                 Located<String> lvalue = withLoc(value); 
                // RBRACKET
                eat(Token.Kind.RBRACKET);
                 acc.add(Option.of(lkey, lvalue)); 
                // options(acc)
                options(acc);
                 return acc; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IDENT, Token.Kind.IMPORT, Token.Kind.LBRACKET, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private  String  string(boolean multi) {
        switch (peek().getKind()) {
            case LSTRING: {
                // value = LSTRING
                String value = ((Token.LSTRING) eat(Token.Kind.LSTRING)).value;
                 return value; 
            }
            case MLSTRING: {
                // value = MLSTRING
                String value = ((Token.MLSTRING) eat(Token.Kind.MLSTRING)).value;
                 if (multi) return value;
	  throw parsingError("Illegal multi-line string literal");
	
            }
            default: {
                throw tokenError(peek(), Token.Kind.LSTRING, Token.Kind.MLSTRING);
            }
        }
    }
    
    private  List<Located<String>>  imports(@Nullable List<Located<String>> imports) {
        switch (peek().getKind()) {
            case ACTION:
            case IDENT:
            case PRIVATE:
            case PUBLIC: {
                 return imports == null ? Lists.empty() : imports; 
            }
            case IMPORT: {
                // IMPORT
                eat(Token.Kind.IMPORT);
                
		List<Located<String>> acc = imports == null ? new ArrayList<>() : imports; 
		Position posStart = _jl_lastTokenStart;
	  	StringBuilder buf = new StringBuilder();
	  	buf.append("import ");
	
                // import_(buf)
                import_(buf);
                
		Located<String> imp = Located.of(buf.toString(), posStart, _jl_lastTokenEnd);
		acc.add(imp);
	
                // imports(acc)
                imports(acc);
                 return acc; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.IDENT, Token.Kind.IMPORT, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private  void  import_(StringBuilder buf) {
        switch (peek().getKind()) {
            case IDENT: {
                // typename(buf)
                typename(buf);
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                 buf.append(";"); return; 
            }
            case STATIC: {
                // STATIC
                eat(Token.Kind.STATIC);
                 buf.append("static "); 
                // typename(buf)
                typename(buf);
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                 buf.append(";"); return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.STATIC);
            }
        }
    }
    
    private  void  typename(StringBuilder buf) {
        // id = IDENT
        String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
         buf.append(id); 
        // maybe_more_typename(buf)
        maybe_more_typename(buf);
         return; 
    }
    
    private  void  maybe_more_typename(StringBuilder buf) {
        switch (peek().getKind()) {
            case DOT: {
                // DOT
                eat(Token.Kind.DOT);
                 buf.append('.'); 
                // more_typename(buf)
                more_typename(buf);
                 return; 
            }
            case SEMICOL: {
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.DOT, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  void  more_typename(StringBuilder buf) {
        switch (peek().getKind()) {
            case IDENT: {
                // typename(buf)
                typename(buf);
                 return; 
            }
            case STAR: {
                // STAR
                eat(Token.Kind.STAR);
                 buf.append('*'); return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.STAR);
            }
        }
    }
    
    private  void  definitions() {
        switch (peek().getKind()) {
            case IDENT: {
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 Located<String> lid = withLoc(id); 
                // EQUAL
                eat(Token.Kind.EQUAL);
                // reg = regular()
                 Regular  reg = regular();
                // SEMICOL
                eat(Token.Kind.SEMICOL);
                 definitions.put(lid, reg); 
                // definitions()
                definitions();
                 return; 
            }
            case PRIVATE:
            case PUBLIC: {
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private  List<Lexer.Entry>  entries() {
        // entry = entry()
         Lexer.Entry  entry = entry();
         List<Lexer.Entry> acc = new ArrayList<>();
	  acc.add(entry);
	
        // more_entries(acc)
        more_entries(acc);
         return acc; 
    }
    
    private  void  more_entries(List<Lexer.Entry> acc) {
        switch (peek().getKind()) {
            case ACTION:
            case END: {
                 return; 
            }
            case PRIVATE:
            case PUBLIC: {
                // entry = entry()
                 Lexer.Entry  entry = entry();
                 acc.add(entry); 
                // more_entries(acc)
                more_entries(acc);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.END, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private  Lexer.Entry  entry() {
        // vis = visibility()
         boolean  vis = visibility();
        // returnType = actionOrErr("entry's return type")
         Extent  returnType = actionOrErr("entry's return type");
        // ruleOrErr()
        ruleOrErr();
        // name = IDENT
        String name = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
         Located<String> lname = Located.of(validJavaIdent(name),
		_jl_lexbuf.getLexemeStart(), _jl_lexbuf.getLexemeEnd());
	
        // args = args()
         @Nullable Extent  args = args();
        // EQUAL
        eat(Token.Kind.EQUAL);
        // shortest = shortest()
         boolean  shortest = shortest();
        // clauses = clauses()
         List<Lexer.Clause>  clauses = clauses();
         return new Lexer.Entry(vis, lname, returnType, shortest,
							args, clauses);
	
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
    
    private  @Nullable Extent  args() {
        switch (peek().getKind()) {
            case ACTION: {
                // args = ACTION
                Extent args = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                 return args; 
            }
            case EQUAL: {
                 return null; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.EQUAL);
            }
        }
    }
    
    private  boolean  shortest() {
        switch (peek().getKind()) {
            case OR: {
                 return false; 
            }
            case SHORTEST: {
                // SHORTEST
                eat(Token.Kind.SHORTEST);
                 return true; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.OR, Token.Kind.SHORTEST);
            }
        }
    }
    
    private  List<Lexer.Clause>  clauses() {
         List<Lexer.Clause> clauses = new ArrayList<>(); 
        // clause(clauses)
        clause(clauses);
        // more_clauses(clauses)
        more_clauses(clauses);
         return clauses; 
    }
    
    private  void  more_clauses(List<Lexer.Clause> acc) {
        switch (peek().getKind()) {
            case ACTION:
            case END:
            case PRIVATE:
            case PUBLIC: {
                 return; 
            }
            case OR: {
                // clause(acc)
                clause(acc);
                // more_clauses(acc)
                more_clauses(acc);
                 return; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.END, Token.Kind.OR, Token.Kind.PRIVATE, Token.Kind.PUBLIC);
            }
        }
    }
    
    private  void  clause(List<Lexer.Clause> acc) {
        // OR
        eat(Token.Kind.OR);
        // lreg = regular_orelse(acc)
         Located<Regular>  lreg = regular_orelse(acc);
        // action = ACTION
        Extent action = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
        
		acc.add(new Lexer.Clause(lreg, action));
	
    }
    
    private  Located<Regular>  regular_orelse(List<Lexer.Clause> acc) {
        switch (peek().getKind()) {
            case EOF:
            case IDENT:
            case LBRACKET:
            case LCHAR:
            case LPAREN:
            case LSTRING:
            case MLSTRING:
            case UNDERSCORE: {
                 Position start = _jl_lastTokenEnd; 
                // reg = regular()
                 Regular  reg = regular();
                 return Located.of(reg, start, _jl_lastTokenEnd); 
            }
            case ORELSE: {
                // ORELSE
                eat(Token.Kind.ORELSE);
                
		// Deal with the special default clause by finding
		// all possible first characters matched by other clauses
		CSet possible = CSet.EMPTY;
		for (Lexer.Clause cl : acc)
			possible = CSet.union(possible, Regulars.first(cl.regular.val));
		CSet others = CSet.complement(possible);
		Regular reg = Regular.plus(Regular.chars(others));	
		return withLoc(reg);	// location of 'orelse'
	
            }
            default: {
                throw tokenError(peek(), Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.LBRACKET, Token.Kind.LCHAR, Token.Kind.LPAREN, Token.Kind.LSTRING, Token.Kind.MLSTRING, Token.Kind.ORELSE, Token.Kind.UNDERSCORE);
            }
        }
    }
    
    private  Regular  regular() {
        // r = altRegular()
         Regular  r = altRegular();
        // reg = regular_op(r)
         Regular  reg = regular_op(r);
         return reg; 
    }
    
    private  Regular  regular_op(Regular r) {
        switch (peek().getKind()) {
            case ACTION:
            case RPAREN:
            case SEMICOL: {
                 return r; 
            }
            case AS: {
                // AS
                eat(Token.Kind.AS);
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 Located<String> lid = withLoc(validJavaIdent(id)); 
                // reg = regular_op(Regular.binding(r, lid))
                 Regular  reg = regular_op(Regular.binding(r, lid));
                 return reg; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.AS, Token.Kind.RPAREN, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  Regular  altRegular() {
        // r = seqRegular()
         Regular  r = seqRegular();
        // reg = altRegular_op(r)
         Regular  reg = altRegular_op(r);
         return reg; 
    }
    
    private  Regular  altRegular_op(Regular r1) {
        switch (peek().getKind()) {
            case ACTION:
            case AS:
            case RPAREN:
            case SEMICOL: {
                 return r1; 
            }
            case OR: {
                // OR
                eat(Token.Kind.OR);
                // r2 = altRegular()
                 Regular  r2 = altRegular();
                 return Regular.or(r1, r2); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.AS, Token.Kind.OR, Token.Kind.RPAREN, Token.Kind.SEMICOL);
            }
        }
    }
    
    private  Regular  seqRegular() {
        // r = postfixRegular()
         Regular  r = postfixRegular();
        // reg = seqRegular_op(r)
         Regular  reg = seqRegular_op(r);
         return reg; 
    }
    
    private  Regular  seqRegular_op(Regular r1) {
        switch (peek().getKind()) {
            case ACTION:
            case AS:
            case OR:
            case RPAREN:
            case SEMICOL: {
                 return r1; 
            }
            case EOF:
            case IDENT:
            case LBRACKET:
            case LCHAR:
            case LPAREN:
            case LSTRING:
            case MLSTRING:
            case UNDERSCORE: {
                // r2 = seqRegular()
                 Regular  r2 = seqRegular();
                 return Regular.seq(r1, r2); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.AS, Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.LBRACKET, Token.Kind.LCHAR, Token.Kind.LPAREN, Token.Kind.LSTRING, Token.Kind.MLSTRING, Token.Kind.OR, Token.Kind.RPAREN, Token.Kind.SEMICOL, Token.Kind.UNDERSCORE);
            }
        }
    }
    
    private  Regular  postfixRegular() {
        // r = diffRegular()
         Regular  r = diffRegular();
        // reg = postfixRegular_op(r)
         Regular  reg = postfixRegular_op(r);
         return reg; 
    }
    
    private  Regular  postfixRegular_op(Regular r) {
        switch (peek().getKind()) {
            case ACTION:
            case AS:
            case EOF:
            case IDENT:
            case LBRACKET:
            case LCHAR:
            case LPAREN:
            case LSTRING:
            case MLSTRING:
            case OR:
            case RPAREN:
            case SEMICOL:
            case UNDERSCORE: {
                 return r; 
            }
            case LANGLE: {
                // LANGLE
                eat(Token.Kind.LANGLE);
                // min = INTEGER
                int min = ((Token.INTEGER) eat(Token.Kind.INTEGER)).value;
                 if (min < 0)
				throw parsingError("Invalid repetition count " + min);
			
                // reg = repetitions(r, min)
                 Regular  reg = repetitions(r, min);
                 return reg; 
            }
            case MAYBE: {
                // MAYBE
                eat(Token.Kind.MAYBE);
                 return Regular.or(Regular.EPSILON, r); 
            }
            case PLUS: {
                // PLUS
                eat(Token.Kind.PLUS);
                 return Regular.plus(r); 
            }
            case STAR: {
                // STAR
                eat(Token.Kind.STAR);
                 return Regular.star(r); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.AS, Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.LANGLE, Token.Kind.LBRACKET, Token.Kind.LCHAR, Token.Kind.LPAREN, Token.Kind.LSTRING, Token.Kind.MAYBE, Token.Kind.MLSTRING, Token.Kind.OR, Token.Kind.PLUS, Token.Kind.RPAREN, Token.Kind.SEMICOL, Token.Kind.STAR, Token.Kind.UNDERSCORE);
            }
        }
    }
    
    private  Regular  repetitions(Regular r, int min) {
        switch (peek().getKind()) {
            case COMMA: {
                // COMMA
                eat(Token.Kind.COMMA);
                // max = INTEGER
                int max = ((Token.INTEGER) eat(Token.Kind.INTEGER)).value;
                 if (max < 0)
		throw parsingError("Invalid repetition count " + max);
	  if (max < min)
	  	throw parsingError("Maximum repetition " + max 
	  		+ " is strictly smaller than minimum " + min);
	
                // RANGLE
                eat(Token.Kind.RANGLE);
                 return Regular.repeat(r, min, max); 
            }
            case RANGLE: {
                // RANGLE
                eat(Token.Kind.RANGLE);
                 return Regular.repeat(r, min); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.COMMA, Token.Kind.RANGLE);
            }
        }
    }
    
    private  Regular  diffRegular() {
        // r1 = atomicRegular()
         Regular  r1 = atomicRegular();
        // reg = diffRegular_HASH(r1)
         Regular  reg = diffRegular_HASH(r1);
         return reg; 
    }
    
    private  Regular  diffRegular_HASH(Regular r1) {
        switch (peek().getKind()) {
            case ACTION:
            case AS:
            case EOF:
            case IDENT:
            case LANGLE:
            case LBRACKET:
            case LCHAR:
            case LPAREN:
            case LSTRING:
            case MAYBE:
            case MLSTRING:
            case OR:
            case PLUS:
            case RPAREN:
            case SEMICOL:
            case STAR:
            case UNDERSCORE: {
                 return r1; 
            }
            case HASH: {
                // HASH
                eat(Token.Kind.HASH);
                // r2 = atomicRegular()
                 Regular  r2 = atomicRegular();
                 return Regular.chars(CSet.diff(asCSet(r1), asCSet(r2))); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.AS, Token.Kind.EOF, Token.Kind.HASH, Token.Kind.IDENT, Token.Kind.LANGLE, Token.Kind.LBRACKET, Token.Kind.LCHAR, Token.Kind.LPAREN, Token.Kind.LSTRING, Token.Kind.MAYBE, Token.Kind.MLSTRING, Token.Kind.OR, Token.Kind.PLUS, Token.Kind.RPAREN, Token.Kind.SEMICOL, Token.Kind.STAR, Token.Kind.UNDERSCORE);
            }
        }
    }
    
    private  Regular  atomicRegular() {
        switch (peek().getKind()) {
            case EOF: {
                // EOF
                eat(Token.Kind.EOF);
                 return Regular.chars(CSet.EOF); 
            }
            case IDENT: {
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 @Nullable Regular reg = Maps.get(definitions, Located.dummy(id));
				  if (reg == null)
				  	throw parsingError("Undefined regular expression " + id);
				  return reg;
				
            }
            case LBRACKET: {
                // cs = charClass()
                 CSet  cs = charClass();
                 return Regular.chars(cs); 
            }
            case LCHAR: {
                // ch = LCHAR
                char ch = ((Token.LCHAR) eat(Token.Kind.LCHAR)).value;
                 return Regular.chars(CSet.singleton(ch)); 
            }
            case LPAREN: {
                // LPAREN
                eat(Token.Kind.LPAREN);
                // reg = regular()
                 Regular  reg = regular();
                // RPAREN
                eat(Token.Kind.RPAREN);
                 return reg; 
            }
            case LSTRING:
            case MLSTRING: {
                // s = string(false)
                 String  s = string(false);
                 return Regular.string(s); 
            }
            case UNDERSCORE: {
                // UNDERSCORE
                eat(Token.Kind.UNDERSCORE);
                 return Regular.chars(CSet.ALL_BUT_EOF); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.EOF, Token.Kind.IDENT, Token.Kind.LBRACKET, Token.Kind.LCHAR, Token.Kind.LPAREN, Token.Kind.LSTRING, Token.Kind.MLSTRING, Token.Kind.UNDERSCORE);
            }
        }
    }
    
    private  CSet  charClass() {
        // LBRACKET
        eat(Token.Kind.LBRACKET);
        // c = charSet()
         CSet  c = charSet();
        // RBRACKET
        eat(Token.Kind.RBRACKET);
         return c; 
    }
    
    private  CSet  charSet() {
        switch (peek().getKind()) {
            case CARET: {
                // CARET
                eat(Token.Kind.CARET);
                // c = charSetPositive()
                 CSet  c = charSetPositive();
                 return CSet.complement(c); 
            }
            case IDENT:
            case LCHAR: {
                // c = charSetPositive()
                 CSet  c = charSetPositive();
                 return c; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.CARET, Token.Kind.IDENT, Token.Kind.LCHAR);
            }
        }
    }
    
    private  CSet  charSetPositive() {
        switch (peek().getKind()) {
            case IDENT: {
                // id = IDENT
                String id = ((Token.IDENT) eat(Token.Kind.IDENT)).value;
                 @Nullable Regular r = Maps.get(definitions, Located.dummy(id));
	  if (r == null) 
	  	throw parsingError("Undefined regular expression " + id);
	
                // res = more_charSetPositive(asCSet(r))
                 CSet  res = more_charSetPositive(asCSet(r));
                 return res; 
            }
            case LCHAR: {
                // ch = LCHAR
                char ch = ((Token.LCHAR) eat(Token.Kind.LCHAR)).value;
                // cs = charSetInterval(ch)
                 CSet  cs = charSetInterval(ch);
                // res = more_charSetPositive(cs)
                 CSet  res = more_charSetPositive(cs);
                 return res; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.LCHAR);
            }
        }
    }
    
    private  CSet  charSetInterval(char first) {
        switch (peek().getKind()) {
            case DASH: {
                // DASH
                eat(Token.Kind.DASH);
                // last = LCHAR
                char last = ((Token.LCHAR) eat(Token.Kind.LCHAR)).value;
                 return CSet.interval(first, last); 
            }
            case IDENT:
            case LCHAR:
            case RBRACKET: {
                 return CSet.singleton(first); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.DASH, Token.Kind.IDENT, Token.Kind.LCHAR, Token.Kind.RBRACKET);
            }
        }
    }
    
    private  CSet  more_charSetPositive(CSet acc) {
        switch (peek().getKind()) {
            case IDENT:
            case LCHAR: {
                // cs = charSetPositive()
                 CSet  cs = charSetPositive();
                 return CSet.union(acc, cs); 
            }
            case RBRACKET: {
                 return acc; 
            }
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.LCHAR, Token.Kind.RBRACKET);
            }
        }
    }
    
    private  Extent  actionOrErr(String msg) {
        switch (peek().getKind()) {
            case ACTION: {
                // a = ACTION
                Extent a = ((Token.ACTION) eat(Token.Kind.ACTION)).value;
                 return a; 
            }
            case END:
            case IDENT:
            case PRIVATE:
            case PUBLIC:
            case RULE: {
                 throw expectedError("Expected Java action here. Did you forget the " + msg + "?"); 
            }
            default: {
                throw tokenError(peek(), Token.Kind.ACTION, Token.Kind.END, Token.Kind.IDENT, Token.Kind.PRIVATE, Token.Kind.PUBLIC, Token.Kind.RULE);
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
            default: {
                throw tokenError(peek(), Token.Kind.IDENT, Token.Kind.RULE);
            }
        }
    }
    
     
    
}
