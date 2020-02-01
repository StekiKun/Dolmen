package org.stekikun.dolmen.tagged;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.syntax.Extent;
import org.stekikun.dolmen.tagged.Optimiser.IdentInfo;

/**
 * A tagged lexer entry is the encoded optimised
 * version of a syntactic {@linkplain org.stekikun.dolmen.syntax.Lexer.Entry lexer entry}.
 * 
 * It contains a single {@linkplain #regexp tagged regular expression}
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
		public final Map<@NonNull String, @NonNull IdentInfo> tags;
		/** The location of the semantic action <i>per se</i> */
		public final Extent loc;
		
		/**
		 * Builds a finisher based on all the arguments
		 * 
		 * @param action
		 * @param tags
		 * @param loc
		 */
		public Finisher(int action, Map<String, IdentInfo> tags, Extent loc) {
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
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			this.append(buf);
			return buf.toString();
		}
	}
	
	/** Whether this entry is public or not */
	public final boolean visibility;
	/** The name of the lexer entry */
	public final String name;
	/** Whether the shortest match priority should be used */
	public final boolean shortest;
	/** The return type of the semantic actions for this lexer entry */
	public final Extent returnType;
	/** The list of formal arguments available in semantic actions */
	public final @Nullable Extent args;
	/** The tagged regular expression encoding all clauses */
	public final TRegular regexp;
	/** The number of tags in the encoded entry */
	public final int memTags;
	/** The list of semantic actions */
	public final List<@NonNull Finisher> actions;

	/**
	 * Builds an encoded lexer entry based on all the arguments
	 * 
	 * @param visibility
	 * @param name
	 * @param returnType
	 * @param shortest
	 * @param args
	 * @param regexp
	 * @param memTags
	 * @param actions
	 */
	public TLexerEntry(boolean visibility, String name, Extent returnType,
			boolean shortest, @Nullable Extent args,
			TRegular regexp, int memTags, List<Finisher> actions) {
		this.visibility = visibility;
		this.name = name;
		this.shortest = shortest;
		this.returnType = returnType;
		this.args = args;
		this.regexp = regexp;
		this.memTags = memTags;
		this.actions = actions;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(name);
		Extent args_ = args;
		if (args_ == null) buf.append("()");
		else buf.append("(").append(args_.find()).append(")");
		buf.append(": ").append(returnType);
		buf.append(shortest ? "[shortest]\n" : "\n");
		buf.append(" regexp: " ).append(regexp).append("\n");
		buf.append(" tags: ").append(memTags).append("\n");
		buf.append(" actions:");
		actions.forEach(fi -> { 
			buf.append("\n - ");
			fi.append(buf);
		});
		return buf.toString();
	}
}