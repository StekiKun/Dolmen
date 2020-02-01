package org.stekikun.dolmen.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.stekikun.dolmen.codegen.LexBuffer;
import org.stekikun.dolmen.codegen.LexBuffer.LexicalError;
import org.stekikun.dolmen.codegen.LexBuffer.Position;
import org.stekikun.dolmen.common.Prompt;

/**
 * This class provides utility methods to help debug lexical analyzers
 * which have been generated with Dolmen. They give various ways to
 * use a lexical analyzer to completely tokenize some input, and record
 * the various tokens returned.
 * <p>
 * These methods are both useful for quick-and-dirty testing of a 
 * lexical analyzer, e.g. manually typing test input sequences and 
 * checking the analyzer behaves as expected, as well as for testing
 * complete files and recording the precise results of the lexical analysis,
 * which can be very useful as a non-regression tool in the continuous
 * integration of a project.
 * <p>
 * In particular, the input positions associated to tokens can be
 * retrieved as well, as they are usually important for error reporting
 * and used in parsers being built on top of the lexical analyzers as
 * well. They are usually hard to thoroughly check and debug, in particular
 * as subtle changes to a lexer description can break locations without
 * affecting the behaviour of the analyzer otherwise.
 * 
 * @see #tokenize(LexerInterface, String, Reader, Writer, boolean)
 * @see #prompt(LexerInterface, boolean)
 * @see #file(LexerInterface, File, File, boolean)
 * 
 * @author Stéphane Lescuyer
 */
public final class Tokenizer {

	private Tokenizer() {
		// Static utility only
	}

	private static StringBuilder appendPos(StringBuilder buf, Position pos) {
		int n = pos.filename.lastIndexOf('/');
		String file = n == -1 ? pos.filename : pos.filename.substring(n + 1);
		buf.append(file).append("[");
		buf.append(pos.line).append(",").append(pos.bol).append("+").append(pos.offset - pos.bol);
		buf.append("]");
		return buf;
	}

	private static String locToString(Position start, Position end) {
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		appendPos(buf, start).append("..");
		appendPos(buf, end).append(")");
		return buf.toString();
	}
	
	/**
	 * This interface acts as a generic proxy to using a Dolmen-generated
	 * lexer in the static debugging functions provided in {@link Tokenizer}.
	 * It must provide a way to {@link #makeLexer(String, Reader) create a lexer},
	 * an {@link #entry(LexBuffer) entry point} to use to tokenize input,
	 * and a {@link #halt(Object) halting condition} used to stop the tokenization
	 * (typically, recognizing an <i>end-of-input</i> special token).
	 * <p>
	 * For convenience, a static factory {@link LexerInterface#of(BiFunction, Function, Object)} 
	 * is provided, so that if {@code MyLexer} has been generated by Dolmen with
	 * a {@code main} entry point and uses some {@code Token.EOF} token for the
	 * end-of-input, one can simply use:
	 * <pre>
	 *   LexerInterface.of(MyLexer::new, MyLexer::main, Token.EOF)
	 * </pre>
	 * to build a suitable tokenizing interface for that lexical analyzer.
	 * 
	 * @author Stéphane Lescuyer
	 *
	 * @param <L>	the type of {@link LexBuffer} that this instance handles
	 * @param <T>	the type of tokens returned by the lexer's main entry
	 */
	public static interface LexerInterface<L extends LexBuffer, T> {
		/**
		 * @param inputName
		 * @param reader
		 * @return a lexical analyzer based on the given input stream
		 */
		L makeLexer(String inputName, Reader reader);
		
		/**
		 * Calls an entry in the given lexing buffer
		 * 
		 * @param lexbuf
		 * @return the next token returned by the entry
		 * @throws LexBuffer.LexicalError
		 */
		T entry(L lexbuf) throws LexBuffer.LexicalError;
		
		/**
		 * @param token		the last token returned by {@link #entry(LexBuffer)}
		 * @return whether tokenization should stop at this token
		 */
		boolean halt(T token);
		
		/**
		 * Typical usage of this method when {@code MyLexer} has been generated
		 * by Dolmen with a {@code main} entry point and uses some {@code Token.EOF}
		 * token for the end-of-input:
		 * <pre>
		 *   LexerInterface.of(MyLexer::new, MyLexer::main, Token.EOF)
		 * </pre>
		 * 
		 * @param makeLexer
		 * @param entry
		 * @param eofToken
		 * @return a tokenization interface which builds lexical buffers
		 * 	using {@code makeLexer}, uses the entry point described by
		 *  {@code entry}, and stops as soon as the token {@code eofToken}
		 *  is encountered.
		 */
		public static <L extends LexBuffer, T>
		LexerInterface<L, T> of(
			BiFunction<String, Reader, L> makeLexer,
			Function<L, T> entry, T eofToken) {
			return new LexerInterface<L, T>() {
				@SuppressWarnings("null")
				@Override
				public L makeLexer(String inputName, Reader reader) {
					return makeLexer.apply(inputName, reader);
				}

				@Override
				public T entry(L lexbuf) throws LexicalError {
					return entry.apply(lexbuf);
				}

				@Override
				public boolean halt(T token) {
					return Objects.equals(token, eofToken);
				}
			};
		}
	}

