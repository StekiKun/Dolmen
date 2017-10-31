package codegen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import syntax.Grammar.TokenDecl;
import syntax.Extent;

/**
 * This class generates a Java class to represent
 * the various tokens declared in a grammar description.
 * <p>
 * The generated Java class is the base class for all
 * token instances. It contains a static inner enum
 * class {@code Kind} listing all the various token
 * kinds (the enum constants follow the token names).
 * <p>
 * Each token without value is represented by
 * a <i>singleton</i> member of the abstract type,
 * whereas each valued token is represented by a
 * specific subclass, with a {@code value} field
 * and a static factory to be used in the lexer's
 * semantic actions.
 * 
 * @see #output(Writer, String, int, List)
 * 
 * @author Stéphane Lescuyer
 */
public final class TokensOutput {

	private final String className;
	private final boolean nested;
	private final List<@NonNull TokenDecl> tokenDecls;
	private final CodeBuilder buf;
	
	protected TokensOutput(String className,
			List<TokenDecl> tokenDecls, CodeBuilder buf) {
		this.className = className;
		this.nested = buf.getCurrentLevel() > 0;
		this.tokenDecls = tokenDecls;
		this.buf = buf;
	}
	
	private TokensOutput(String className,
			List<TokenDecl> tokenDecls, int level) {
		this.className = className;
		this.nested = level > 0;
		this.tokenDecls = tokenDecls;
		this.buf = new CodeBuilder(level);
	}

	private void genTokenKind() {
		buf.emit("public enum Kind").openBlock();
		boolean first = true;
		for (TokenDecl decl : tokenDecls) {
			if (first) first = false;
			else buf.emitln(",");
			buf.emit(decl.name.val);
		}
		buf.emit(";");
		buf.closeBlock();
		buf.newline();
	}
	
	private void genMethods() {
		buf.emit("private ").emit(className).emit("() ").openBlock();
		buf.emit("// nothing to do");
		buf.closeBlock();
		buf.newline();
		buf.emitln("@Override");
		buf.emitln("public abstract String toString();");
		buf.newline();
		buf.emitln("public abstract Kind getKind();");
		buf.newline();
	}
	
	private void genValuedToken(TokenDecl decl) {
		Extent loc = decl.valueType;
		final @NonNull String name = decl.name.val;
		if (loc == null) throw new IllegalArgumentException();
		final String valType = loc.find();
		
		buf.emit("public final static class ")
		   .emit(name).emit(" extends ").emit(className).openBlock();
		
		buf.emit("public final ").emit(valType).emitln(" value;");
		buf.newline();
		
		buf.emit("private ").emit(name).emit("(")
		   .emit(valType).emit(" value)").openBlock();
		buf.emit("this.value = value;");
		buf.closeBlock();
		buf.newline();
		
		buf.emitln("@Override");
		buf.emit("public String toString()").openBlock();
		buf.emit("return \"").emit(name).emit("(\" + value + \")\";");
		buf.closeBlock();
		buf.newline();
		
		buf.emitln("@Override");
		buf.emit("public Kind getKind()").openBlock();
		buf.emit("return Kind.").emit(name).emit(";");
		buf.closeBlock0();
		
		buf.closeBlock();

		buf.emit("public static ").emit(name).emit(" ")
		   .emit(name).emit("(").emit(valType).emit(" value)").openBlock();
		buf.emit("return new ").emit(name).emit("(value);");
		buf.closeBlock();
		buf.newline();
	}
	
	private void genValuedTokens() {
		for (TokenDecl decl : tokenDecls) {
			if (!decl.isValued()) continue;
			genValuedToken(decl);
		}
	}
	
	private void genSingleton() {
		buf.emit("private static abstract class Singleton extends ")
		   .emit(className).openBlock();
		buf.emitln("private final Kind kind;");
		buf.emitln("private Singleton(Kind kind) { this.kind = kind; }");
		buf.newline();
		// buf.emitln("@SuppressWarnings(\"null\")");
		buf.emitln("@Override");
		buf.emit("public String toString()").openBlock();
		buf.emit("return kind.toString();").closeBlock();
		buf.newline();
		buf.emitln("@Override");
		buf.emit("public Kind getKind()").openBlock();
		buf.emit("return kind;").closeBlock0();
		buf.closeBlock();
	}
	
	private void genSingletonToken(TokenDecl decl) {
		buf.newline();
		buf.emit("public static final ").emit(className).emit(" " + decl.name.val)
		   .emit(" = new Singleton(Kind.").emit(decl.name.val).emit(") {};");
	}
	
	private void genSingletonTokens() {
		if (tokenDecls.stream().allMatch(TokenDecl::isValued))
			return;
		genSingleton();
		for (TokenDecl decl : tokenDecls) {
			if (decl.isValued()) continue;
			genSingletonToken(decl);
		}
	}
	
	protected void genTokens() {
		if (!nested) {
			buf.emitln("@SuppressWarnings(\"javadoc\")");
			buf.emitln("@org.eclipse.jdt.annotation.NonNullByDefault({})");
		}
		buf.emit("public ").emit(nested ? "static " : "")
		   .emit("abstract class ").emit(className).openBlock();
		buf.newline();
		genTokenKind();
		genMethods();
		genValuedTokens();
		genSingletonTokens();
		buf.closeBlock();
	}
	
	/**
	 * Outputs to {@code writer} the definition of a token class
	 * for the tokens described in {@code tokenDecls}. The token
	 * class name is given by {@code className} and code is emitted
	 * starting at the given indentation {@code level}.
	 * 
	 * @param writer
	 * @param className
	 * @param level
	 * @param tokenDecls
	 * @throws IOException
	 */
	public static void output(Writer writer, String className, 
			int level, List<TokenDecl> tokenDecls) throws IOException {
		TokensOutput out = new TokensOutput(className, tokenDecls, level);
		out.genTokens();
		out.buf.print(writer);
	}
}
