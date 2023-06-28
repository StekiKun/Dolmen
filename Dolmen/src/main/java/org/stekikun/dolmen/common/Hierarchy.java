package org.stekikun.dolmen.common;

/**
 * Used to annotate base classes of hierarchy
 * for the "Create hierarchy switch" quick-assist
 * 
 * @author Stéphane Lescuyer
 */
public @interface Hierarchy {

	/**
	 * @return the name of the method that returns
	 * 	the enum value describing what case of
	 *  a hierarchy an instance belongs to
	 */
	String value();
}