	/**
	 * Retrieves the tokens from {@code lexbuf} until the halting
	 * condition in {@code lexer} is met, and outputs them to
	 * {@code writer}, one on each line.
	 * 
	 * @param lexer			an interface to the lexical analyzer to use
	 * @param lexbuf		the lexing buffer to read from
	 * @param writer		character stream to write the tokens to
	 * @param positions		whether token locations are displayed as well
	 * @return the number of tokens that have been recognized
	 * 
	 * @throws LexBuffer.LexicalError
	 * @throws IOException
	 */
	private static <L extends LexBuffer, T> 
	long tokenize(LexerInterface<L, T> lexer, L lexbuf, Writer writer, boolean positions)
		throws LexBuffer.LexicalError, IOException {
		long count = 0;
		while (true) {
			T tok = lexer.entry(lexbuf);
			if (lexer.halt(tok))
				break;
			if (positions) {
				writer.append(String.format("%-30s", Objects.toString(tok)));
				writer.append(locToString(lexbuf.getLexemeStart(), lexbuf.getLexemeEnd()));
			}
			else
				writer.append(String.format("%s", Objects.toString(tok)));
			writer.append("\n");
			++count;
		}
		return count;
	}

	/**
	 * Initializes a lexical analyzer with the given input stream,
	 * based on the {@code lexer} interface, and repeatedly consumes
	 * tokens from the input until the halting condition in {@code lexer}
	 * is met. The tokens are displayed, one per line, using the given
	 * {@code writer}. Optionally, the start and end positions of each
	 * token can be displayed as well along the token.
	 * <p>
	 * Potential lexical and IO errors are caught and displayed, and
	 * abort the tokenization process. <b>This method does not attempt
	 * to close the given reader/writer streams, this should be handled
	 * by the caller as necessary.</b>
	 * 
	 * @param lexer			an interface to the lexical analyzer to use 
	 * @param inputName		a user-friendly name describing the input
	 * @param reader		character stream to feed the lexer with
	 * @param writer		character stream to write the tokens to
	 * @param positions		whether token locations are displayed as well
	 */
	public static <L extends LexBuffer, T>
	void tokenize(LexerInterface<L, T> lexer, 
			String inputName, Reader reader, Writer writer, boolean positions) {
		try {
			tokenize(lexer, lexer.makeLexer(inputName, reader), writer, positions);
		} catch (LexicalError | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method can be used to conveniently test a lexical analyzer
	 * against various one-line sentences entered manually or fed from a
	 * test file. It reads one line from the standard input at a time
	 * and tokenizes it using the given {@code lexer}, as described
	 * by {@link #tokenize(LexerInterface, LexBuffer, Writer, boolean)}.
	 * In response, the tokens are displayed on standard output, one per line.
	 * Optionally, the start and end positions of each
	 * token can be displayed as well along the token.
	 * <p>
	 * Potential lexical and IO errors are caught and displayed, and
	 * handling of the subsequent lines on standard input resumes normally.
	 * The method stops when encountering end-of-input or a totally empty line.
	 * 
	 * <i>Of course, this method is not suitable to test sentences which
	 * themselves contain line breaks.</i>
	 * 
	 * @param lexer			an interface to the lexical analyzer to use
	 * @param positions		whether token locations are displayed as well
	 */
	public static <L extends LexBuffer, T>
	void prompt(LexerInterface<L, T> lexer, boolean positions) {
		while (true) {
			StringWriter writer = new StringWriter(100);
			String line = Prompt.getInputLine("");
			if (line == null || "".equals(line))
				break;
			
			try {
				tokenize(lexer, lexer.makeLexer("", new StringReader(line)), writer, positions);
				System.out.println(writer);
			} catch (LexicalError | IOException e) {
				// Print the partial tokens before the exception
				System.out.println(writer);
				e.printStackTrace(System.out);
			}
		}
	}

	/**
	 * Uses the given {@code lexer} interface to tokenize the contents
	 * of the file {@code input}, and stores the result in the
	 * {@code output} file. The tokenization process repeatedly consumes
	 * tokens from the input until the halting condition in {@code lexer}
	 * is met. The tokens are displayed, one per line, in the output.
	 * Optionally, the start and end positions of each
	 * token can be displayed as well along the token.
	 * <p>
	 * Potential lexical and IO errors are caught and displayed on standard 
	 * output, and abort the tokenization process.
	 * 
	 * @param lexer			an interface to the lexical analyzer to use
	 * @param positions		whether token locations are displayed as well
	 */
	public static <L extends LexBuffer, T>
	void file(LexerInterface<L, T> lexer, File input, File output, boolean positions) {
		try (FileReader reader = new FileReader(input);
			BufferedReader bufReader = new BufferedReader(reader);
			FileWriter writer = new FileWriter(output);
			BufferedWriter bufWriter = new BufferedWriter(writer)) {
			tokenize(lexer, input.toString(), bufReader, bufWriter, positions);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
