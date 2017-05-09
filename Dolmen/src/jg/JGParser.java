package jg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import codegen.GrammarOutput;
import syntax.Grammar;
import syntax.GrammarRule;
import syntax.Grammars;
import syntax.Grammars.NTermsInfo;
import syntax.Grammars.PredictionTable;
import syntax.Location;
import syntax.Production;

/**
 * This class defines an instance of {@link Grammar}
 * which describes the grammar of Dolmen grammar
 * descriptions, with extension {@code .jg}. The
 * concrete syntax is summarized in docs/PARSER.syntax.
 * <p>
 * The parser is fed by a stream of tokens
 * as provided by the generated lexer {@link JGLexer}.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class JGParser {

	private JGParser() {
		// Static utility only
	}

	static Grammar.TokenDecl token(String name) {
		return new Grammar.TokenDecl(name, null);
	}
	
	static Grammar.TokenDecl vtoken(String name, String valType) {
		return new Grammar.TokenDecl(name, Location.inlined(valType));
	}
	
	static Production.Actual actual(String s) {
		int ndx = s.indexOf('=');
		final @Nullable String binding;
		if (ndx < 0)
			binding = null;
		else
			binding = s.substring(0, ndx).trim();
		
		@Nullable final Location args;
		final int bnd;
		int act = s.indexOf('(', ndx);
		if (act >= 0) {
			bnd = act;
			assert (s.charAt(s.length() - 1) == ')');
			@SuppressWarnings("null")
			@NonNull String args_ = s.substring(act + 1, s.length() - 1);
			args = Location.inlined(args_);
		}
		else {
			bnd = s.length();
			args = null;
		}
		@SuppressWarnings("null")
		@NonNull String item = s.substring(ndx + 1, bnd).trim();
		return new Production.Actual(binding, item, args);
	}
	
	static final Location VOID = Location.inlined("void");
	static final Location RETURN = Location.inlined("return;");
	
	static Production production(@NonNull String... items) {
		Production.Builder builder = new Production.Builder();
		for (int i = 0; i < items.length; ++i) {
			String item = items[i];
			if (item.startsWith("@")) {
				@SuppressWarnings("null")
				@NonNull String action = item.substring(1);
				builder.addAction(Location.inlined(action));
			}
			else
				builder.addActual(actual(item));
		}
		return builder.build();
	}
	
	private static GrammarRule vrule(boolean vis,
			String name_, @NonNull Object... productions) {
		// Find name/args in name parameter
		int par = name_.indexOf('(');
		@NonNull String name;
		@Nullable Location args;
		if (par < 0) {
			name = name_;
			args = null;
		} else {
			assert (name_.charAt(name_.length() - 1) == ')');
			@SuppressWarnings("null")
			@NonNull String tmp =  name_.substring(0, par);
			name = tmp;
			@SuppressWarnings("null")
			@NonNull String args_ = name_.substring(par + 1, name_.length() - 1);
			args = Location.inlined(args_);
		}
		
		// Find return type, if any
		Location retType = VOID;
		int first = 0;
		if (productions[0] instanceof String) {
			retType = Location.inlined((String) productions[0]);
			++first;
		}
		
		GrammarRule.Builder builder = new GrammarRule.Builder(vis, retType, name, args);
		for (int i = first; i < productions.length; ++i)
			builder.addProduction((Production) productions[i]);
		return builder.build();		
	}
	
	static GrammarRule rule(String name, @NonNull Object... productions) {
		return vrule(false, name, productions);
	}

	static GrammarRule prule(String name, @NonNull Object... productions) {
		return vrule(true, name, productions);
	}
	
	/**
	 * Grammar rules (see PARSER.syntax)
	 */
	
	@SuppressWarnings("null")
	private static final List<String> imports =
		Arrays.asList(
			"import org.eclipse.jdt.annotation.Nullable;",
			"import java.util.List;", "import java.util.ArrayList;",
			"import syntax.Location;"
//			, "import syntax.Grammar;",
//			"import syntax.GrammarRule;", "import syntax.Grammar.TokenDecl;"
			);
	private static final String header = "";
	
	private static final String footer =
			"/**\n" +
		"     * Testing this parser\n" +
		"     */\n" +
		"    public static void main(String[] args) throws java.io.IOException {\n" +
		"		String prompt;\n" +
		"       while ((prompt = common.Prompt.getInputLine(\">\")) != null) {\n" +
		"			try {\n" +
		"				JGLexer lexer = new JGLexer(\"-\",\n" +
		"					new java.io.StringReader(prompt));\n" +
		"				JGParserGenerated parser = new JGParserGenerated(lexer::main);\n" +
		"				System.out.println(parser.start());\n" +
		"			} catch (ParsingException e) {\n" +
		"				e.printStackTrace();\n" +
		"			}\n" +
		"		}\n" +
		"	}\n";
	
	/**
	 * The grammar description for .jg files
	 */
	public static final Grammar GRAMMAR =
		new Grammar.Builder(
			imports, Location.inlined(header), Location.inlined(footer))
			// IDENT of String | ACTION of Location | ARGUMENTS of Location 
			// | EQUAL | BAR | SEMICOL | IMPORT | STATIC | DOT | STAR 
			// | PUBLIC | PRIVATE | TOKEN | RULE | EOF
			.addToken(vtoken("IDENT", "String"))
			.addToken(vtoken("ACTION", "Location"))
			.addToken(vtoken("ARGUMENTS", "Location"))
			.addToken(token("EQUAL"))
			.addToken(token("BAR"))
			.addToken(token("DOT"))
			.addToken(token("STAR"))
			.addToken(token("SEMICOL"))
			.addToken(token("IMPORT"))
			.addToken(token("STATIC"))
			.addToken(token("PUBLIC"))
			.addToken(token("PRIVATE"))
			.addToken(token("TOKEN"))
			.addToken(token("RULE"))
			.addToken(token("EOF"))
			/**
			 * <pre>
			 * start -> imports tokens ACTION rules ACTION EOF
			 * </pre>
			 */
			.addRule(prule("start", "List<String>",
				production("imp = imports(null)", "@return imp;")))
