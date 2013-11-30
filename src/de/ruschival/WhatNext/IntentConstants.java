/******************************************************************************
 * \filename IntentConstants.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 * 
 * \brief Constants for Intent action and return codes gobal to WhatNext
 * 
 ******************************************************************************/

package de.ruschival.WhatNext;

/**
 * @author ruschi
 *
 */
public interface IntentConstants {
	/**
	 * Intent Codes
	 */
	/**
	 * Intent Code to TaskEdit for creating a new task
	 */
	public static final int ITC_NEW_TASK  =  0x8001;
	/**
	 * Intent Code to TaskEdit for editing task
	 */
	public static final int ITC_EDIT_TASK =  0x8002;
	/**
	 * Intent Code for PauseDialog Activity stopping a task
	 */
	public static final int ITC_PAUSE_TASK = 0x8003;
	/**
	 * Intent Code for Starting a Task 
	 */
	public static final int ITC_START_TASK = 0x8004;
}
