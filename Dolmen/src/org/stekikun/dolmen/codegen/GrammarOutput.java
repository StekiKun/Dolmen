package org.stekikun.dolmen.codegen;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Constants;
import org.stekikun.dolmen.common.CountingWriter;
import org.stekikun.dolmen.common.Iterables;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.syntax.CExtent;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.syntax.Located;
import org.stekikun.dolmen.syntax.TokenDecl;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.GrammarRule;
import org.stekikun.dolmen.unparam.Grammars;
import org.stekikun.dolmen.unparam.Production;
import org.stekikun.dolmen.unparam.Grammars.PredictionTable;
import org.stekikun.dolmen.unparam.Production.ActionItem;
import org.stekikun.dolmen.unparam.Production.Actual;
import org.stekikun.dolmen.unparam.Production.Continue;

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
 * @see #output(Writer, String, Config, Grammar, Grammars.PredictionTable)
 * 
 * @author Stéphane Lescuyer
 */
public final class GrammarOutput {

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

	private Map<String, String> ruleNameCache = new HashMap<>();

	private String ruleName(String ruleName) {
		// Names which do not encode applications of parametric rules need no escaping
		if (!ruleName.contains("<")) return ruleName;
		// Look in the cache for this particular rule
		@Nullable String cached = ruleNameCache.get(ruleName);
		if (cached != null) return cached;
		// Escape the following special characters introduced in names by the
		// expansion mechanism: '<', '>', ',' and ' '
		StringBuilder buf = new StringBuilder(ruleName.length());
		for (int i = 0; i < ruleName.length(); ++i) {
			char ci = ruleName.charAt(i);
			char newci = ci;
			switch (ci) {
			case '<': newci = 'ˎ'; break;
			case '>': newci = 'ˏ'; break;
			case ',': newci = 'ˌ'; break;
			case ' ': continue;
			}
			buf.append(newci);
		}
		String jRuleName = buf.toString();
		ruleNameCache.put(ruleName, jRuleName);
		return jRuleName;
	}
	
	private void genAnnotations(String annotations) {
		// In case the configuration provides several annotations
		// split around newlines and trim potential leading blanks
		if (annotations.isEmpty()) return;
		String[] lines = annotations.split("\n");
		for (String line : lines) {
			String lline = line.trim();
			if (lline.isEmpty()) continue;
			buf.emitln(line);
		}
	}
	
	private void genHeader() {
		if (grammar.header.length() == 0) return;
		buf.newline().emitTracked(grammar.header).newline();
	}
	
