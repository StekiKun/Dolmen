package common;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Static utility functions to interact
 * with standard input
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Prompt {

	private Prompt() {
		// Static utility only
	}

	/**
	 * @return the next line of standard input (blocking for it)
	 * 	or {@code null} if there is no such line (typically
	 *  standard input was closed)
	 */
	public static 
	@Nullable String getInputLine() {
		try {
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
}