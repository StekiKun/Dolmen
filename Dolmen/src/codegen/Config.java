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
import syntax.Option;
import syntax.Reporter;

/**
 * Configuration description for {@link GrammarOutput}
 * <p>
 * Options can be configured in the grammar description
 * using pairs of key-value descriptions. The set of
 * supported keys is described in the enumeration {@link Keys}.
 * <p>
 * A {@link #DEFAULT} configuration is available.
 * 
 * @author Stéphane Lescuyer
 * @see #ofOptions(List, Reporter)
 */
public final class Config {
	
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
		Positions("positions", Keys::asBoolean);
		
		/** Name of the key */
		public final String key;
		/** Parser function for the option value */
		public final Function<String, Object> parser;
		
		private Keys(String key, Function<String, Object> parser) {
			this.key = key;
			this.parser = parser;
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
	}
	
	/** 
	 * Whether the parser should keep positions for
	 * non-terminal symbols as well as terminal symbols	
	 */
	public final boolean positions;
	
	/**
	 * Builds a default configuration
	 */
	private Config() {
		this(new EnumMap<>(Keys.class));
	}
	
	/** The default configuration for grammar generation */
	public static final Config DEFAULT = new Config();
	
	/**
	 * Builds a configuration for {@link GrammarOutput} from
	 * the given options. Not all options need be configured
	 * in {@code options}, those that are unspecified will
	 * take on default values.
	 * 
	 * @param options
	 * @throws ClassCastException if some options are
	 * 	associated to incompatible value types
	 */
	public Config(EnumMap<Keys, @NonNull Object> options) {
		this.positions =
			(boolean) options.getOrDefault(Keys.Positions, false);
	}
	
	/**
	 * This method never fails to build a valid configuration, but 
	 * can report unexpected things such as illegal keys or values 
	 * through the reporter. Only <i>warnings</i> are reported.
	 * 
	 * @param options
	 * @param reporter
	 * @return the grammar configuration that is implied
	 * 	by the option settings given in {@code options}
	 */
	public static Config ofOptions(List<Option> options, Reporter reporter) {
		EnumMap<Keys, Object> indexedOptions = new EnumMap<>(Keys.class);
		
		for (Option option : options) {
			// Check that the option key is recognized
			final String keyName = option.key.val;
			final @Nullable Keys key = Keys.fromName.get(keyName);
			if (key == null) {
				reporter.add(Reports.unknownOption(option));
				continue;
			}
			// Check that it is not set multiple times
			if (indexedOptions.containsKey(key)) {
				reporter.add(Reports.duplicateOption(option));
				continue;
			}
			// Parse the associated value
			Object value;
			try {
				value = key.parser.apply(option.value.val);
			}
			catch (IllegalArgumentException e) {
				reporter.add(Reports.illformedValue(option, e));
				continue;
			}
			indexedOptions.put(key, value);
		}

		return new Config(indexedOptions);
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
			String msg = String.format("Unknown option \"%s\", this setting will be ignored", option.key.val);
			return IReport.of(msg, Severity.WARNING, option.key);
		}
		
		static IReport illformedValue(Option option, IllegalArgumentException e) {
			String msg = String.format("Illegal value for option "
					+ "\"%s\" this setting will be ignored: %s", 
					option.key.val, e.getMessage());
			return IReport.of(msg, Severity.WARNING, option.value);
		}

	}
}