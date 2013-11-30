/******************************************************************************
 * \filename ICSExporter.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief 
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext.RFC554;

import java.util.ArrayList;
import java.util.Calendar;
import de.ruschival.WhatNext.*;
import android.text.format.DateFormat;

/**
 * @author Thomas Ruschival ICSExporter
 */
public class VcalConverter implements IvcalFieldTags, IwhatNextFieldTags {

	/**
	 * Export a complete ical/VCAL String with headers where the current task is
	 * included as VEVENT + VTODO item
	 * 
	 * @param task
	 * @return String representation of Task
	 */
	public static String getTaskiCal(Task task) {
		StringBuilder icalBuilder = new StringBuilder(1024);
		icalBuilder.append(VCAL_BEGIN);
		icalBuilder.append(VCAL_VERSION);
		icalBuilder.append(VCAL_PRODID);
		generateVevent(task, icalBuilder);
		generateVtodo(task, icalBuilder);
		icalBuilder.append(VCAL_END);
		icalBuilder.trimToSize();
		return icalBuilder.toString();
	}

	/**
	 * Export a complete ical/VCAL String with headers where the current task is
	 * included as VEVENT item
	 * 
	 * @param task
	 * @return String representation of Task
	 */
	public static String getTaskAsVevent(Task task) {
		StringBuilder icalBuilder = new StringBuilder(1024);
		icalBuilder.append(VCAL_BEGIN);
		icalBuilder.append(VCAL_VERSION);
		icalBuilder.append(VCAL_PRODID);
		generateVevent(task, icalBuilder);
		icalBuilder.append(VCAL_END);
		icalBuilder.trimToSize();
		return icalBuilder.toString();
	}
	
	
	/**
	 * Export a complete ical/VCAL String with headers where the current task is
	 * included as VTODO item
	 * 
	 * @param task
	 * @return String representation of Task
	 */
	public static String getTaskAsVtodo(Task task) {
		StringBuilder icalBuilder = new StringBuilder(1024);
		icalBuilder.append(VCAL_BEGIN);
		icalBuilder.append(VCAL_VERSION);
		icalBuilder.append(VCAL_PRODID);
		generateVtodo(task, icalBuilder);
		icalBuilder.append(VCAL_END);
		icalBuilder.trimToSize();
		return icalBuilder.toString();
	}

	/**
	 * Format a Calendar into a RFC5545 Date Representation yyyyMMddThhmmssZ
	 * 
	 * @param cal
	 * @return Formatted String
	 */
	public static String formatRFC5545DateTime(Calendar cal) {
		String retval = (String) DateFormat.format("yyyyMMdd", cal);
		retval += "T" + DateFormat.format("kkmmss", cal) + "Z";
		return retval;
	}

	/**
	 * Map the different task States to a simpler model of RFC5545 states
	 * 
	 * @param state
	 * @return string representation constant
	 */
	public static String convertTaskState(Task.State state) {
		String result = STATUS_NEEDS_ACTION;
		switch (state) {
		case READY:
		case FUTURE:
		case OVERDUE:
			result = STATUS_NEEDS_ACTION;
			break;
		case DONE:
			result = STATUS_COMPLETE;
			break;
		case RUNNING:
		case RUNNING_OVERDUE:
			result = STATUS_IN_PROGRESS;
		}
		return result;
	}

	/**
	 * Build a RFC5545 VEVENT entry from task information does not take parent or
	 * children into account
	 * 
	 * @return VEVENT entry as string
	 */
	public static void generateVevent(Task task, StringBuilder builder) {
		builder.append(VEVENT_BEGIN);
		/* NOTE: DTSTAMP or UID not both! */
		builder.append(UID).append(buildUID(task,"EVT")).append("\n");
		builder.append(DTEND).append(formatRFC5545DateTime(task.getDue())).append("\n");
		addCommonFields(task, builder);
		appendAlarms(task, builder);
		builder.append(VEVENT_TRANSP).append("TRANSPARENT\n");
		builder.append(VEVENT_END);
		builder.trimToSize();
	}

