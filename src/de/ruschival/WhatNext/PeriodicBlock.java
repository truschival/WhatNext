/******************************************************************************
 * \filename PeriodicBlock.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 *
 * SPDX-License-Identifier:      GPL-2.0+
 *
 * \brief Class for periodically reoccuring events blocking alarms
 * 
 ******************************************************************************/

package de.ruschival.WhatNext;

import java.util.HashSet;

/**
 * @author Thomas Ruschival
 * 
 * PeriodicBlock allows definition of (global) periodic time intervals in which
 *        Alarms are suppressed.
 *        If Periodic Blocks overlap only the Alarms of tasks in the alarmCategories of the
 *        periodic block with the highest order are shown.
 */
public class PeriodicBlock {
	/**
	 * first start of this timeslot in ms since epoch
	 */
	private long start = 0;
	/**
	 * Duration in ms
	 */
	private int duration = 0;

	/**
	 * Period for recurring events in ms relative to start
	 */
	private int period = 0;

	/**
	 * Order of Block if overlap occurs. Determines which alarms are suppressed
	 */
	private int order = 0;

	/**
	 * Set of categories for which alarm may occur within this Periodic Block
	 */
	private HashSet<Category> alarmCategories;

	/**
	 * Constructor for a periodically reoccuring block
	 * 
	 * @param start
	 *            first start of this timeslot in ms since epoch
	 * @param duration
	 *            Duration in ms
	 * @param period
	 *            Period for recurring events in ms relative to start
	 * @param order
	 *            Order of Block if overlap occurs.
	 */
	public PeriodicBlock(long start, int duration, int period, int order) {
		this.start = start;
		this.duration = duration;
		this.period = period;
		this.order = order;
		alarmCategories = new HashSet<Category>();
	}

	/**
	 * @return the start
	 */
	public long getStart() {
		return start;
	}

	/**
	 * @param start
	 *            the start to set
	 */
	public void setStart(long start) {
		this.start = start;
	}

	/**
	 * @return the duration
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * @param duration
	 *            the duration to set
	 */
	public void setDuration(int duration) {
		this.duration = duration;
	}

	/**
	 * @return the period
	 */
	public int getPeriod() {
		return period;
	}

	/**
	 * @param period
	 *            the period to set
	 */
	public void setPeriod(int period) {
		this.period = period;
	}

	/**
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * @param order
	 *            the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * @return the alarmCategories
	 */
	public HashSet<Category> getAlarmCategories() {
		return alarmCategories;
	}

	/**
	 * Calculate the number of elapsed periods for this event
	 * 
	 * @param now
	 * @return the nPeriods
	 */
	public int getPeriodsElapsed(long now) {
		return (int) ((now - start) / period);
	}

	/**
	 * Calculates the most proximate reoccurence of this Block
	 * 
	 * @param now
	 * @return time in ms for next start
	 */
	public long getComingStart(long now) {
		if (start < now) {
			return start;
		}
		int nPeriods = getPeriodsElapsed(now) + 1;
		return start + nPeriods * period;
	}

	/**
	 * Calculates the most proximate end of this Block
	 * 
	 * @param now
	 * @return time in ms for next end
	 */
	public long getComingEnd(long now) {
		int nPeriods = getPeriodsElapsed(now);
		if (!isInBlock(now)) {
			nPeriods += 1;
		}
		return (start + (nPeriods * period) + duration);
	}

	/**
	 * Checks if current time now is within a reoccurence of this Block
	 * 
	 * @param now
	 * @return true if now is within the bounds of a reoccurence
	 */
	public boolean isInBlock(long now) {
		int nPeriods = getPeriodsElapsed(now);
		if ((start + (nPeriods * period)) < now
				&& now <= (start + (nPeriods * period) + duration)) {
			return true;
		}
		return false;
	}

}