package codegen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import automaton.Automata;
import automaton.DFA;
import automaton.DFA.GotoAction;
import automaton.DFA.MemAction;
import automaton.DFA.Perform;
import automaton.DFA.Remember;
import automaton.DFA.Shift;
import automaton.DFA.TagAction;
import automaton.DFA.TransActions;
import common.CSet;
import syntax.Location;
import tagged.Optimiser.IdentInfo;
import tagged.Optimiser.TagAddr;
import tagged.TLexerEntry.Finisher;

/**
 * This class generates a Java class that simulates the
 * automata in some {@link Automata} instance. Each
 * {@link automaton.Automata.Entry entry} is realized
 * by an entry point with the corresponding return type.
 * 
 * @see #output(Writer, String, Automata)
 * 
 * @author Stéphane Lescuyer
 */
public final class AutomataOutput {

	/** The automata being generated */
	private final Automata aut;
	
	/** The code buffer being used */
	private final CodeBuilder buf;
	
	private AutomataOutput(Automata aut) {
		this.aut = aut;
		this.buf = new CodeBuilder(0);
	}

	private void genLexicalError() {
		buf.newline();
		buf.emitln("/**");
		buf.emitln(" * Exception raised by generated lexer");
		buf.emitln(" */");
		buf.emit("public static final class LexicalError extends RuntimeException")
		   .openBlock();
		buf.emitln("private static final long serialVersionUID = 1L;");
    	buf.newline();
		buf.emitln("/**");
		buf.emitln(" * @param msg	error message");
		buf.emitln(" */");
		buf.emit("public LexicalError(String msg)").openBlock();
		buf.emit("super(msg);").closeBlock();
		buf.closeBlock();
    }
	
	private void genConstructor(String name) {
		buf.newline();
	    buf.emitln("/**");
	    buf.emitln(" * Returns a fresh lexer based on the given character stream");
	    buf.emitln(" * @param inputname");
	    buf.emitln(" * @param reader");
	    buf.emitln(" */");
		buf.emit("public ").emit(name).emit("(String inputname, java.io.Reader reader)").openBlock();
	    buf.emit("super(inputname, reader);");
	    buf.closeBlock();
	}
		
	private void genHeader() {
		String hdr = aut.header.find();
		if (hdr.isEmpty()) return;
		buf.newline().emitln(aut.header.find());
	}

	private void genFooter() {
		String ftr = aut.footer.find();
		if (ftr.isEmpty()) return;
		buf.newline().emitln(aut.footer.find());
	}
	
	private static String cellName(int idx) {
		return "_jl_cell" + idx;
	}
	
	private static String memoryName(String entryName) {
		return "_jl_mem_" + entryName;
	}
	
	private void genMemAccess(int addr) {
		buf.emit("memory[" + addr + "]");
	}
	
	private void genRemember(Remember remember) {
		if (remember == Remember.NOTHING) return;
		buf.emitln("mark(" + remember.action + ");");
		genTagActions(remember.tagActions);
	}
	
	private void genTagActions(List<@NonNull TagAction> actions) {
		for (TagAction action : actions) {
			if (action.from >= 0) {
				// SetTag (tag <- from)
				genMemAccess(action.tag);
				buf.emit(" = ");
				genMemAccess(action.from);
				buf.emitln(";");
			}
			else {
				// EraseTag (tag)
				genMemAccess(action.tag);
				buf.emitln(" = -1;");
			}
		}
	}
	
	private void genPattern(CSet cset) {
		buf.emit("// ").emit(cset.toString());
		cset.forEach(c -> {
			buf.newline().emit("case " + (int)c + ":");
		});
	}
	
	private void genMemActions(List<@NonNull MemAction> memActions) {
		for (MemAction action : memActions) {
			int src = action.getSrc();
			int dst = action.getDest();
			if (src < 0) {
				// Set(dst)
				genMemAccess(dst);
				buf.emitln(" = curPos;");
			}
			else {
				// Copy(dst <- src)
				genMemAccess(dst);
				buf.emit(" = ");
				genMemAccess(src);
				buf.emitln(";");
			}
		}
	}
	
	private void genTransActions(TransActions trans) {
		genMemActions(trans.memActions);
		
		// Now generate the goto action
		GotoAction gotoAction = trans.gotoAction;
		if (gotoAction == GotoAction.BACKTRACK) {
			buf.emitln("return rewind();");
		}
		else {
			buf.emit("return ").emit(cellName(gotoAction.target))
							   .emit("();");
		}
	}
	
	private static TransActions mostFrequent(
		Map<@NonNull CSet, @NonNull TransActions> table) {
		int freq = -1;
		CSet mostFreq = CSet.EMPTY;	// irrelevant
		for (CSet cset : table.keySet()) {
			int card = cset.cardinal();
			if (freq < card) {
				freq = card; mostFreq = cset;
			}
		}
		return table.get(mostFreq);
	}
	
	private void genTransTable(
			Map<@NonNull CSet, @NonNull TransActions> table) {
		// Generates a large switch where each charset is
		// written as some or-pattern, and the most frequent
		// one uses "default"
		/// TODO: make a balanced binary decision tree to
		///		  minimize the amount of comparisons?
		///		  shouldn't the compiler do it?
		buf.emit("switch (getNextChar()) {").newline();
		TransActions defTrans = mostFrequent(table);
		table.forEach((cset, trans) -> {
			if (trans != defTrans) {
				genPattern(cset);
				buf.openBlock();
				genTransActions(trans);
				buf.closeBlock();
			}
		});
		
		buf.emit("default: ").openBlock();
		genTransActions(defTrans);
		buf.closeBlock();
		buf.emitln("}");
	}
	
