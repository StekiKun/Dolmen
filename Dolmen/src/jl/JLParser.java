package jl;

import static jl.JLToken.DOT;
import static jl.JLToken.END;
import static jl.JLToken.IMPORT;
import static jl.JLToken.STATIC;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.Lists;
import jl.JLToken.Action;
import jl.JLToken.Ident;
import jl.JLToken.Kind;
import syntax.Lexer;
import syntax.Location;

public class JLParser {

	public static class ParsingException extends RuntimeException {
		public ParsingException(String s) {
			super(s);
		}
	}
	private static ParsingException error(JLToken token, Kind...expected) {
		StringBuilder buf = new StringBuilder();
		buf.append("Found token ").append(token);
		buf.append(", expected any of {");
		for (int i = 0; i < expected.length; ++i) {
			if (i != 0) buf.append(',');
			buf.append(expected[i]);
		}
		buf.append('}');
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return new ParsingException(res);
	}
	
	private final Iterator<JLToken> tokens;
	private @Nullable JLToken nextToken;
	
	// private final Stack<List<JLToken>> stack;
	
	public JLParser(Iterator<JLToken> tokens) {
		this.tokens = tokens;
		// this.stack = new Stack<>();
		this.nextToken = null;
	}
	
	public static JLParser of(Supplier<JLToken> tokenizer) {
		return new JLParser(new Iterator<JLToken>() {
			@Nullable JLToken nextToken = tokenizer.get();
			
			@Override
			public boolean hasNext() {
				return nextToken != END;
			}

			@Override
			public @NonNull JLToken next() {
				JLToken tok = nextToken;
				if (tok == null)
					throw new NoSuchElementException();
				if (tok == END) nextToken = null;
				else nextToken = tokenizer.get();
				return tok;
			}
		});
	}

	private JLToken peek() {
		if (nextToken != null) return nextToken;
		if (!tokens.hasNext())
			throw new ParsingException("No more tokens!");
		nextToken = tokens.next();
		return nextToken;
	}
	
	private void eat() {
		peek(); nextToken = null;
	}
	
	private JLToken eat(JLToken.Kind kind) {
		JLToken ctoken = peek();
		if (kind != ctoken.getKind())
			throw error(ctoken, kind);
		nextToken = null;
		return ctoken;
	}
	
	public Lexer parseLexer() {
		List<String> imports = parseImports();
		Action header = (Action) (eat(Kind.ACTION));
		return new Lexer(imports, header.value, Lists.empty(), Location.DUMMY);
	}
	
	private List<String> parseImports() {
		List<String> imports = new ArrayList<>(2);
		while (peek() == IMPORT)
			imports.add(parseImport());
		if (imports.isEmpty()) return Lists.empty();
		return imports;
	}
	
	private String parseImport() {
		eat(Kind.IMPORT);
		StringBuilder buf = new StringBuilder();
		buf.append("import ");
		if (peek() == STATIC) {
			eat(); buf.append("static ");
		}
		parseImportString(buf);
		@SuppressWarnings("null")
		@NonNull String res = buf.toString();
		return res;
	}
	
	private void parseImportString(StringBuilder buf) {
		JLToken id = eat(Kind.IDENT);
		buf.append(((Ident) id).value);
		typename:
		while (peek() == DOT) {
			eat(); buf.append('.');
			JLToken ctoken = peek();
			switch (ctoken.getKind()) {
			case IDENT:
				buf.append(((Ident) id).value); eat(); break;
			case STAR:
				buf.append('*'); eat(); break typename;
			default:
				throw error(ctoken, Kind.IDENT, Kind.STAR);
			}
		}
		eat(Kind.SEMICOL);
	}
	
	public static void main(String[] args) throws IOException {
		JLLexerGenerated lexer = new JLLexerGenerated("tests/jl/test1.jl",
			new FileReader("tests/jl/test1.jl"));
//		JLToken tok;
//		while (true) {
//			tok = lexer.main();
//			System.out.println(tok);
//			if (tok == END) break;
//		}
		
		JLParser parser = of(new Supplier<JLToken>() {
			@SuppressWarnings("null")
			@Override
			public JLToken get() {
				try {
					return lexer.main();
				} catch (IOException e) {
					e.printStackTrace();
					return END;
				}
			}
		});
		Lexer lexerDef = parser.parseLexer();
		System.out.println(lexerDef.toString());
	}
}