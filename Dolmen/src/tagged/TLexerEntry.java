package tagged;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import syntax.Location;
import tagged.Optimiser.IdentInfo;

/**
 * A tagged lexer entry is the encoded optimised
 * version of a syntactic {@link syntax.Lexer.Entry lexer entry}.
 * 
 * It contains a single {@link #regexp tagged regular expression}
 * gathering all the different clauses of the original entry,
 * and describes semantic actions in terms of how bound names are
 * implemented into tags.
 * 
 * @author Stéphane Lescuyer
 */
public final class TLexerEntry {

	/**
	 * Packs together a description of one of the semantic 
	 * actions in a tagged lexer entry
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class Finisher {
		/** 
		 * The semantic action's number
		 * (in order of appearance in the syntactic clauses)
		 */
		public final int action;
		/**
		 * For every name bound in the corresponding clause,
		 * an interpretation of the identifier in terms of 
		 * tags and static offsets
		 */
		public final Map<String, IdentInfo> tags;
		/** The location of the semantic action <i>per se</i> */
		public final Location loc;
		
		/**
		 * Builds a finisher based on all the arguments
		 * 
		 * @param action
		 * @param tags
		 * @param loc
		 */
		public Finisher(int action, Map<String, IdentInfo> tags, Location loc) {
			this.action = action;
			this.tags = tags;
			this.loc = loc;
		}
		
		StringBuilder append(StringBuilder buf) {
			buf.append("" + action).append(": ");
			buf.append(tags).append(" ");
			buf.append(loc);
			return buf;
		}
		
		@SuppressWarnings("null")
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			this.append(buf);
			return buf.toString();
		}
	}
	
	/** The name of the lexer entry */
	public final String name;
	/** The tagged regular expression encoding all clauses */
	public final TRegular regexp;
	/** The number of tags in the encoded entry */
	public final int memTags;
	/** The list of semantic actions */
	public final List<Finisher> actions;

	/**
	 * Builds an encoded lexer entry based on all the arguments
	 * 
	 * @param name
	 * @param regexp
	 * @param memTags
	 * @param actions
	 */
	public TLexerEntry(String name, TRegular regexp,
				int memTags, List<Finisher> actions) {
		this.name = name;
		this.regexp = regexp;
		this.memTags = memTags;
		this.actions = actions;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(name).append(": \n");
		buf.append(" regexp: " ).append(regexp).append("\n");
		buf.append(" tags: ").append(memTags).append("\n");
		buf.append(" actions: \n");
		actions.forEach(fi -> { 
			buf.append(" - ");
			fi.append(buf);
		});
		@SuppressWarnings("null")
		@NonNull String res = buf.toString(); 
		return res;
	}
}