package common;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class contains various utility about null
 * or non-null values, which can be convenient in
 * a project that uses the annotation-based null-analysis.
 * 
 * @author Stéphane Lescuyer
 */
public abstract class Nulls {

	private Nulls() {
		// Static utility only
	}
	
	/**
	 * <i>This is designed to "cast" a {@link Nullable} value
	 * to a {@link NonNull} one in contexts where the type is
	 * not strong enough but the programmer is sure enough (e.g.
	 * a call to {@link Map#get} after a check with {@link Map#containsKey}).
	 * It checks the input for {@code null} anyway and the case
	 * being throws an {@link NullPointerException}, acting as
	 * a <b>dynamically-checked cast</b>.
	 * It is advised that every call to this method be <b>justified
	 * with some suitable comment</b>.
	 * </i>
	 * 
	 * @param v
	 * @return {@code v} making sure it is non-null
	 * @throws NullPointerException if {@code v} is null
	 */
	public static <T> @NonNull T ok(T v) {
		if (v == null) throw new NullPointerException();
		return v;
	}
}
