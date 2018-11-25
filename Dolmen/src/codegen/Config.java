package codegen;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import syntax.IReport;
import syntax.IReport.Severity;
import unparam.Grammar;
import syntax.Lexer;
import syntax.Option;
import syntax.PGrammar;
import syntax.Reporter;

/**
 * Configuration description for Dolmen generation of
 * both lexical and syntax analyzers.
 * <p>
 * Options can be configured in the grammar description
 * using pairs of key-value descriptions. The set of
 * supported keys is described in the enumeration {@link Keys}.
 * Configuration instances can then be built from such a list
 * of key-value bindings 
 * (see {@link #ofOptions(Relevance, List, Reporter)}).
 * <p>
 * Each option can be relevant to either solely lexer generation
 * or parser generation, or to both.
 * <p>
 * For better declarative support, a configuration instance can
 * also be built using a builder class with chained methods
 * for each configurable option.
 * <p>
 * Finally, a {@link #DEFAULT} configuration is available.
 * 
 * @author Stéphane Lescuyer
 * @see #ofOptions(Relevance, List, Reporter)
 */
public final class Config {
	
	/**
	 * Enumeration representing the relevance of a
	 * configuration: it can be usable only within a lexer
	 * description, a parser description, or both.
	 * <p>
	 * Relevance values form a semi-lattice with {@link #BOTH}
	 * as a bottom element for the {@link #includes(Relevance)}
	 * relation.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public static enum Relevance {
		/** Only relevant to generation of lexical analyzers */
		LEXER,
		/** Only relevant to generation of syntax analyzers */
		PARSER,
		/** Relevant for both lexical and syntax analyzers */
		BOTH;
		