//			.addRule(prule("start", "Grammar",
//				production("imp = imports(null)", "tdecls = tokens",
//						"header = ACTION", "rules = rules", "footer = ACTION", "EOF",
//						"@return new Grammar(imp, tdecls, header, rules, footer);")))
			/**
			 * <pre>
			 * imports ->
			 * imports -> IMPORT import_ SEMICO imports
			 * 
			 * import_ -> STATIC IDENT typename
			 * import_ -> IDENT typename
			 * 
			 * typename -> 
			 * typename -> DOT typename0
			 * 
			 * typename0 -> STAR
			 * typename0 -> IDENT typename
			 * </pre>
			 */
			.addRule(rule("imports(@Nullable List<String> imp)", "List<String>",
				production("@return imp == null ? Lists.empty() : imp;"),
				production("@List<String> acc = imp == null ? new ArrayList<String>() : imp;",
					"IMPORT", "elt = import_", "SEMICOL",
					"@acc.add(\"import \" + elt + \";\");",
					"imports(acc)",
					"@return acc;")))
			.addRule(rule("import_", "String",
				production("STATIC", "id = IDENT", "tn = typename",
					"@return \"static \" + id + tn;"),
				production("id = IDENT", "tn = typename",
					"@return id + tn;")))
			.addRule(rule("typename", "String",
				production("@return \"\";"),
				production("DOT", "ty = typename0", "@return \".\" + ty;")))
			.addRule(rule("typename0", "String",
				production("STAR", "@return \"*\";"),
				production("id = IDENT", "ty = typename", "@return id + ty;")))
			/**
			 * <pre>
			 * tokens -> 
			 * tokens -> TOKEN token tokens
			 * 
			 * token -> IDENT	// must be all upper case
			 * token -> ACTION IDENT
			 * </pre>
			 */
			
			/**
			 * <pre>
			 * rules ->
			 * rules -> rule rules
			 * 
			 * rule -> vis ACTION RULE IDENT EQUAL 
			 * 			production productions	// not all upper case
			 * 
			 * vis -> PUBLIC
			 * vis -> PRIVATE
			 * 
			 * productions ->
			 * productions -> production productions
			 * 
			 * production -> BAR items ACTION
			 * 
			 * items -> 
			 * items -> IDENT item items
			 * 
			 * item -> 
			 * item -> EQUAL IDENT
			 * </pre>
			 */
			
			.build();

	static void generateParser(String className, Grammar grammar)
			throws IOException {
		System.out.println(grammar);
		System.out.println("Analysing grammar description...");
		NTermsInfo infos = Grammars.analyseGrammar(grammar, null);
		PredictionTable predictTable = Grammars.predictionTable(grammar, infos);
		
		File file = new File("src/jg/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			writer.append("package jg;\n");
			if (!predictTable.isLL1()) {
				System.out.println("Cannot generate parser for non-LL(1) grammar:");
				System.out.println(predictTable.toString());
				return;
			}
			GrammarOutput.output(writer, className, grammar, predictTable);

		}
		System.out.println("Generated in " + file.getAbsolutePath());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		 // generateLexer(StraightLineProgram.LEXER, "StraightLineLexer");
		 generateParser("JGParserGenerated", GRAMMAR);
	}
}