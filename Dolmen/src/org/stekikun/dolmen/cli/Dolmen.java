package org.stekikun.dolmen.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.List;

import org.stekikun.dolmen.automaton.Automata;
import org.stekikun.dolmen.automaton.Determinize;
import org.stekikun.dolmen.cli.Args.ArgsParsingException;
import org.stekikun.dolmen.cli.Args.Item;
import org.stekikun.dolmen.codegen.AutomataOutput;
import org.stekikun.dolmen.codegen.BaseParser.ParsingException;
import org.stekikun.dolmen.codegen.Config;
import org.stekikun.dolmen.codegen.GrammarOutput;
import org.stekikun.dolmen.codegen.LexBuffer.LexicalError;
import org.stekikun.dolmen.common.Bookkeeper;
import org.stekikun.dolmen.common.CountingWriter;
import org.stekikun.dolmen.jge.JGELexer;
import org.stekikun.dolmen.jge.JGEParser;
import org.stekikun.dolmen.jle.JLELexer;
import org.stekikun.dolmen.jle.JLEParser;
import org.stekikun.dolmen.syntax.IReport;
import org.stekikun.dolmen.syntax.Lexer;
import org.stekikun.dolmen.syntax.PGrammar;
import org.stekikun.dolmen.syntax.PGrammars;
import org.stekikun.dolmen.syntax.Reporter;
import org.stekikun.dolmen.unparam.Expansion;
import org.stekikun.dolmen.unparam.Expansion.PGrammarNotExpandable;
import org.stekikun.dolmen.unparam.Grammar;
import org.stekikun.dolmen.unparam.Grammars;

/**
 * This class contains the entry-point routines when using Dolmen
 * as a command-line tool.
 * 
 * <p> The command-line options per se are
 * defined in {@link Item}, and this class is defined as the
 * application entry point for the Dolmen JAR. It can handle
 * the generation of both lexical and syntactic analyzers.
 * 
 * @author Stéphane Lescuyer
 */
public final class Dolmen {