	public static void addCommonFields(Task task, StringBuilder builder) {
		/** StartDateTime */
		builder.append(START).append(formatRFC5545DateTime(task.getStart())).append("\n");
		/** Put our CREATED here */
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(task.dtstamp);
		builder.append(CREATED).append(formatRFC5545DateTime(cal)).append("\n");
		/** synchronization time stamp */
		cal.setTimeInMillis(task.lastmodified);
		builder.append(LAST_MOD).append(formatRFC5545DateTime(cal)).append("\n");
		/** TaskName = Summary */
		builder.append(SUMMARY).append(task.name).append("\n");

		/** Task Description */
		builder.append(DESCRIPTION).append(task.desc).append("\n");

		/** Location field */
		builder.append(LOCATION).append(task.location).append("\n");
		/** Geo-coordinates */
		if (task.geoloc != null) {
			builder.append(GEO_LOC).append("xx").append("\n");
		}
		/** URI for more information */
		if (task.uri != null) {
			builder.append(URI).append(task.uri).append("\n");
		}

		/** Priority as INTEGER ! 0 corresponds to no Priority thus +1 */
		builder.append(PRIORITY).append(task.getPriorityOrdinal() + 1).append("\n");
		/*
		 * The following are OPTIONAL, and MAY occur more than once.
		 */

		/** CATEGORIES */
		builder.append(CATEGORIES).append(task.getCategoriesString()).append("\n");

		/** WCET */
		builder.append(X_WN_WCET).append(task.getWcet()).append("\n");
		/** ACTUAL */
		builder.append(X_WN_ACTUAL).append(task.getActual()).append("\n");
	}

	/**
	 * Append individual reminders as VALARM element to the respective top-level element
	 * 
	 * @param task
	 * @param alarmsBuilder
	 */
	public static void appendAlarms(Task task, StringBuilder alarmsBuilder) {
		ArrayList<Reminder> alarms = task.getReminders();
		for (Reminder reminder : alarms) {
			alarmsBuilder.append(VALARM_BEGIN);
			String delta = TimeConversion.getTimeString(reminder.getDelta());
			String unit = DURATION_UNITS[TimeConversion.getUnitIndex(reminder.getDelta())];
			alarmsBuilder.append(VALARM_TRIGGER).append(VALARM_TRIGGER_REL_END);
			alarmsBuilder.append(delta).append(unit).append("\n");
			alarmsBuilder.append(ACTION).append(ACTION_DISPLAY);
			alarmsBuilder.append(DESCRIPTION).append(task.name).append("\n");
			alarmsBuilder.append(VALARM_END);
		}
	}

	/**
	 * Build a RFC5545 VTODO entry from task information does not take parent or
	 * children into account
	 * 
	 * @param task
	 *            task to transform
	 * @param builder
	 *            where to append the information
	 */
	public static void generateVtodo(Task task, StringBuilder builder) {
		builder.append(VTODO_BEGIN);
		/** NOTE: DTSTAMP or UID not both! */
		builder.append(UID).append(buildUID(task,"TDO")).append("\n");
		/** Due DateTIME */
		builder.append(VTODO_DUE).append(formatRFC5545DateTime(task.getDue())).append("\n");
		/** percent-complete in INTEGER */
		int perc = (int) (task.getProgress() * 100);
		builder.append(VTODO_PERC_COMPLETE).append(String.format("%d", perc)).append("\n");
		addCommonFields(task, builder);
		appendAlarms(task, builder);
		/** STATUS : "NEEDS-ACTION" , "COMPLETED" , "IN-PROCESS" , "CANCELLED" */
		builder.append(STATUS).append(convertTaskState(task.getState())).append("\n");
		builder.append(VTODO_END);
	}

	/**
	 * Build a unique identifier for synchornization <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.4.7"> RFC5545 UID
	 * </a>
	 * 
	 * @param task
	 *            providing information
	 * @param type
	 *            additional information for Unique ID in case we build a VTODO/VEVENT
	 * @return uid string
	 */
	public static String buildUID(Task task, String type) {
		StringBuilder uid = new StringBuilder(32);
		uid.append(task.dtstamp+task.id).append("-").append(type).append("@WhatNext");
		return uid.toString();
	}

}
