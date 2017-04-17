package test.examples;

import automaton.Automata;
import automaton.Determinize;
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

	// Common regular expressions
	
	private final static CSet lalpha = CSet.interval('a', 'z');
	private final static CSet ualpha = CSet.interval('A', 'Z');
	private final static CSet digit = CSet.interval('0', '9');
	
	/**
	 * A simple lexer definition example with
	 * a single entry matching basic identifiers:
	 * 
	 *  rule ident: [_a-zA-Z][_a-zA-Z0-9]* { dummy action }
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
			new Lexer.Entry("ident", false, Lists.empty(),
				Maps.singleton(ident, Location.DUMMY));
		
		final static Lexer LEXER = 
			new Lexer(Location.DUMMY, Lists.singleton(entry), Location.DUMMY);
	}
	
	/**
	 * A simple lexer definition example with
	 * a single entry matching basic identifiers ending
	 * with a '$' sign. Yeah, string identifiers from
	 * good ol' Basic.
	 * 
	 *  rule ident: [_a-zA-Z][_a-zA-Z0-9]*$ { dummy action }
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class BasicIDs {
		private final static Regular ident =
			Regular.seq(SimpleIDs.ident, 
				Regular.chars(CSet.singleton('$')));
		private final static Lexer.Entry entry =
			new Lexer.Entry("ident", false, Lists.empty(),
				Maps.singleton(ident, Location.DUMMY));
		
		final static Lexer LEXER = 
			new Lexer(Location.DUMMY, Lists.singleton(entry), Location.DUMMY);
	}
	
	private static void test(Lexer lexer) {
		System.out.println("=========LEXER========");
		System.out.println(lexer);
		System.out.println("--------ENCODED-------");
		TLexer tlexer = Encoder.encodeLexer(lexer);
		System.out.println(tlexer);
		System.out.println("--------AUTOMATA------");
		Automata aut = Determinize.lexer(lexer);
		System.out.println(aut);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test(SimpleIDs.LEXER);
		test(BasicIDs.LEXER);
	}
	
}