	/**
	 * A special {@link PrintStream} printing nothing.
	 * It is used in {@link Item#QUIET quiet mode}.
	 */
	private static final PrintStream nullStream =
		new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		});
	
	/**
	 * Appends the problems in {@code reports} to the given file {@code out}.
	 * 
	 * @param reporter
	 * @param out
	 * @throws IOException
	 */
	private static void report(List<? extends IReport> reports, FileWriter out) throws IOException {
		if (reports.isEmpty()) return;
		for (IReport report : reports)
			out.append(report.display()).append("\n\n");
	}
	
	/**
	 * Generates a lexical analyzer from the file {@code input}, which should
	 * be a Dolmen lexer description. The generated lexical analyzer will be
	 * a compilation unit with name {@code className} and shall be stored in
	 * the {@code output} file location.
	 * 
	 * @param args			command-line arguments
	 * @param input			input lexer description
	 * @param output		where the lexical analyzer shall be written
	 * @param reportsFile   where the problems will be reported
	 * @param className		the class name of the generated lexical analyzer
	 */
	private static void generateLexer(Args args,
			File input, File output, File reportsFile, String className) {
		PrintStream log = args.getFlag(Item.QUIET) ? nullStream : System.out;
		final Bookkeeper tasks = Bookkeeper.start(log, "Compiling lexer description " + input);

		try (FileReader reader = new FileReader(input);
			 FileWriter reports = new FileWriter(reportsFile))
		{
			JLELexer jlLexer = new JLELexer(input.getPath(), reader);
			try {
				JLEParser jlParser = new JLEParser(jlLexer, JLELexer::main);
				Lexer lexer = jlParser.lexer();
				tasks.done("Lexer description successfully parsed");
				
				Reporter configReporter = new Reporter();
				Config config = Config.ofLexer(lexer, configReporter);
				tasks.problems(configReporter.getReports().size());
				report(configReporter.getReports(), reports);
				
				Automata aut = Determinize.lexer(lexer, true);
				tasks.done("Compiled lexer description to automata");
				tasks.infos("(" + aut.automataCells.length + " states in " 
						+ aut.automataEntries.size() + " automata)");
				
				List<IReport> autReports = aut.findProblems(lexer);
				tasks.problems(autReports.size());
				report(autReports, reports);
				
				try (Writer writer = 
						new CountingWriter(new FileWriter(output, false))) {
					writer.append("package " + args.getString(Item.PACKAGE) + ";\n\n");
					AutomataOutput.output(writer, className, config, aut);
					tasks.leaveWith("Generated lexer in " + output);
				} catch (IOException e) {
					e.printStackTrace(log);
					tasks.aborted("Could not output generated lexer");
					return;
				}			
				return;
			}
			catch (LexicalError e) {
				tasks.aborted("Lexical error in lexer description");
				System.out.println(e.getMessage());
			}
			catch (ParsingException e) {
				tasks.aborted("Syntax error in lexer description");
				System.out.println(e.getMessage());
			}
			catch (Lexer.IllFormedException e) {
				tasks.aborted("Lexer description is not well-formed");
				System.out.println(e.getMessage());
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace(log);
		}
		catch (IOException e) {
			e.printStackTrace(log);
		} 
	}

	/**
	 * Generates a syntactic analyzer from the file {@code input}, which should
	 * be a Dolmen parser description. The generated syntactic analyzer will be
	 * a compilation unit with name {@code className} and shall be stored in
	 * the {@code output} file location.
	 * 
	 * @param args			command-line arguments
	 * @param input			input parser description
	 * @param output		where the syntactic analyzer shall be written
	 * @param reportsFile   where the problems will be reported
	 * @param className		the class name of the generated syntactic analyzer
	 */
	private static void generateParser(Args args,
			File input, File output, File reportsFile, String className) {
		PrintStream log = args.getFlag(Item.QUIET) ? nullStream : System.out; 
		final Bookkeeper tasks = Bookkeeper.start(log, "Compiling grammar description " + input);
		
		try (FileReader reader = new FileReader(input);
			 FileWriter reports = new FileWriter(reportsFile))
		{
			JGELexer jgLexer = new JGELexer(input.getPath(), reader);
			JGEParser jgParser = new JGEParser(jgLexer, JGELexer::main);
			try {
				PGrammar pgrammar = jgParser.start();
				tasks.done("Grammar description successfully parsed");

				Reporter configReporter = new Reporter();
				Config config = Config.ofPGrammar(pgrammar, configReporter);
				tasks.problems(configReporter.getReports().size());
				report(configReporter.getReports(), reports);
				
				tasks.enter("Grammar expansion");
				Reporter pdepsReporter = new Reporter();
				PGrammars.Dependencies deps = PGrammars.dependencies(pgrammar.rules);
				PGrammars.findUnusedSymbols(pgrammar, deps, pdepsReporter);
				PGrammars.analyseGrammar(pgrammar, deps, pdepsReporter);
				tasks.done("Analysed parametric rules");
				tasks.problems(pdepsReporter.getReports().size());
				if (pdepsReporter.hasErrors()) {
					tasks.aborted("Inconsistent use of parametric rules");
					System.out.println(pdepsReporter);
					return;
				}
				report(pdepsReporter.getReports(), reports);
	
				Expansion.checkExpandability(pgrammar);
				tasks.done("Expandability check successful");
				Grammar grammar = Expansion.of(pgrammar);
				tasks.leaveWith("Expanded to ground grammar");
				tasks.infos("(" + grammar.rules.size() + " ground non-terminals"
						+ " from " + pgrammar.rules.size() + " rules)");
				
				Reporter depsReporter = new Reporter();
				Grammars.PredictionTable predictTable =
					Grammars.predictionTable(grammar, 
						Grammars.analyseGrammar(grammar, null, depsReporter));
				tasks.done("Analysed expanded grammar and built prediction table");
				tasks.problems(depsReporter.getReports().size());
				report(depsReporter.getReports(), reports);
				List<IReport> conflicts = predictTable.findConflicts();
				if (!conflicts.isEmpty()) {
					tasks.aborted("Expanded grammar is not LL(1)");
					conflicts.forEach(conf -> System.out.println(conf.display()));
					return;
				}
				tasks.done("Expanded grammar is LL(1)");
				
				try (Writer writer =
						new CountingWriter(new FileWriter(output, false))) {
					writer.append("package " + args.getString(Item.PACKAGE) + ";\n\n");
					GrammarOutput.output(writer, className, config, grammar, predictTable);
					tasks.leaveWith("Generated parser in " + output);
				} catch (IOException e) {
					e.printStackTrace(log);
					tasks.aborted("Could not output generated parser");
					return;
				}
				
				return;
			}
			catch (LexicalError e) {
				tasks.aborted("Lexical error in grammar description");
				System.out.println(e.getMessage());
			}
			catch (ParsingException e) {
				tasks.aborted("Syntax error in grammar description");
				System.out.println(e.getMessage());
			}
			catch (PGrammar.IllFormedException e) {
				tasks.aborted("Grammar description is not well-formed");
				System.out.println(e.getMessage());
			}
			catch (Grammar.IllFormedException e) {
				tasks.aborted("Grammar description is not well-formed");
				System.out.println(e.getMessage());
			}
			catch (PGrammarNotExpandable e) {
				tasks.aborted("Grammar is not expandable");
				System.out.println(e.getReport().display());
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace(log);
		}
		catch (IOException e) {
			e.printStackTrace(log);
		}
	}
	
	/**
	 * Handles a command-line request with source file {@code filename}
	 * and parsed command-line arguments {@code args}. The input file
	 * should exist, be readable, and be either a lexer description or
	 * a parser description.
	 * 
	 * @param args			the command-line arguments
	 * @param filename		the input file name
	 */
	private static void handle(Args args, String filename) {
		File file = new File(filename);
		if (!file.exists()) {
			System.out.println("Cannot find file " + filename);
			return;
		}
		if (!file.canRead()) {
			System.out.println("Cannot read file " + filename + ", check permissions");
			return;
		}
		if (!file.isFile()) {
			System.out.println("Specified file " + filename + " is not a regular file");
			return;
		}
		
		// If neither --lexer or --grammar was specified, we figure out automatically
		// using the extension
		boolean lexer;
		if (!args.getFlag(Item.LEXER) && !args.getFlag(Item.PARSER)) {
			if (filename.endsWith(".jl")) {
				lexer = true;
			}
			else if (filename.endsWith(".jg")) {
				lexer = false;
			}
			else {
				System.out.println("Cannot determine whether " + filename
						+ " is a lexer or parser description. Please specify --lexer or --grammar.");
				return;
			}
		}
		else
			// By exclusion, we also know that lexer = !args.getFlag(Item.PARSER)
			lexer = args.getFlag(Item.LEXER);
		
		// Compute the generated class name and filename
		String className = args.getString(Item.CLASS);
		if (className.isEmpty()) {
			String fname = file.getName();
			int ext = fname.lastIndexOf('.');
			className = ext < 0 ? fname : fname.substring(0, ext);
		}
		String dir = args.getString(Item.OUTPUT);
		if (!dir.endsWith("/")) dir += "/";
		String genFilename = dir + className + ".java";
		
		// Making sure the destination file can be accessed as well
		File genFile = new File(genFilename);
		if (!genFile.exists())
			try {
				genFile.createNewFile();
			} catch (IOException e) {
				System.out.println("Cannot create output file " + genFile);
				return;
			}
		if (!genFile.canWrite()) {
			System.out.println("Cannot write to output file " +  genFile);
			return;
		}
		
		// Determine the file used for the problem reports
		File reportsFile;
		String reportsName = args.getString(Item.REPORTS);
		if (reportsName.isEmpty())
			reportsFile = new File(filename + ".reports");
		else
			reportsFile = new File(reportsName);

		// Call the lexer or parser generation, as adequate
		if (lexer)
			generateLexer(args, file, genFile, reportsFile, className);
		else
			generateParser(args, file, genFile, reportsFile, className);
	}
	
	/**
	 * Dolmen's command-line entry point
	 * <p>
	 * Possible command-line arguments are described in {@link Item}. 
	 * 
	 * @param args_
	 */
	public static void main(String[] args_) {
		// First of all, try and parse the arguments, abort if anything goes wrong
		Args args;
		String filename;
		try {
			args = Args.parse(args_);
			
			// If help was required, we display it right now
			if (args.getFlag(Item.HELP)) {
				System.out.println(Args.getUsage());
			}
			
			// Check that there is exactly one stray argument, which must be the source
			if (args.getExtras().isEmpty())
				throw new ArgsParsingException("No source file was specified");
			if (args.getExtras().size() > 1)
				throw new ArgsParsingException("Exactly one source file required");
			filename = args.getExtras().get(0);
		}
		catch (ArgsParsingException e) {
			System.out.println(e.getMessage());
			System.out.println(Args.getUsage());
			return;
		}
		
		// Handle the command
		handle(args, filename);
	}
}