	private void genCell(int cellIdx, DFA.Cell cell) {
		buf.newline()
			.emit("private int ").emit(cellName(cellIdx))
			.emit("(").emit(") throws java.io.IOException").openBlock();
		switch (cell.getKind()) {
		case PERFORM: {
			final Perform perform = (Perform) cell;
			genTagActions(perform.tagActions);
			buf.emit("return " + perform.action + ";");
			break;
		}
		case SHIFT: {
			final Shift shift = (Shift) cell;
			genRemember(shift.remember);
			genTransTable(shift.transTable);
			break;
		}
		}
		buf.closeBlock();
	}
	
	private void genEntryArgs(@Nullable Location args) {
		if (args == null) return;
		buf.emit(args.find());
	}
	
	private void genTagAddr(TagAddr addr) {
		switch (addr.base) {
		case TagAddr.START:
			buf.emit("startPos"); break;
		case TagAddr.END:
			buf.emit("curPos"); break;
		default:
			genMemAccess(addr.base); break;
		}
		if (addr.offset != 0) {
			buf.emit(" + ");
			if (addr.offset < 0)
				buf.emit("(" + addr.offset + ")");
			else buf.emit("" + addr.offset);
		}
	}
	
	private void genEnvBinding(String id, IdentInfo info) {
		// First find the actual type of the binding
		final String stype;
		if (info.end == null) {
			if (info.optional)
				stype = "Optional<Character> ";
			else
				stype = "char ";
		}
		else {
			if (info.optional)
				stype = "Optional<String> ";
			else
				stype = "String ";
		}
		// Then emit the definition
		buf.emit("final ").emit(stype).emit(id).emit(" = ");
		buf.emit("getSubLexeme")
		   .emit(info.optional ? "Opt" : "")
		   .emit(info.end == null ? "Char" : "")
		   .emit("(");
		genTagAddr(info.start);
		final @Nullable TagAddr end = info.end;
		if (end != null) {
			buf.emit(", ");
			genTagAddr(end);
		}
		buf.emitln(");");
	}
	
	private void genFinishers(List<@NonNull Finisher> finishers) {
		for (Finisher finisher : finishers) {
			buf.emit("case " + finisher.action + ": ").openBlock();
			// Prepare the environment with bindings, for the
			// semantic action
			finisher.tags.forEach(this::genEnvBinding);
			// Add the user-defined semantic action
			if (finisher.loc == Location.DUMMY) {
				buf.emit("return; // TODO: missing semantic action");
			}
			else
				buf.emit(finisher.loc.find());
			buf.closeBlock();
		}
		// Generate a default case for when input didn't match
		buf.emit("default:").incrIndent().newline();
		buf.emit("throw new LexicalError(\"Empty token\");");
		buf.decrIndent().newline();
	}
	
	private void genEntry(Automata.Entry entry) {
		buf.newline()
		    .emitln("/**")
		    .emit(" * Entry point for rule ").emitln(entry.name)
		    .emitln(" */")
			.emit(entry.visibility ? "public " : "private ")
			.emit(entry.returnType.find()).emit(" ")
			.emit(entry.name).emit("(");
		genEntryArgs(entry.args);
		buf.emit(") throws java.io.IOException").openBlock();
		// Initialization of lexer variables for this entry
		buf.emitln("// Initialize lexer for this automaton");
		if (entry.memSize > 0) {
			buf.emit("memory = ").emit(memoryName(entry.name)).emitln(";");
			buf.emitln("java.util.Arrays.fill(memory, -1);");
		}
		buf.emitln("start();");
		// Perform initial memory actions if any
		if (!entry.initializer.isEmpty()) {
			buf.emitln("// Memory actions for the initial state");
			genMemActions(entry.initializer);
		}
		// Launch the recognition...
		buf.emit("int result = ").emit(cellName(entry.initialState))
								 .emitln("();");
		// ...and switch on the returned action
		// (if Backtrack is encountered before a final state,
		//  lastAction will be -1)
		buf.emitln("switch (result) {");
		genFinishers(entry.finishers);
		buf.emitln("}");
		buf.closeBlock();
		// Add a final field for the memory used by this entry
		if (entry.memSize > 0) {
			buf.emit("private final int ")
				.emit("@org.eclipse.jdt.annotation.NonNull [] ")
				.emit(memoryName(entry.name))
				.emit(" = new int[").emit("" + entry.memSize).emitln("];");
		}
	}
	
	private void genClass(String name) {
		aut.imports.forEach(imp -> buf.emitln(imp));
		buf.emitln("/**")
		   .emitln(" * Lexer generated by Dolmen ")
		   .emitln(" */");
		buf.emitln("@org.eclipse.jdt.annotation.NonNullByDefault({})");
		buf.emit("public final class ").emit(name);
		buf.emit(" extends codegen.LexBuffer").openBlock();
		genLexicalError();
		
		genHeader();
		genConstructor(name);
		
		// For every automata entry, there will be a public
		// entry point
		for (Automata.@NonNull Entry entry : aut.automataEntries)
			genEntry(entry);
		
		// Generate code for every cell in the automata
		for (int i = 0; i < aut.automataCells.length; ++i)
			genCell(i, aut.automataCells[i]);
		
		genFooter();
		buf.closeBlock();
	}
	
	/**
	 * Generates the code from the automata {@code aut} in
	 * a Java class with name {@code className}, exported
	 * using the given {@code writer}.
	 * 
	 * @param writer
	 * @param className
	 * @param aut
	 * @throws IOException
	 */
	public static void output(Writer writer,
			String className, Automata aut) throws IOException {
		AutomataOutput output = new AutomataOutput(aut);
		output.genClass(className);
		output.buf.print(writer);
	}
	
}