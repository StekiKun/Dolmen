package org.stekikun.dolmen.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.stekikun.dolmen.common.Maps;
import org.stekikun.dolmen.common.Prompt;

/**
 * This class describes the various command-line arguments that can be
 * used with the Dolmen CLI as an {@link Args.Item enumeration}. This enumerations
 * drives a generic command-line {@link #parse(String[]) parsing} implementation
 * which is used by the CLI in {@link Dolmen}.
 * 
 * @author Stéphane Lescuyer
 */
public final class Args {

	/**
	 * An enumeration type listing the different types
	 * of options that can be expected in the CLI.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static enum Type {
		/** A flag option, expecting no value: it is either present, or absent */
		FLAG,
		/** A numeric option, expecting an integer value */
		NUMERIC,
		/** A string option, expecting any string value */
		STRING
	}
	
	/**
	 * Common variant type holding values associated to various {@link Args.Type types}
	 * of command-line options.
	 * <p>
	 * Values can be produced using the various static factories associated to
	 * every type of option: {@link #mkFlag(boolean)}, {@link #mkNumeric(int)}
	 * and {@link #mkString(String)}.
	 * 
	 * @see #getType()
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static abstract class Value {
		/**
		 * @return the type associated to this value
		 */
		abstract Type getType();
		
		/**
		 * @return this flag value as a boolean
		 * @throws IllegalArgumentException if the value is not of type {@link Type#FLAG}
		 */
		public boolean asFlag() {
			throw new IllegalArgumentException("Value has type " + getType() + ", not FLAG");
		}
		/**
		 * @return this numeric value as an integer
		 * @throws IllegalArgumentException if the value is not of type {@link Type#NUMERIC}
		 */
		public int asNumeric() {
			throw new IllegalArgumentException("Value has type " + getType() + ", not NUMERIC");
		}
		/**
		 * @return this value as a string
		 * @throws IllegalArgumentException if the value is not of type {@link Type#STRING}
		 */
		public String asString() {
			throw new IllegalArgumentException("Value has type " + getType() + ", not STRING");
		}
		
		@Override
		public final String toString() {
			switch (getType()) {
			case FLAG:
				return asFlag() ? "true" : "false";
			case NUMERIC:
				return "" + asNumeric();
			case STRING:
				return asString();
			}
			throw new IllegalStateException("Unexpected enum constant " + getType());
		}
		
		/**
		 * @param b		whether the flag is present or not
		 * @return a flag value 
		 */
		public static Value mkFlag(boolean b) {
			return new Value() {
				@Override Type getType() { return Type.FLAG; }
				@Override public boolean asFlag() { return b; }
			};
		}
		/**
		 * @param v
		 * @return a numeric value
		 */
		public static Value mkNumeric(int v) {
			return new Value() {
				@Override Type getType() { return Type.NUMERIC; }
				@Override public int asNumeric() { return v; }
			};
		}
		/**
		 * @param s
		 * @return a string value
		 */
		public static Value mkString(String s) {
			return new Value() {
				@Override Type getType() { return Type.STRING; }
				@Override public String asString() { return s; }
			};
		}
		
		/**
		 * The value associated to an enabled flag
		 */
		public static final Value YES = mkFlag(true);
		/**
		 * The value associated to a disabled flag
		 */
		public static final Value NO = mkFlag(false);
	}
	
	// Convenient empty set of items (cannot use EnumSet here because Item is not yet initialized)
	private final static Set<Item> NONE = Collections.emptySet();
	
	/**
	 * This enumeration lists of all available options in the Dolmen command-line interface.
	 * <p>
	 * Each item describes the names by which the option can be used, the type of the associated value,
	 * a user-description, etc. It also describes its possible relations to other options, such as
	 * which options are incompatible together.
	 * <p>
	 * This enumeration has all the information required to produce a {@link Args#getUsage() usage string}
	 * and {@link Args#parse(String[]) parse the command-line arguments} while interpreting values with
	 * their expected types and checking constraints.
	 * <p>
	 * 
	 * <i>NB: It is not possible to use {@link EnumSet}s for the {@link #implies} and {@link #excludes}
	 * 	fields because enum constants are initialized when the class is loaded and enum classes
	 *  also register themselves at the same time. {@link EnumSet}s require some of this dynamic
	 *  information and cannot be used in the static initializers of the enum class itself (and therefore
	 *  not the enum constants' parameters).
	 * </i>
	 * 
	 * @author Stéphane Lescuyer
	 */
	static enum Item {
		HELP('h', "help", Type.FLAG, Value.NO,
			"display this help"),
		QUIET('q', "quiet", Type.FLAG, Value.NO,
			"suppress all output except for error report"),
		LEXER('l', "lexer", Type.FLAG, Value.NO,
			"generate a lexer from the source file"),
		PARSER('g', "grammar", Type.FLAG, Value.NO,
			"generate a parser from the source file",
			false, NONE, Collections.singleton(LEXER)),
		OUTPUT('o', "output", Type.STRING, Value.mkString("."),
			"output directory for the generated class (by default, the working directory)"),
		CLASS('c', "class", Type.STRING, Value.mkString(""),
			"name of the generated class (by default, the name of the source file)"),
		PACKAGE('p', "package", Type.STRING, Value.mkString(""),
			"name of the package for the generated class (required)",
			true, NONE, NONE),
		REPORTS('r', "reports", Type.STRING, Value.mkString(""),
			"file where potential problems should be reported (by default, the source file + '.reports')",
			false, NONE, NONE);
		
		Item(@Nullable Character shortName, String longName, Type type,
			Value deflt, @Nullable String description,
			boolean required, Set<Item> implies, Set<Item> excludes) {
			this.shortName = shortName;
			this.longName = longName;
			this.type = type;
			this.description = description;
			this.deflt = deflt;
			this.required = required;
			this.implies = implies;
			this.excludes = excludes;
			if (longName.isEmpty())
				throw new IllegalArgumentException("Illegal option name: " + longName);
			if (deflt.getType() != type)
				throw new IllegalArgumentException("Cannot use default value of type " +
						deflt.getType() + " for item of type " + type);
		}
		
		Item(@Nullable Character shortName, String longName, Type type,
			Value deflt, @Nullable String description) {
			this(shortName, longName, type, deflt, description, false, NONE, NONE);
		}
		
		/** An optional short name by which the option can be used */
		final @Nullable Character shortName;
		/** The option's long name */
		final String longName;
		/** The type of the option */
		final Type type;
		/** An optional user-friendly short description of the option */
		final @Nullable String description;
		/** A default value for the option */
		final Value deflt;
		/** Whether the option is required on the command-line */
		final boolean required;
		/** A set of other options which are required if this one is present */
		final Set<Item> implies;
		/** A set of other options which are forbidden if this one is present */
		final Set<Item> excludes;
		
		final static Map<Character, Item> shortNames;
		final static Map<String, Item> longNames;
		final static int tab;
		static {
			shortNames = new HashMap<>();
			longNames = new HashMap<>();
			int max = 0;
			
			for (Item item : Item.values()) {
				int l = item.longName.length() + 2;
				if (longNames.containsKey(item.longName))
					throw new IllegalStateException("Duplicate option long name: " + item.longName);
				longNames.put(item.longName, item);
				@Nullable Character c = item.shortName;
				if (c != null) {
					l += 4;
					if (shortNames.containsKey(item.shortName))
						throw new IllegalStateException("Duplicate option short name: " + c);
					shortNames.put(c, item);
				}
				max = Math.max(max, l);
			}
			tab = max;
		}
	}
	
	// The value associated to options parsed on the command-line (and not defaults) 
	private final EnumMap<Item, Value> options;
	// The list of standalone arguments, in their order of appearance
	private final List<String> leftover;
		
	private Args(EnumMap<Item, Value> options, List<String> leftover) {
		this.options = options;
		this.leftover = leftover;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Raw arguments: ").append(leftover).append("\n");
		for (Item item : Item.values()) {
			@Nullable Value v = Maps.get(options, item);
			buf.append(v == null ? "    " : "[*] ");
			if (v == null) v = item.deflt;
			buf.append(item).append(": ").append(v.toString()).append("\n");
		}
		return buf.toString();
	}
	
	/**
	 * @param item
	 * @return the boolean value associated to the option {@code item}
	 * @throws IllegalArgumentException if {@code item} is not of type {@link Type#FLAG}
	 */
	public boolean getFlag(Item item) {
		if (item.type != Type.FLAG) throw new IllegalArgumentException();
		Value value = options.getOrDefault(item, item.deflt);
		return value.asFlag();
	}

	/**
	 * @param item
	 * @return the numeric value associated to the option {@code item}
	 * @throws IllegalArgumentException if {@code item} is not of type {@link Type#NUMERIC}
	 */
	public int getNumeric(Item item) {
		if (item.type != Type.NUMERIC) throw new IllegalArgumentException();
		Value value = options.getOrDefault(item, item.deflt);
		return value.asNumeric();		
	}
	
	/**
	 * @param item
	 * @return the string value associated to the option {@code item}
	 * @throws IllegalArgumentException if {@code item} is not of type {@link Type#STRING}
	 */
	public String getString(Item item) {
		if (item.type != Type.STRING) throw new IllegalArgumentException();
		Value value = options.getOrDefault(item, item.deflt);
		return value.asString();		
	}
		
	/**
	 * Arguments are listed in order of appearance on the command line.
	 * 
	 * @return the list of <i>extra</i> arguments that were given on the
	 * 	command-line, i.e. arguments which were neither options nor option
	 *  values
	 */
	public List<String> getExtras() {
		return Collections.unmodifiableList(leftover);
	}
	
	/**
	 * A custom exception raised when encountering an error while
	 * parsing command-line arguments. It contains a message describing
	 * the issue.
	 * 
	 * @see Args#parse(String[])
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static final class ArgsParsingException extends Exception {
		private static final long serialVersionUID = -8459365209536721907L;
		
		ArgsParsingException(String message) {
			super(message);
		}
	}
	
	/**
	 * Parses the given array of command-line arguments {@code args} according
	 * to the various available options described in {@link Args.Item}.
	 * <p>
	 * The expected format of the command-line arguments is as follows:
	 * <ul>
	 * <li> options are introduced with a double hyphen -- followed by their 
	 * 	name, e.g. {@code --name}
	 * <li> options may be available with a one-character short name as well,
	 * 	in which case they are introduced with a single hyphen, e.g. {@code -n}
	 * <li> when an option expects a {@link Value value} of some {@link Type type},
	 * 	the value must directly follow the option in the argument array;
	 *  {@link Type#FLAG flag} options require no value
	 * <li> it is possible to use the short name notation to give more than
	 *  one option at a time, e.g. {@code -lah}, provided all given options
	 *  are flags
	 * <li> stray arguments which are not associated to an option are
	 * 	taken as is and put in an {@link Args#getExtras() extra arguments list}.
	 * </ul>
	 * 
	 * @param args
	 * @return an instance of {@link Args} which contains the result of
	 * 	the parsing of {@code args}
	 * @throws ArgsParsingException	if an error is encountered while parsing {@code args}
	 * 
	 * @see Args#getFlag(Item)
	 * @see Args#getNumeric(Item)
	 * @see Args#getString(Item)
	 * @see Args#getExtras()
	 */
	public static Args parse(String[] args) throws ArgsParsingException {
		final EnumMap<Item, Value> options = new EnumMap<>(Item.class);
		final List<String> leftover = new ArrayList<String>();
		
		// Parse all arguments from the command line
		int idx = 0;
		while (idx < args.length) {
			String arg = args[idx];
			if (arg.charAt(0) == '-') {
				// Parsing an option name
				Item item;
				if (arg.length() >= 2 && arg.charAt(1) == '-') {
					// Potentially a long option
					String opt = arg.substring(2);
					if (opt.isEmpty())
						throw new ArgsParsingException("Invalid empty option name");
					@Nullable Item item_ = Item.longNames.get(opt);
					if (item_ == null)
						throw new ArgsParsingException("Unknown option name: " + opt);
					item = item_;
				}
				else {
					String opt = arg.substring(1);
					if (opt.isEmpty())
						throw new ArgsParsingException("Invalid empty option name");
					// If there are more than one short name, they must all be flags
					if (opt.length() > 1) {
						for (int i = 0; i < opt.length(); ++i) {
							char c = opt.charAt(i);
							@Nullable Item item_ = Item.shortNames.get(c);
							if (item_ == null) {
								String msg = "Unknown option name: " + c;
								if (Item.longNames.containsKey(opt))
									msg += ". Did you mean --" + opt + "?";
								throw new ArgsParsingException(msg);
							}
							if (item_.type != Type.FLAG)
								throw new ArgsParsingException("Option " + c + " is not a flag");
							options.put(item_, Value.YES);
						}
						++idx; continue;
					}
					@Nullable Item item_ = Item.shortNames.get(opt.charAt(0));
					if (item_ == null)
						throw new ArgsParsingException("Unknown option name: " + opt);
					item = item_;
				}
				
				// Look for the next associated argument, if any
				if (item.type == Type.FLAG) {
					options.put(item, Value.YES);
					++idx; continue;
				}
				++idx;
				@Nullable String aarg;
				if (idx == args.length)
					aarg = null;
				else
					aarg = args[idx];
				if (aarg == null || aarg.charAt(0) == '-')
					throw new ArgsParsingException("Missing argument for option " + arg);
				
				Value v;
				if (item.type == Type.NUMERIC) {
					try {
						v = Value.mkNumeric(Integer.parseInt(aarg));
					}
					catch (NumberFormatException e) {
						throw new ArgsParsingException("Expected numeric argument for option " + arg);
					}
				}
				else
					v = Value.mkString(aarg);
				options.put(item, v);
				++idx; continue;
			}
			else {
				// Standalone argument
				leftover.add(arg);
				++idx; continue;
			}
		}
		
		// Check that all constraints expressed in Item are met
		for (Item item : Item.values()) {
			if (options.containsKey(item)) {
				for (Item req : item.implies) {
					if (!options.containsKey(req))
						throw new ArgsParsingException("Option --" + req.longName + " is required because of --"
								+ item.longName + ", but is missing");
				}
				for (Item excl : item.excludes) {
					if (options.containsKey(excl))
						throw new ArgsParsingException(
								"Options --" + item.longName + " and --" + excl.longName + " are incompatible");
				}
			}
			else if (item.required)
				throw new ArgsParsingException("Option --" + item.longName + " is required and missing");
		}
		
		return new Args(options, leftover);
	}

	/**
	 * @return a <i>usage</i> string derived from the various options described in {@link Item},
	 * 	suitable for presentation to users
	 */
	static String getUsage() {
		StringBuilder buf = new StringBuilder();
		buf.append("java -jar Dolmen.jar <options> source\n");
		buf.append("\n");
		buf.append("Dolmen will generate a lexical or syntactic analyzer based on the description given in the 'source' file.\n");
		buf.append("Options:\n");
		for (Item item : Item.values()) {
			buf.append(" ");
			int k = 4;
			if (item.shortName != null) {
				buf.append("-").append(item.shortName).append(", ");
			}
			else buf.append("    ");
			buf.append("--").append(item.longName);
			k += 2 + item.longName.length();
			for (int i = Item.tab + 4 - k; i >= 0; --i)
				buf.append(" ");
			if (item.description != null)
				buf.append(item.description);
			buf.append("\n");
		}
		return buf.toString();
	}
	
	/**
	 * An entry point for testing argument parsing only
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(getUsage());
		
		String prompt;
		while ((prompt = Prompt.getInputLine(">")) != null) {
			String[] splits = prompt.split("\\s+");
			try {
				Args aargs = parse(splits);
				System.out.println(aargs);
			} catch (ArgsParsingException e) {
				System.out.println(e.getMessage());
				System.out.println(getUsage());
			}
			
		}
	}
}