/******************************************************************************
 * \filename TimeConversion.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 * 
 * \brief Utility class for time calculation
 * 
 ******************************************************************************/

package de.ruschival.WhatNext;

import android.content.Context;

public class TimeConversion {
	/**
	 * Constant to convert from ms (long) to Minutes
	 */
	public static final long MS_TO_MIN = 60 * 1000;

	/**
	 * Constant to convert from ms (long) to hours
	 */
	public static final long MS_TO_H = 60 * MS_TO_MIN;

	/**
	 * Constant to convert from ms (long) to days
	 */
	public static final long MS_TO_DAY = 24 * MS_TO_H;

	/**
	 * Constant to convert from ms (long) to weeks
	 */
	public static final long MS_TO_WEEK = 7 * MS_TO_DAY;

	/**
	 * Constant to convert from ms (long) to months
	 */
	public static final long MS_TO_MONTH = 30 * MS_TO_DAY;

	/**
	 * Array representation for easy lookup of constants/positions in strings.xml
	 */
	public static final long[] TIMEUNIT_CONSTANTS = { MS_TO_MIN, MS_TO_H, MS_TO_DAY,
			MS_TO_WEEK, MS_TO_MONTH };
	
	/**
	 * Array for universal time unit representations
	 */
	public static final String[] TIME_SI_UNITS = {"min", "h", "d", "w", "m"};

	/**
	 * Determine the maximum time unit that is capable of representing the given number as
	 * integer
	 * 
	 * @param time
	 *            delta in ms
	 * @return timeUnit see constants and TimeUnits in strings.xml
	 */
	public static int getUnitIndex(long time) {
		if (time == 0) {
			return 0;
		}
		for (int i = TIMEUNIT_CONSTANTS.length - 1; i >= 0; i--) {
			if (time % TIMEUNIT_CONSTANTS[i] == 0) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Return the localized representation of the maximum time unit capable of representing the
	 * given time as integer
	 * 
	 * @param time
	 *            delta in ms
	 * @param ctx
	 *            context of application for access of
	 * @return
	 */
	public static String getUnitString(long time, Context ctx) {
		String unit = new String();
		int unitIndex = getUnitIndex(time);
		unit = ctx.getResources().getStringArray(R.array.TimeUnits)[unitIndex];
		return unit;
	}

	/**
	 * Return the time divided by the appropriate time unit as string
	 * 
	 * @param time delta in ms
	 * @return
	 */
	public static String getTimeString(long time) {
		int unitIndex = getUnitIndex(time);
		String timeS = Long.toString(time / TIMEUNIT_CONSTANTS[unitIndex]);
		return timeS;
	}
	
	/**
	 * Return a short string/symbol for the time unit. E.g. (s, min, h...)
	 * @param time delta in ms
	 * @return
	 */
	public static String getSiUnit(long time){
		int unitIndex = getUnitIndex(time);
		return TIME_SI_UNITS[unitIndex];
	}
	
	/**
	 * Build an approximate (rounded) timestring with Si unit, 
	 * with parenthesis e.g. 92 min -> (+1h)
     * @param time  delta in ms
	 * @return
	 */
	public static String getApproximateString(long time){
		int unit=0;
		String result = new String("(");
		if(time > 0){
			result += ">";
		}
		
		for (unit = TIMEUNIT_CONSTANTS.length - 1; unit >= 0; unit--) {
			if (Math.abs(time) > TIMEUNIT_CONSTANTS[unit]) {
				break;
			}
		}
		result+=Long.toString(time/TIMEUNIT_CONSTANTS[unit])+TIME_SI_UNITS[unit]+")";
		return result;
	}
}
