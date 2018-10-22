package common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Convenience functions about the Java syntax 
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Java {

	/**
	 * An array of all keywords of the Java language (JLS 8)
	 * 
	 * <i>NB: This includes the reserved literals {@code false}, {@code true}
	 * 	and {@code null}, which are not technically keywords.</i>
	 */
	public static final @NonNull String[] KEYWORDS = {
		"false", "null", "true",	// technically reserved literals, not keywords
		"abstract", "continue", "for", "new", "switch",
		"assert", "default", "if", "package", "synchronized",
		"boolean", "do", "goto", "private", "this",
		"break", "double", "implements", "protected", "throw",
		"byte", "else", "import", "public", "throws",
		"case", "enum", "instanceof", "return", "transient",
	    "catch", "extends", "int", "short", "try",
	    "char", "final", "interface", "static", "void",
	    "class", "finally", "long", "strictfp", "volatile",
	    "const", "float", "native", "super", "while"
	};

	/**
	 * A set of all keywords of the Java language (JLS 8)
	 * 
	 * <i>NB: This includes the reserved literals {@code false}, {@code true}
	 * 	and {@code null}, which are not technically keywords.</i>
	 */
	public static final Set<String> keywordSet;
	static {
		Set<String> kws = new HashSet<>();
		for (String kw : KEYWORDS)
			kws.add(kw);
		keywordSet = Collections.unmodifiableSet(kws);
	}
	
}
