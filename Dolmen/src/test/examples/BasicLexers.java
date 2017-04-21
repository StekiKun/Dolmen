package test.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import automaton.Automata;
import automaton.Determinize;
import codegen.AutomataOutput;
import common.CSet;
import common.Lists;
import common.Maps;
import syntax.Lexer;
import syntax.Location;
import syntax.Regular;
import tagged.Encoder;
import tagged.TLexer;

/**
 * Manually constructed basic lexer definitions,
 * used as examples for testing
 * 
 * @author Stéphane Lescuyer
 */
public abstract class BasicLexers {

	private BasicLexers() {
		// Static utility only
	}

	final static Location VOID = Location.inlined("void");
	
	// Common regular expressions
	
	final static CSet lalpha = CSet.interval('a', 'z');
	final static CSet ualpha = CSet.interval('A', 'Z');
	final static CSet digit = CSet.interval('0', '9');
	
	// Ordered clauses
	
	static Map<Regular, Location> clauses(@NonNull Regular... regs) {
		Map<Regular, Location> res = new LinkedHashMap<>(regs.length);
		for (int i = 0; i < regs.length; ++i)
			res.put(regs[i], Location.DUMMY);
		return res;
	}
	
	/**
	 * A simple lexer definition example with
	 * a single entry matching basic identifiers:
	 * <pre>
	 *  rule ident: [_a-zA-Z][_a-zA-Z0-9]* { dummy action }
	 * </pre>
	 * The expected DFA should have two states and be like this:
	 * <pre>
	 *  state 0: {initial}
	 *  	[_a-zA-Z] -> Goto 1
	 *  state 1: {final -> action 0}
	 *  	[_a-zA-Z0-9] -> Goto 1
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class SimpleIDs {
		private final static CSet idstart =
			CSet.union(CSet.union(lalpha, ualpha), CSet.singleton('_'));
		private final static CSet idbody =
			CSet.union(digit, idstart);
		
		private final static Regular ident =
			Regular.seq(Regular.chars(idstart), 
				Regular.star(Regular.chars(idbody)));
		private final static Lexer.Entry entry =
			new Lexer.Entry("ident", VOID, false, Lists.empty(),
				Maps.singleton(ident, Location.DUMMY));
		
		final static Lexer LEXER = 
			new Lexer(Lists.empty(), Location.DUMMY, 
					Lists.singleton(entry), Location.DUMMY);
	}
	
	/**
	 * A simple lexer definition example with
	 * a single entry matching basic identifiers ending
	 * with a '$' sign. Yeah, string identifiers from
	 * good ol' Basic.
	 * <pre>
	 *  rule ident: [_a-zA-Z][_a-zA-Z0-9]*$ { dummy action }
 	 * </pre>
	 * The expected DFA should have three states and be like this:
	 * <pre>
	 *  state 0: {initial}
	 *  	[_a-zA-Z]    -> Goto 1
	 *  state 1:
	 *  	[_a-zA-Z0-9] -> Goto 1
	 *  	$            -> Goto 2
	 *  state 2: {final -> action 0}
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class BasicIDs {
		private final static Regular ident =
			Regular.seq(SimpleIDs.ident, 
				Regular.chars(CSet.singleton('$')));
		private final static Lexer.Entry entry =
			new Lexer.Entry("ident", VOID, false, Lists.empty(),
				Maps.singleton(ident, Location.DUMMY));
		
		final static Lexer LEXER = 
			new Lexer(Lists.empty(), Location.DUMMY,
					Lists.singleton(entry), Location.DUMMY);
	}

	/**
	 * A simple lexer definition example with
	 * a single entry matching basic identifiers ending
	 * with a '$' sign, and binding the part before the '$'
	 * sign to the name 'id'.
	 * <pre>
	 *  rule ident: ([_a-zA-Z][_a-zA-Z0-9]* as id)$ { dummy action }
	 * </pre>
	 * The expected DFA should be exactly as for {@link BasicIDs}
	 * if <i>optimisation</i> is activated (because all tags should
	 * be removed), and otherwise should look like this, for some
	 * cells N and M:
	 * <pre>
	 *  state 0: {initial (Set(N))}
	 *  	[_a-zA-Z]    -> Goto 1 (Set(M))
	 *  state 1:
	 *  	[_a-zA-Z0-9] -> Goto 1 (Set(M))
	 *  	$            -> Goto 2
	 *  state 2: {final -> action 0}
	 *  	Perform action 0, id.start <- N, id.end <- M
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class BoundIDs {
		private final static Regular ident =
			Regular.seq(
				Regular.binding(SimpleIDs.ident, "id", Location.DUMMY), 
				Regular.chars(CSet.singleton('$')));
		private final static Lexer.Entry entry =
			new Lexer.Entry("ident", VOID, false, Lists.empty(),
				Maps.singleton(ident, Location.inlined("System.out.println(id); return;")));
		
		final static Lexer LEXER = 
			new Lexer(Lists.empty(), Location.DUMMY, 
					Lists.singleton(entry), Location.DUMMY);
	}
	
	/**
	 * A simple lexer definition example with
	 * a single entry matching both basic identifiers and
	 * integer literals.
	 * <pre>
	 *  rule ident_int: 
	 *  | [_a-zA-Z][_a-zA-Z0-9]* { dummy action }
	 *  | [0-9]+                 { dmumy action }
	 * </pre>
	 * The expected DFA should have three states and look
	 * like this:
	 * <pre>
	 *  state 0: {initial}
	 *  	[_a-zA-Z] -> Goto 2
	 *      [0-9]     -> Goto 1
	 *  state 1: {final -> action 1}
	 *      [0-9] -> Goto 1
	 *  state 2: {final -> action 0}
	 *  	[_a-zA-Z0-9] -> Goto 2
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class IDsIntegers {
		private final static Regular integer =
			Regular.seq(
				Regular.chars(digit),
				Regular.star(Regular.chars(digit)));
		private final static Regular ident = SimpleIDs.ident;
		
		private final static Map<Regular, Location> clauses =
			clauses(ident, integer);
		private final static Lexer.Entry entry =
			new Lexer.Entry("ident_int", VOID, false, Lists.empty(), clauses);
		
		final static Lexer LEXER = 
			new Lexer(Lists.empty(), Location.DUMMY, 
					Lists.singleton(entry), Location.DUMMY);
	}
	
	/**
	 * A simple lexer definition example with
	 * a single entry matching both basic identifiers and
	 * also a couple of keywords 'DO' and 'FOR'. The keywords
	 * must be given priority over identifiers, but when
	 * longest match, identifiers can have a keyword as prefix.
	 * <pre>
	 *  rule identkw:
	 *  | "DO"					 { dummy action }
	 *  | "FOR"                  { dummy action }
	 *  | [_a-zA-Z][_a-zA-Z0-9]* { dummy action }
	 * </pre>
	 * The expected DFA should have seven states and look
	 * like this:
	 * <pre>
	 *  state 0: {initial}
	 *  	[_a-zA-CEG-Z] -> Goto 1
	 *      F             -> Goto 2
	 *      D             -> Goto 3
	 *  state 1: {final -> action 2}	# not a keyword
	 *      [_a-zA-Z0-9] -> Goto 1
	 *  state 2: {final -> action 2}	# F.. 
	 *  	[_a-zA-NP-Z0-9] -> Goto 1
	 *      O               -> Goto 5
	 *  state 3: {final -> action 2}	# D.. 
	 *  	[_a-zA-NP-Z0-9] -> Goto 1
	 *      O               -> Goto 4
	 *  state 4: {final -> action 0}	# DO.. 
	 *  	[_a-zA-Z0-9] -> Goto 1
	 *  state 5: {final -> action 2}    # FO..
	 *  	[_a-zA-QS-Z0-9] -> Goto 1
	 *      R               -> Goto 6
	 *  state 6: {final -> action 1}    # FOR..
	 *      [_a-zA-Z0-9] -> Goto 1
	 * </pre>
	 * With <i>shortest-match rule</i> instead, the
	 * keywords are never matched and the DFA has four states,
	 * but could be reduced to two if minimized:
	 * <pre>
	 *  state 0: {initial}
	 *  	[_a-zA-CEG-Z] -> Goto 1
	 *      F             -> Goto 2
	 *      D             -> Goto 3
	 *  state 1: {final -> action 2}
	 *  state 2: {final -> action 2}
	 *  state 3: {final -> action 2}
	 * </pre>
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class IDsKeywords {
		private final static Regular DO = Regular.string("DO");
		private final static Regular FOR = Regular.string("FOR");
		private final static Regular ident = SimpleIDs.ident;
		
		private final static Map<Regular, Location> clauses =
			clauses(DO, FOR, ident);
		private final static Lexer.Entry entry =
			new Lexer.Entry("identkw", VOID, false, Lists.empty(), clauses);
		private final static Lexer.Entry sh_entry =
				new Lexer.Entry("identkw", VOID, true, Lists.empty(), clauses);
		
		final static Lexer LEXER =
			new Lexer(Lists.empty(), Location.DUMMY, 
					Lists.singleton(entry), Location.DUMMY);
		final static Lexer SHORTEST = 
			new Lexer(Lists.empty(), Location.DUMMY,
					Lists.singleton(sh_entry), Location.DUMMY);
	}
	
	static void test(Lexer lexer, boolean opt) {
		System.out.println("=========LEXER========");
		System.out.println(lexer);
		System.out.println("--------ENCODED-------");
		TLexer tlexer = Encoder.encodeLexer(lexer, opt);
		System.out.println(tlexer);		
		System.out.println("--------AUTOMATA------");
		Automata aut = Determinize.lexer(lexer, opt);
		System.out.println(aut);
	}

	static void testOutput(String className, Lexer lexer, boolean opt) {
		System.out.println("=========LEXER========");
		System.out.println(lexer);
		System.out.println("--------ENCODED-------");
		TLexer tlexer = Encoder.encodeLexer(lexer, opt);
		System.out.println(tlexer);		
		System.out.println("--------AUTOMATA------");
		Automata aut = Determinize.lexer(lexer, opt);
		System.out.println(aut);
		File file = new File("src-gen/" + className + ".java");
		try (FileWriter writer = new FileWriter(file, false)) {
			AutomataOutput.output(writer, className, aut);
			System.out.println("----------JAVA--------");
			System.out.println("Generated in " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// StringWriter writer = new StringWriter(1024);
//		try {
//			AutomataOutput.output(writer, className, aut);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.out.println("----------JAVA--------");
//		System.out.println(writer.toString());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		test(SimpleIDs.LEXER, true);
//		test(BasicIDs.LEXER, true);
//		test(BoundIDs.LEXER, true);
//		test(BoundIDs.LEXER, false);
//		test(IDsIntegers.LEXER, true);
//		test(IDsKeywords.LEXER, true);
//		test(IDsKeywords.SHORTEST, true);
		testOutput("SimpleIDs", SimpleIDs.LEXER, true);
		testOutput("BasicIDs", BasicIDs.LEXER, true);
		testOutput("BoundIDs", BoundIDs.LEXER, true);
		testOutput("BoundIDsNoOpt", BoundIDs.LEXER, false);
		testOutput("IDsIntegers", IDsIntegers.LEXER, true);
		testOutput("IDsKeywords", IDsKeywords.LEXER, true);
		testOutput("IDsKeywordsShortest", IDsKeywords.SHORTEST, true);
	}
	
}