		/**
		 * @param r
		 * @return whether this relevance value is more
		 * 	permissive than {@code r}, i.e. whether all
		 * 	options relevant in {@code r} are also relevant
		 * 	in {@code this}
		 */
		public boolean includes(Relevance r) {
			switch (this) {
			case BOTH:
				return true;
			case LEXER:
				return r == LEXER;
			case PARSER:
				return r == PARSER;
			}
			return false;
		}
	}
	
	/**
	 * Enumeration of the keys for configurable options.
	 * <p>
	 * Each option is characterized by its {@linkplain #key key name},
	 * which is the name that can be used in grammar descriptions to
	 * specify the option's value, and by a {@linkplain #parser parser function}
	 * used to interpret the associated string value. The parser function
	 * must either return an object of a type suitable for the option,
	 * or must throw an {@link IllegalArgumentException}.
	 * 
	 * @author Stéphane Lescuyer
	 */
	@SuppressWarnings("javadoc")
	public static enum Keys {
		Positions(Relevance.PARSER, "positions", false, Keys::asBoolean),
		TokenAnnotations(Relevance.PARSER, "token_annotations", "@SuppressWarnings(\"javadoc\")", Keys::asString),
		ClassAnnotations(Relevance.BOTH, "class_annotations", "", Keys::asString);
		
		/** Relevance of the option associated to that key */
		public final Relevance relevance;
		/** Name of the key */
		public final String key;
		/** Default value of the option associated to that key */
		public final Object defaultValue;
		/** Parser function for the option value */
		public final Function<String, Object> parser;
		
		private <@NonNull B> Keys(Relevance relevance, 
				String key, B defaultValue, Function<String, B> parser) {
			this.relevance = relevance;
			this.key = key;
			this.defaultValue = defaultValue;
			this.parser = (s) -> parser.apply(s); // η-expansion for type weakening 
		}
		
		/**
		 * @param options
		 * @return the value set for this key in {@code options},
		 * 	or the {@linkplain #defaultValue default value} if
		 *  the key is not bound in {@code options}
		 */
		Object from(EnumMap<Keys, Object> options) {
			return options.getOrDefault(this, this.defaultValue);
		}
		
		public static final Map<String, Keys> fromName;
		static {
			fromName = new HashMap<String, Keys>();
			for (Keys k : Keys.values()) {
				fromName.put(k.key, k);
			}
		}
		
		private static Boolean asBoolean(String s) {
			if ("true".equals(s)) return true;
			else if ("false".equals(s)) return false;
			throw new IllegalArgumentException("expected boolean value 'true' or 'false'");
		}
		
		private static String asString(String s) {
			return s;
		}
	}
	
	/** 
	 * Whether the parser should keep positions for
	 * non-terminal symbols as well as terminal symbols	
	 */
	public final boolean positions;
	
	/**
	 * Annotations which must be added to the generated
	 * parser or lexer class
	 */
	public final String classAnnotations;

	/**
	 * Annotations which must be added to the generated
	 * token class in a generated parser
	 */
	public final String tokenAnnotations;

	/**
	 * Builds a default configuration
	 */
	private Config() {
		this(new EnumMap<>(Keys.class));
	}
	
	/** The default configuration for grammar generation */
	public static final Config DEFAULT = new Config();
	
	/**
	 * Builds a configuration from the given options. 
	 * Not all options need be configured in {@code options}, 
	 * those that are unspecified will take on default values.
	 * 
	 * @param options
	 * @throws ClassCastException if some options are
	 * 	associated to incompatible value types
	 */
	public Config(EnumMap<Keys, @NonNull Object> options) {
		this.positions = (boolean) Keys.Positions.from(options);
		this.classAnnotations = (String) Keys.ClassAnnotations.from(options);
		this.tokenAnnotations = (String) Keys.TokenAnnotations.from(options);
	}
	
	/**
	 * This method never fails to build a valid configuration, but 
	 * can report unexpected things such as illegal keys or values 
	 * through the reporter. Only <i>warnings</i> are reported.
	 * 
	 * @param relevance	context where the configuration should be relevant
	 * @param options
	 * @param reporter	can be null if no reporting is required
	 * @return the configuration that is implied
	 * 	by the option settings given in {@code options}
	 */
	public static Config ofOptions(Relevance relevance, 
			List<Option> options, @Nullable Reporter reporter) {
		EnumMap<Keys, Object> indexedOptions = new EnumMap<>(Keys.class);
		
		for (Option option : options) {
			// Check that the option key is recognized
			final String keyName = option.key.val;
			final @Nullable Keys key = Keys.fromName.get(keyName);
			if (key == null) {
				if (reporter != null)
					reporter.add(Reports.unknownOption(option));
				continue;
			}
			// Check that the option is relevant here
			if (!key.relevance.includes(relevance)) {
				if (reporter != null)
					reporter.add(Reports.unknownOption(option));
				continue;
			}
			// Check that it is not set multiple times
			if (indexedOptions.containsKey(key)) {
				if (reporter != null)
					reporter.add(Reports.duplicateOption(option));
				continue;
			}
			// Parse the associated value
			Object value;
			try {
				value = key.parser.apply(option.value.val);
			}
			catch (IllegalArgumentException e) {
				if (reporter != null)
					reporter.add(Reports.illformedValue(option, e));
				continue;
			}
			indexedOptions.put(key, value);
		}

		return new Config(indexedOptions);
	}

	/**
	 * Same as {@link #ofOptions(Relevance, List, Reporter) 
	 * 	ofOptions(Relevance.PARSER, grammar.options, reporter)}.
	 * 
	 * @param grammar
	 * @param reporter
	 * @return the configuration that is implied from the options
	 * 	in {@code grammar}
	 */
	public static Config ofPGrammar(PGrammar grammar, @Nullable Reporter reporter) {
		return ofOptions(Relevance.PARSER, grammar.options, reporter);
	}
	/**
	 * Same as {@link #ofOptions(Relevance, List, Reporter) 
	 * 	ofOptions(Relevance.PARSER, grammar.options, reporter)}.
	 * 
	 * @param grammar
	 * @param reporter
	 * @return the configuration that is implied from the options
	 * 	in {@code grammar}
	 */
	public static Config ofGrammar(Grammar grammar, @Nullable Reporter reporter) {
		return ofOptions(Relevance.PARSER, grammar.options, reporter);
	}
	/**
	 * Same as {@link #ofOptions(Relevance, List, Reporter) 
	 * 	ofOptions(Relevance.LEXER, grammar.options, reporter)}.
	 * 
	 * @param lexer
	 * @param reporter
	 * @return the configuration that is implied from the options
	 * 	in {@code lexer}
	 */
	public static Config ofLexer(Lexer lexer, @Nullable Reporter reporter) {
		return ofOptions(Relevance.LEXER, lexer.options, reporter);
	}
	
	/**
	 * Static utility class to build the various problem reports
	 * that can arise in building an instance of {@link Config}
	 * 
	 * @author Stéphane Lescuyer
	 */
	private static abstract class Reports {

		static IReport duplicateOption(Option option) {
			String msg = String.format("Option \"%s\" is already set, "
					+ "this setting will be ignored", option.key.val);
			return IReport.of(msg, Severity.WARNING, option.key);
		}
		
		static IReport unknownOption(Option option) {
			String msg = String.format(
				"Unknown option \"%s\", this setting will be ignored", option.key.val);
			return IReport.of(msg, Severity.WARNING, option.key);
		}
		
		static IReport illformedValue(Option option, IllegalArgumentException e) {
			String msg = String.format("Illegal value for option "
					+ "\"%s\" this setting will be ignored: %s", 
					option.key.val, e.getMessage());
			return IReport.of(msg, Severity.WARNING, option.value);
		}

	}
	
	/**
	 * @return a fresh configuration builder,
	 * 	initialized with default values for all options
	 */
	public static Config.Builder start() {
		return new Config.Builder();
	}
	
	/**
	 * A <i>builder</i> class for {@linkplain Config configurations},
	 * which allows setting all options using method chaining.
	 * 
	 * @author Stéphane Lescuyer
	 */
	public final static class Builder {
		private final EnumMap<Keys, Object> options;
		
		Builder() {
			this.options = new EnumMap<>(Keys.class);
		}
		
		/**
		 * @see Keys#Positions
		 * @param b
		 * @return {@code this}
		 */
		public Builder positions(boolean b) {
			options.put(Keys.Positions, b);
			return this;
		}
		
		/**
		 * @see Keys#ClassAnnotations
		 * @param s
		 * @return {@code this}
		 */
		public Builder classAnnotations(String s) {
			options.put(Keys.ClassAnnotations, s);
			return this;
		}
		
		/**
		 * @see Keys#TokenAnnotations
		 * @param s
		 * @return {@code this}
		 */
		public Builder tokenAnnotations(String s) {
			options.put(Keys.TokenAnnotations, s);
			return this;
		}
		
		/**
		 * @return the configuration from this builder's state
		 */
		public Config done() {
			return new Config(options);
		}
	}
}