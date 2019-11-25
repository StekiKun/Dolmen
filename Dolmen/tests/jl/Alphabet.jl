[ class_annotations = "@SuppressWarnings(\"null\")" ]

import org.eclipse.jdt.annotation.Nullable;

import java.io.FileReader;
import java.io.BufferedReader;

{ }

ident = ['A'-'Z']+;
nl = '\n' | '\r' | "\r\n";
not_nl = [^'\n''\r']+;

public { @Nullable String } rule main =
| ident nl
	{ return getLexeme(); }
| "#include" ' '* (not_nl as filename) nl
	{
		BufferedReader reader;
		try { reader = new BufferedReader(new FileReader(filename)); }
		catch (java.io.IOException e) {
			throw error("Cannot include file " + filename + ": " + e.getMessage());
		}
		pushInput(filename, reader);
		continue main;
	}
| "#switchto" ' '* (not_nl as filename) nl
	{
		BufferedReader reader;
		try { reader = new BufferedReader(new FileReader(filename)); }
		catch (java.io.IOException e) {
			throw error("Cannot switch to file " + filename + ": " + e.getMessage());
		}
		changeInput(filename, reader);
		continue main;
	}
| eof
	{
		if (hasMoreInput()) {
			popInput(); continue main;
		}
		else
			return null;
	}

{ }