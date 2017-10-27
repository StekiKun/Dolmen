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
			@NonNull String args_ = s.substring(act + 1, s.length() - 1);
			args = Location.inlined(args_);
		}
		else {
			bnd = s.length();
			args = null;
		}
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
			@NonNull String tmp =  name_.substring(0, par);
			name = tmp;
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
	
	private static final List<String> imports =
		Arrays.asList(
			"import org.eclipse.jdt.annotation.Nullable;",
			"import org.eclipse.jdt.annotation.NonNull;",
			"import java.util.List;", "import java.util.ArrayList;",
			"import common.Lists;", "import syntax.Location;",
			"import syntax.Production;", "import syntax.Grammar.TokenDecl;",
			"import syntax.GrammarRule;", "import syntax.Grammar;"
			);
	private static final String header =
			"/**\n" +
		"     * Returns {@code true} if the given string contains a lower-case letter\n" +
		"     */\n" +
		"     private static boolean isLowerId(String name) {\n" +
		"         return name.chars().anyMatch(ch -> Character.isLowerCase(ch));\n" +
		"     }\n";
	
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
			.addToken(vtoken("IDENT", "@NonNull String"))
			.addToken(vtoken("ACTION", "@NonNull Location"))
			.addToken(vtoken("ARGUMENTS", "@NonNull Location"))
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
			.addRule(prule("start", "@NonNull Grammar",
				production("imports = imports(null)", "tdecls = tokens(null)",
					"header = ACTION", "rules = rules(null)",
					"footer = ACTION", "EOF",
					"@Grammar.Builder builder = new Grammar.Builder(imports, header, footer);",
					"@tdecls.forEach(tdecl -> builder.addToken(tdecl));",
					"@rules.forEach(rule -> builder.addRule(rule));",
					"@return builder.build();")))
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
			.addRule(rule("imports(@Nullable List<@NonNull String> imp)",
					"@NonNull List<@NonNull String>",
				production("@return imp == null ? Lists.empty() : imp;"),
				production("@@NonNull List<@NonNull String> acc = imp == null ? new ArrayList<>() : imp;",
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
			.addRule(rule("tokens(@Nullable List<@NonNull TokenDecl> tokens)",
					"@NonNull List<@NonNull TokenDecl>",
				production("@return Lists.empty();"),
				production(
					"@@NonNull List<@NonNull TokenDecl> acc = tokens == null ? new ArrayList<>() : tokens;",
					"TOKEN", "tok = token",
					"@acc.add(tok);", "tokens(acc)", "@return acc;")))
			.addRule(rule("token", "@NonNull TokenDecl",
				production("id = IDENT", 
					"@if (isLowerId(id))",
					"@   throw new ParsingException(\"Token name should be all uppercase: \" + id);",
					"@return new TokenDecl(id, null);"),
				production("val = ACTION", "id = IDENT",
					"@if (isLowerId(id))",
					"@    throw new ParsingException(\"Token name should be all uppercase: \" + id);",
					"@return new TokenDecl(id, val);")))
			/**
			 * <pre>
			 * rules ->
			 * rules -> rule rules
			 * 
			 * rule -> vis ACTION RULE IDENT args EQUAL 
			 * 			production productions	// not all upper case
			 * 
			 * vis -> PUBLIC
			 * vis -> PRIVATE
			 * 
			 * args ->
			 * args -> ARGUMENTS
			 * </pre>
			 */
			.addRule(rule("rules(@Nullable List<@NonNull GrammarRule> rules)",
				"@NonNull List<@NonNull GrammarRule>", 
				production("@return Lists.empty();"),
				production("rule = rule",
					"@@NonNull List<@NonNull GrammarRule> acc = rules == null ? new ArrayList<>() : rules;",
					"@acc.add(rule);",
					"rules(acc)",
					"@return acc;")))
			.addRule(rule("rule", "@NonNull GrammarRule",
				production("vis = visibility", "rtype = ACTION", "RULE",
					"name = IDENT",
					"@if (!Character.isLowerCase(name.charAt(0)))",
					"@    throw new ParsingException(\"Rule name must start with a lower case letter: \" + name);",
					"args = args", "EQUAL",
					"@GrammarRule.@NonNull Builder builder =",
					"@	new GrammarRule.Builder(vis, rtype, name, args);",
					"prod = production", "@builder.addProduction(prod);",
					"productions(builder)",
					"@return builder.build();")))
			.addRule(rule("visibility", "boolean",
				production("PUBLIC", "@return true;"),
				production("PRIVATE", "@return false;")))
			.addRule(rule("args", "@Nullable Location",
				production("@return null;"),
				production("loc = ARGUMENTS", "@return loc;")))
			/**
			 * <pre>
			 * productions -> SEMICOL		// necessary to separate from footer
			 * productions -> production productions
			 * 
			 * production -> BAR items
			 * 
			 * items -> 
			 * items -> ACTION items
			 * items -> IDENT actual items
			 * 
			 * actual -> args
			 * actual -> EQUAL IDENT args
			 * </pre>
			 */
			.addRule(rule("productions(GrammarRule.@NonNull Builder builder)", "void",
				production("SEMICOL", "@return;"),
				production("prod = production", "@builder.addProduction(prod);",
						"productions(builder)", "@return;")))
			.addRule(rule("production", "@NonNull Production",
				production("BAR", "@Production.Builder builder = new Production.Builder();",
						"items(builder)", "@return builder.build();")))
			.addRule(rule("items(Production.Builder builder)", "void",
				production("@return;"),
				production("loc = ACTION", "@builder.addAction(loc);", "items(builder)", "@return;"),
				production("id = IDENT", "actual = actual(id)",
					"@builder.addActual(actual);", "items(builder)", "@return;")))
			.addRule(rule("actual(@NonNull String id)", "Production.@NonNull Actual",
				production("args = args", "@return new Production.Actual(null, id, args);"),
				production("EQUAL", "name = IDENT", "args = args",
					"@return new Production.Actual(id, name, args);")))
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
			GrammarOutput.output(writer, className,
				GrammarOutput.DEFAULT_CONFIG, grammar, predictTable);

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