/******************************************************************************
 * \filename IwhatNextFieldTags.java
 * Copyright (c) 2012 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief 
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext.RFC554;

/**
 * @author Thomas Ruschival - @date Feb 5, 2012
 * IwhatNextFieldTags
 */
public interface IwhatNextFieldTags {
	/**
	 * WhatNext Specific field codes
	 */
	/**
	 * Field Code for including worst case execution time estimate in vcal element
	 */
	public static final String X_WN_WCET = "X-WN-WCET:";
	/**
	 * Field Code for including actually commited work on this task
	 */
	public static final String X_WN_ACTUAL = "X-WN-ACTUAL:";
	/**
	 * Field Code for including PERC_COMPLETE in Event
	 */
	public static final String X_WN_PERC_COMPLETE = "X-WN-PERC_COMPLETE:";
	
}
