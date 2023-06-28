package org.stekikun.dolmen.common;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Static utility functions to interact
 * with standard input and terminals
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Prompt {

	private Prompt() {
		// Static utility only
	}

	/**
	 * @param prompt	prompt line displayed
	 * @return the next line of standard input (blocking for it)
	 * 	or {@code null} if there is no such line (typically
	 *  standard input was closed)
	 */
	public static 
	@Nullable String getInputLine(String prompt) {
		try {
			System.out.print(prompt + "> ");
			/**
			 * This scanner is never closed, on purpose,
			 * because we don't want to close stdin!
			 * The important thing is the scanner itself
			 * does not escape the function.
			 */
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);
			return scanner.nextLine();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	private static String control(int code) {
		return "\033[" + Integer.toString(code) + "m";
	}
	
	/**
	 * @return the ANSI control sequence to reset
	 * 	text attributes
	 */
	public static String reset() {
		return control(0);
	}
	
	/**
	 * @return the ANSI control sequence to set
	 *  text to bold/bright
	 */
	public static String bold() {
		return control(1);
	}
	
	/**
	 * @return the ANSI control sequence to set
	 *  text color to dim
	 */
	public static String dim() {
		return control(2);
	}
	
	/**
	 * @return the ANSI control sequence to set
	 *  text to underlined
	 */
	public static String underlined() {
		return control(4);
	}
	
	/**
	 * @return the ANSI control sequence to set
	 * 	text to blinking
	 */
	public static String blink() {
		return control(5);
	}
	
	/**
	 * @return the ANSI control sequence to invert
	 *  foreground and background colors
	 */
	public static String reverse() {
		return control(7);
	}
	
	/**
	 * Enumerates basic 16 terminal colors along with their
	 * ANSI color code.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@SuppressWarnings("javadoc")
	public static enum Colors {
		DEFAULT(39),
		BLACK(30),
		RED(31),
		GREEN(32),
		YELLOW(33),
		BLUE(34),
		MAGENTA(35),
		CYAN(36),
		LIGHT_GRAY(37),
		DARK_GRAY(90),
		LIGHT_RED(91),
		LIGHT_GREEN(92),
		LIGHT_YELLOW(93),
		LIGHT_BLUE(94),
		LIGHT_MAGENTA(95),
		LIGHT_CYAN(96),
		WHITE(97),
		;
		
		/** The ANSI color code for this color, used as foreground color */
		public final int code;
		
		private Colors(int code) {
			this.code = code;
		}
	}
	
	/***
	 * @param c
	 * @return the ANSI control sequence to turn the
	 * 	text foreground color to {@code c}
	 */
	public static String fg(Colors c) {
		return control(c.code);
	}

	/***
	 * @param c
	 * @return the ANSI control sequence to turn the
	 * 	text background color to {@code c}
	 */
	public static String bg(Colors c) {
		return control(c.code + 10);
	}

	/**
	 * A wrapper around a {@link PrintStream} which may or may
	 * not support colored output via ANSI terminal control sequences.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class TermStream {
		private final PrintStream out;
		private final boolean colorSupport;
		
		/**
		 * Returns a wrapper around the given print stream {@code out}
		 * with support for ANSI control sequences if {@code colorSupport}
		 * is {@code true}. If instead, {@code colorSupport} is {@code false},
		 * methods such as {@link #clear()}, {@link #dim()} or {@link #fg(Colors)}
		 * will simply do nothing.
		 * 
		 * @param out
		 * @param colorSupport
		 */
		public TermStream(PrintStream out, boolean colorSupport) {
			this.out = out;
			this.colorSupport = colorSupport;
		}
		
		/**
		 * Prints the given character to the stream.
		 * 
		 * @param c
		 * @return this stream
		 */
		public TermStream print(char c) {
			out.print(c); return this;
		}
		/**
		 * Prints the given string to the stream.
		 * 
		 * @param s
		 * @return this stream
		 */
		public TermStream print(String s) {
			out.print(s); return this;
		}
		/**
		 * Prints the given string followed by a system line separator.
		 * 
		 * @param s
		 * @return this stream
		 */
		public TermStream println(String s) {
			out.println(s); return this;
		}
		
		/**
		 * Prints a system line separator.
		 * 
		 * @return this stream
		 */
		public TermStream newline() {
			return println("");
		}
		/**
		 * Prints the given number of spaces (' ').
		 * 
		 * @param nspaces
		 * @return this stream
		 */
		public TermStream spaces(int nspaces) {
			for (int i = 0; i < nspaces; ++i)
				print(' ');
			return this;
		}
		
		/**
		 * Reset the current text attributes to default.
		 * 
		 * @return this stream
		 */
		public TermStream clear() {
			if (colorSupport) 
				out.print(Prompt.reset());
			return this;
		}
		/**
		 * Enables the 'bold' text style.
		 * 
		 * @return this stream
		 */
		public TermStream bold() {
			if (colorSupport)
				out.print(Prompt.bold());
			return this;
		}
		/**
		 * Enables the 'dim' text style.
		 * 
		 * @return this stream
		 */
		public TermStream dim() {
			if (colorSupport)
				out.print(Prompt.dim());
			return this;
		}
		/**
		 * Enables the 'underlined' text style.
		 * 
		 * @return this stream
		 */
		public TermStream underlined() {
			if (colorSupport)
				out.print(Prompt.underlined());
			return this;
		}
		/**
		 * Enables the 'blink' text style.
		 * 
		 * @return this stream
		 */
		public TermStream blink() {
			if (colorSupport)
				out.print(Prompt.blink());
			return this;
		}
		/**
		 * Enables the 'reverse' text style.
		 * 
		 * @return this stream
		 */
		public TermStream reverse() {
			if (colorSupport)
				out.print(Prompt.reverse());
			return this;
		}
		
		/**
		 * Sets the foreground text color to {@code c}.
		 * 
		 * @param c
		 * @return this stream
		 */
		public TermStream fg(Colors c) {
			if (colorSupport)
				out.print(Prompt.fg(c));
			return this;
		}
		/**
		 * Sets the background text color to {@code c}.
		 * 
		 * @param c
		 * @return this stream
		 */
		public TermStream bg(Colors c) {
			if (colorSupport)
				out.print(Prompt.bg(c));
			return this;
		}
	}
}