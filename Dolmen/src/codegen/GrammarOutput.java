package codegen;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import common.CountingWriter;
import common.Iterables;
import common.Maps;
import syntax.Grammar;
import syntax.Grammar.TokenDecl;
import syntax.GrammarRule;
import syntax.Grammars.PredictionTable;
import syntax.Located;
import syntax.Extent;
import syntax.Production;
import syntax.Production.ActionItem;
import syntax.Production.Actual;

/**
 * This class generates a Java class that implements
 * parsing for a given grammar description.
 * <p>
 * Each public non-terminal in the grammar description 
 * leads to a public entry point in the generated
 * grammar.
 * <p>
 * The generated Java class also contains classes
 * to represent the various tokens declared in the
 * grammar.
 * 
 * @see TokensOutput
 * @see #output(Writer, String, Config, Grammar, PredictionTable)
 * 
 * @author Stéphane Lescuyer
 */
public final class GrammarOutput {

	/**
	 * Configuration for {@link GrammarOutput}
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Config {
		/** 
		 * Whether the parser should keep positions for
		 * non-terminal symbols as well as terminal symbols	
		 */
		public final boolean positions;
		
		/**
		 * Builds a configuration for {@link GrammarOutput}
		 * 
		 * @param positions
		 */
		public Config(boolean positions) {
			this.positions = positions;
		}
	}
	
	/** The default configuration for grammar generation */
	public static final Config DEFAULT_CONFIG = new Config(false);
	
	private final Config config;
	private final Grammar grammar;
	private final PredictionTable predict;
	private final CodeBuilder buf;
	
	/**
	 * Initialize an instance to emit parsing code
	 * for the given grammar and prediction table
	 * 
	 * @param config
	 * @param grammar
	 * @param predict
	 */
	private GrammarOutput(Config config,
			Grammar grammar, PredictionTable predict) {
		this.config = config;
		this.grammar = grammar;
		this.predict = predict;
		this.buf = new CodeBuilder(0);
	}
	
	private static String ruleName(String ruleName) {
		return ruleName;
	}
	
	private void genHeader() {
		if (grammar.header.length() == 0) return;
		buf.newline().emitTracked(grammar.header).newline();
	}
	
	private void genConstructor(String name) {
		buf.emitln("@SuppressWarnings(\"null\")");
		buf.emit("public <T extends codegen.LexBuffer>").emit(name).emit("(")
		   .emit("T lexbuf, ")
		   .emit("java.util.function.Function<T, Token> tokens)")
		   .openBlock();
		buf.emit("super(lexbuf, tokens);")
		   .closeBlock();
		buf.newline();
	}
	
	private void genMethods() {
		buf.emit("private Token eat(Token.Kind kind)").openBlock();
		buf.emitln("Token ctoken = eat();");
		buf.emitln("if (kind != ctoken.getKind())");
		buf.incrIndent().emit("    throw tokenError(ctoken, kind);")
		   .decrIndent().newline();
		buf.emit("return ctoken;");
		buf.closeBlock();
	}

	private void genFooter() {
		if (grammar.footer.length() == 0) return;
		buf.newline().emitTracked(grammar.footer).newline();
	}
	
	private void genActual(Production.Actual actual) {
		final String name = actual.item.val;
		buf.newline();
		buf.emitln("// " + actual.toString());
		@Nullable Located<String> boundLoc = actual.binding;
		@Nullable String bound = boundLoc == null ? null : boundLoc.val;
		// If the item is bound, we need to assign the
		// result of parsing the item to some local variable
		// NB: it is up to the user to avoid capture in
		//	choosing binding names
		// NNB: it is up to the user to not bind the results
		//	of void non-terminals
		if (bound != null) {
			if (actual.isTerminal()) {
				Optional<TokenDecl> declo =
					grammar.tokenDecls.stream()
						   .filter(decl -> decl.name.val.equals(actual.item.val))
						   .findFirst();
				if (!declo.isPresent())
					throw new IllegalStateException("Undeclared terminal " + actual.item.val);
				@Nullable Extent valueType = declo.get().valueType;
				if (valueType == null) {
					System.err.println("Bound terminal " + actual + " has no value."
							+ " Ignoring binding in generated code.");
					bound = null;
				}
				else
					buf.emitTracked(valueType)
					   .emit(" ").emit(bound).emit(" = ");
			}
			else {
				buf.emitTracked(grammar.rule(actual.item.val).returnType)
				   .emit(" ").emit(bound).emit(" = ");
			}
		}
		
		// Bound terminals must be cast to the concrete
		// token class, to retrieve their value field
		if (actual.isTerminal()) {
			if (bound != null)
				buf.emit("((Token.").emit(name).emit(") ");
			buf.emit("eat(Token.Kind.").emit(name).emit(")");
			if (bound != null)
				buf.emit(").value");
			buf.emit(";");
			if (config.positions) {
				buf.newline();
				String arg = bound == null ? "null" : "\"" + bound + "\"";
				buf.emit("shift(" + arg + ");");
			}
		}
		else {
			buf.emit(ruleName(name)).emit("(");
			@Nullable Extent args = actual.args;
			if (args != null)
				buf.emitTracked(args);
			buf.emit(");");
			if (config.positions) {
				buf.newline();
				String arg = bound == null ? "null" : "\"" + bound + "\"";
				buf.emit("leave(" + arg + ");");
			}
		}
	}
	
	private void genProduction(Production prod) {
		// For each item, either call the corresponding
		// non-terminal method, or eat the terminal token.
		// Semantic actions are simply inlined in generated
		// code.
		// NB: it is up to the semantic action to either
		//	return; or return val; depending on whether
		//  the return type is void, and of course not
		//	to return in the middle of a production rule.
		if (config.positions)
			buf.newline()
			   .emit("enter(" + Iterables.size(prod.actuals()) + ");");
		for (Production.Item item : prod.items) {
			switch (item.getKind()) {
			case ACTUAL: {
				final Actual actual = (Actual) item;				
				genActual(actual);
				break;
			}
			case ACTION: {
				final ActionItem actionItem = (ActionItem) item;
				buf.newline().emitTracked(actionItem.extent);
				break;
			}
			}
		}
	}
	
	private void genRule(GrammarRule rule, Map<String, List<Production>> trans) {
		buf.newline();
		buf.emit(rule.visibility ? "public " : "private ")
		   .emitTracked(rule.returnType).emit(" ");
		buf.emit(ruleName(rule.name.val)).emit("(");
		if (rule.args != null) buf.emitTracked(rule.args);
		buf.emit(")").openBlock();
		
		// Now is the time to decide what production we are going to use
		// We start by compacting the transition table for this rule so that
		// tokens which lead to the same production are put together in the
		// same case block.
		// We know the transition table is in stable order, and we must ensure
		// our compacted table is as well.
		final Map<@NonNull Production, @NonNull List<@NonNull String>> prodTable =
			new LinkedHashMap<>();
		trans.forEach((term, prods) -> {
			final Production prod = prods.get(0);
			@Nullable List<String> terms_ = Maps.get(prodTable, prod);
			List<String> terms;
			if (terms_ == null) {
				terms = new ArrayList<>();
				prodTable.put(prod, terms);
			}
			else
				terms = terms_;
			terms.add(term);
		});
		
		// If no rules, well, is it supposed to happen?
		if (prodTable.size() == 0) {
			String msg = "Unproducable rule " + rule.name;
			buf.emit("throw new ParsingException(\"")
			   .emit(msg).emit("\");");
		}
		// When only one production used, no need to switch!
		else if (prodTable.size() == 1) {
			genProduction(prodTable.keySet().iterator().next());
		}
		// When more than one production used, we have to peek and switch
		else {
			buf.emit("switch (peek().getKind())").openBlock();
			for (Map.Entry<Production, List<String>> entry : prodTable.entrySet()) {
				final Production prod = entry.getKey();
				boolean first = true;
				for (String term : entry.getValue()) {
					if (first) first = false;
					else buf.newline();
					buf.emit("case ").emit(term).emit(":");
				}
				buf.emit(" {").incrIndent();
				genProduction(prod);
				buf.closeBlock();
			}
			// Generate a default rule for when no tokens
			// (Beware: if all tokens are accounted for, do not generate
			//  a default clause)
			if (trans.size() < grammar.tokenDecls.size()) {
				buf.emit("default:").openBlock();
				buf.emit("throw tokenError(peek()");
				trans.keySet().forEach(tok -> buf.emit(", Token.Kind." + tok));
				buf.emit(");");
				buf.closeBlock0();
			}
			buf.closeBlock0();
		}
		
		buf.closeBlock();
	}
	
	private void genRules() {
		for (GrammarRule rule : grammar.rules.values())
			genRule(rule, predict.tableFor(rule.name.val));
	}

	protected void genParser(String name) {
		grammar.imports.forEach(imp -> 
		buf.startTrackedRange(imp.start)
			.emit(imp.val).endTrackedRange()
			.newline());
		buf.emitln("/**")
		   .emitln(" * Parser generated by Dolmen ")
		   .emitln(" */");
		buf.emitln("@SuppressWarnings(\"javadoc\")");
		buf.emitln("@org.eclipse.jdt.annotation.NonNullByDefault({})");
		buf.emit("public final class ").emit(name)
		   .emitIf(!config.positions, " extends codegen.BaseParser<")
		   .emitIf(config.positions, " extends codegen.BaseParser.WithPositions<")
		   .emit(name).emit(".Token>").openBlock();
		buf.newline();
		
		// Before anything else, generate the token class
		TokensOutput tokensOutput = new TokensOutput("Token", grammar.tokenDecls, buf);
		tokensOutput.genTokens();
		tokensOutput = null;	// free mem
		buf.newline();
		
		genHeader();
		genConstructor(name);
		genMethods();

		// Generate a nest of recursive parsing methods,
		// one for each terminal
		genRules();
		
		genFooter();
		buf.closeBlock();
	}
	
	/**
	 * Outputs to {@code writer} the definition of a top-down
	 * parser for the given {@code grammar}, provided the prediction table
	 * {@code predict} has no conflicts. The name of the
	 * generated Java class is {@code className} and specific
	 * configuration for the code generation is passed via {@code config}.
	 * <p>
	 * Returns the source mappings computed when emitting
	 * the code. Positions in generated code are computed
	 * assuming that {@code writer} is fresh, unless a
	 * {@link CountingWriter} is passed in which case its
	 * current character count is taken into account.
	 * 
	 * @param writer
	 * @param className
	 * @param config
	 * @param grammar
	 * @param predict
	 * @throws IOException
	 */
	public static SourceMapping output(Writer writer, String className, 
			Config config, Grammar grammar, PredictionTable predict)
			throws IOException {
		if (!predict.isLL1())
			throw new IllegalArgumentException("Cannot generate LL(1) parser for this grammar");
		GrammarOutput out = new GrammarOutput(config, grammar, predict);
		int offset =
				writer instanceof CountingWriter ?
					(int) ((CountingWriter) writer).getCount() :
					0;
			out.buf.withTracker(className + ".java", offset);
		out.genParser(className);
		out.buf.print(writer);
		return out.buf.getSourceMapping();
	}
	
	/**
	 * Same as {@link #output(Writer, String, Config, Grammar, PredictionTable)}
	 * with the default configuration {@link #DEFAULT_CONFIG}.
	 * 
	 * @param writer
	 * @param className
	 * @param grammar
	 * @param predict
	 * @throws IOException
	 */
	public static SourceMapping outputDefault(Writer writer,
		String className, Grammar grammar, PredictionTable predict)
		throws IOException {
		return output(writer, className, DEFAULT_CONFIG, grammar, predict);
	}
}