	private void genConstructor(String name) {
		buf.emitln("/**");
	    buf.emitln(" * Builds a new parser based on the given lexical buffer");
	    buf.emitln(" * and tokenizer");
	    buf.emitln(" * @param lexbuf");
	    buf.emitln(" * @param tokens");
	    buf.emitln(" */");
		buf.emit("public <T extends org.stekikun.dolmen.codegen.LexBuffer> ").incrIndent().newline();
		buf.emit(name).emit("(")
		   .emit("T lexbuf, ")
		   .emit("java.util.function.Function<T, Token> tokens)")
		   .decrIndent().openBlock();
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
			@Nullable CExtent args = actual.args;
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
	
	private void genProduction(@Nullable String continuation, Production prod) {
		// For each item, either call the corresponding
		// non-terminal method, or eat the terminal token.
		// Semantic actions are simply inlined in generated
		// code.
		// NB: it is up to the semantic action to either
		//	return; or return val; depending on whether
		//  the return type is void, and of course not
		//	to return in the middle of a production rule.
		//  The exception is the continuation which acts
		//  as an actual + the corresponding return at the
		//  same time.
		if (config.positions && continuation == null)
			buf.emitln("enter(" + Iterables.size(prod.actuals()) + ");");
		boolean first = true;
		for (Production.Item item : prod.items) {
			if (first) first = false;
			else buf.newline();
			switch (item.getKind()) {
			case ACTUAL: {
				final Actual actual = (Actual) item;				
				genActual(actual);
				break;
			}
			case ACTION: {
				final ActionItem actionItem = (ActionItem) item;
				buf.emitTracked(actionItem.extent);
				break;
			}
			case CONTINUE: {
				@SuppressWarnings("unused")
				final Continue cont = (Continue) item;
				buf.emitlnIf(config.positions, "rewind();");
				if (continuation == null)
					throw new IllegalStateException();
				buf.emit("continue ").emit(continuation).emit(";");
				break;
			}
			}
		}
	}
	
	private void genRule(GrammarRule rule, Map<String, List<Production>> trans) {
		buf.newline();
		if (rule.visibility) {
			buf.emitln("/**");
			buf.emitln(" * Entry point for the non-terminal " + rule.name.val);
			buf.emitln(" */");
		}
		final String ruleName = ruleName(rule.name.val);
		final @Nullable String continuation = 
			rule.hasContinuation() ? ruleName : null;
		final boolean continued = continuation != null;
		
		buf.emit(rule.visibility ? "public " : "private ")
		   .emitTracked(rule.returnType).emit(" ");
		buf.emit(ruleName).emit("(");
		if (rule.args != null) buf.emitTracked(rule.args);
		buf.emit(")").openBlock();
		
		// Now is the time to decide what production we are going to use
		// We start by compacting the transition table for this rule so that
		// tokens which lead to the same production are put together in the
		// same case block.
		// We know the transition table is in stable order, and we must ensure
		// our compacted table is as well.
		final Map<org.stekikun.dolmen.unparam.Production, @NonNull List<@NonNull String>> prodTable =
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
			String msg = "Unproducable rule " + rule.name.val;
			buf.emit("throw parsingError(\"")
			   .emit(msg).emit("\");");
		}
		// When only one production used, no need to switch!
		else if (prodTable.size() == 1) {
			// There should be no need for a loop either, as if there is a continuation
			// this rule should never return anyway... Let's do it nonetheless if
			// users write some fancy unexpected logic in semantic actions.
			Production prod = prodTable.keySet().iterator().next();
			if (continued) {
				// Cf. comment below in general case for why the position buffer
				// is allocated here
				buf.emitlnIf(config.positions, "enter(" + Iterables.size(prod.actuals()) + ");");
				buf.emitln(ruleName + ":");
				buf.emit("while (true)").openBlock();
			}
			genProduction(continuation, prod);
			if (continued) buf.closeBlock();
		}
		// When more than one production used, we have to peek and switch
		else {
			// Add infinite loop around the productions' code for an
			// action which want to efficiently reenter the same rule.
			// Also, in that case we want to reuse the same buffer on the position 
			// stack when reentering the rule, and thus need to allocate it once
			// and for all outside the switch.
			if (continued) {
				int maxsize = prodTable.keySet().stream().mapToInt(
					prod -> Iterables.size(prod.actuals())).max().getAsInt();
				buf.emitlnIf(config.positions, "enter(" + maxsize + ");");
				buf.emitln(ruleName + ":");
				buf.emit("while (true)").openBlock();
			}
			buf.emit("switch (peek().getKind())").openBlock();
			for (Map.Entry<Production, List<String>> entry : prodTable.entrySet()) {
				final Production prod = entry.getKey();
				boolean first = true;
				for (String term : entry.getValue()) {
					if (first) first = false;
					else buf.newline();
					buf.emit("case ").emit(term).emit(":");
				}
				buf.openBlock();
				genProduction(continuation, prod);
				buf.closeBlock();
			}
			// Generate a default rule for when no tokens
			if (trans.size() < grammar.tokenDecls.size()) {
				// Output this magic sequence to be nice with target
				// projects which use the "signal even if 'default' case exists"
				// incomplete-switch warning on enums.
				buf.emitln("//$CASES-OMITTED$");
				buf.emit("default:").openBlock();
				buf.emit("throw tokenError(peek()");
				trans.keySet().forEach(tok -> buf.emit(", Token.Kind." + tok));
				buf.emit(");");
				buf.closeBlock0();
			}
			if (continued)
				buf.closeBlock0();
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
			.emit(imp.val).endTrackedRange(null)
			.newline());
		buf.newline();
		buf.emitln("/**")
		   .emitln(" * Parser generated by Dolmen " + Constants.VERSION)
		   .emitln(" */");
		genAnnotations(config.classAnnotations);
		buf.emit("public final class ").emit(name)
		   .emitIf(!config.positions, " extends org.stekikun.dolmen.codegen.BaseParser<")
		   .emitIf(config.positions, " extends org.stekikun.dolmen.codegen.BaseParser.WithPositions<")
		   .emit(name).emit(".Token>").openBlock();
		buf.newline();
		
		// Before anything else, generate the token class
		TokensOutput tokensOutput = 
			new TokensOutput("Token", config, grammar.tokenDecls, buf);
		tokensOutput.genTokens();
		tokensOutput = null;	// free mem
		buf.newline();
		
		genHeader();
		genConstructor(name);
		genMethods();

		// Generate a nest of recursive parsing methods,
		// one for each non-terminal
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
	 * Same as {@link #output(Writer, String, Config, Grammar, Grammars.PredictionTable)}
	 * with the default configuration {@link Config#DEFAULT}.
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
		return output(writer, className, Config.DEFAULT, grammar, predict);
	}
}
