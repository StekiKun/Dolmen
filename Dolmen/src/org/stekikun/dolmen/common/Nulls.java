package org.stekikun.dolmen.common;

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
	
	/**
	 * <i> This is designed to "cast" an array of {@link Nullable} 
	 *  values to an array of {@link NonNull} values. It is necessary
	 *  because except in the case of array initializers, it is often
	 *  not possible to initialize all the cells in an array at once, 
	 *  and thus impossible to declare them as containing non-null values
	 *  from the start. This method can be used once all cells are known
	 *  to have been initialized to non-null values. <b>It does not perform
	 *  any dynamic checks and therefore is potentially type unsafe.</b>
	 *  
	 *  It is advised that every call to this method be <b>justified
	 *  with some suitable comment</b>.
	 * </i>
	 * 
	 * @param a
	 * @return {@code a} with a stronger type <i>without run-time checks</i> 
	 */
	@SuppressWarnings("null")
	public static <T> @NonNull T[] arrayOk(T @NonNull[] a) {
		return a;
	}
}
