package org.stekikun.dolmen.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to decorate fields and methods from
 * Dolmen classes which are accessed or extended in Dolmen-generated
 * code but which should probably not be called by user-defined code
 * (i.e. semantic actions, prelude or postlude).
 * <p>
 * These methods or fields are declared as {@code protected} instead
 * of {@code private} because they must be used/accessed by
 * generated code in sub-classes, but users accessing them
 * do this at the risk of breaking the inner mechanisms of
 * their generated lexer/parser.
 * 
 * @author Stéphane Lescuyer
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DolmenInternal {

	/**
	 * @return whether read-only access from user-code is acceptable
	 */
	boolean read() default false;
}