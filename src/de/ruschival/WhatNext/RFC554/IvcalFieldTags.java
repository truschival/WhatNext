/******************************************************************************
 * \filename RFC5545FieldTags
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Interface with reserved words defining elements of VTODO, VEVENT
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/

package de.ruschival.WhatNext.RFC554;

public interface IvcalFieldTags {
	public static final String FILE_EXTENSION = ".ics";

	public static final String MIME_TYPE = "text/calendar";

	public static final String VCAL_BEGIN = "BEGIN:VCALENDAR\n";

	public static final String VCAL_VERSION = "VERSION:2.0\n";

	public static final String VCAL_PRODID = "PRODID:-//DE.RUSCHIVAL.WHATNEXT/NONSGML V1.0//EN\n";

	public static final String VCAL_END = "END:VCALENDAR\n";

	public static final String VTODO_BEGIN = "BEGIN:VTODO\n";

	public static final String VTODO_END = "END:VTODO\n";

	public static final String VEVENT_BEGIN = "BEGIN:VEVENT\n";

	public static final String VEVENT_END = "END:VEVENT\n";

	public static final String VALARM_BEGIN = "BEGIN:VALARM\n";

	public static final String VALARM_END = "END:VALARM\n";

	/**
	 * Trigger for VALARM
	 */
	public static final String VALARM_TRIGGER = "TRIGGER;";

	/**
	 * Trigger relation always related to END with negaive relative duration
	 */
	public static final String VALARM_TRIGGER_REL_END = "RELATED=END:-PT";
	
	/**
	 * Trigger relation always related to END with negaive relative duration
	 */
	public static final String VALARM_TRIGGER_REL_DUR = "VALUE=DURATION:-PT";

	/**
	 * Action-Keyword for VALARM <a href="http://tools.ietf.org/html/rfc5545#section-3.8.6.1"> RFC5545:
	 * ACTION </a>
	 */
	public static final String ACTION = "ACTION:";

	/**
	 * Specificc Action - Diplay the reminder
	 */
	public static final String ACTION_DISPLAY = "DISPLAY\n";
	
	/**
	 * Array for DURATION units same order as @see TimeConversion.TIME_SI_UNITS
	 * 
	 * @warning Months does not exist!
	 *          <a href="http://tools.ietf.org/html/rfc5545#section-3.3.6 >
	 *          RFC5545: DURATION </a>
	 */
	public static final String[] DURATION_UNITS = { "M", "H", "D", "W", "" };

	/**
	 * Globally unique id for this VTODO/VEVENT 3.8.4.7. Unique Identifier
	 */
	public static final String UID = "UID:";

	/**
	 * Short description <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.12"> RFC5545:
	 * SUMMARY </a>
	 */
	public static final String SUMMARY = "SUMMARY:";

	/**
	 * Detailed free text description <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.5"> RFC5545:
	 * DESCRIPTION </a>
	 */
	public static final String DESCRIPTION = "DESCRIPTION:";

	/**
	 * Progress of Work (0-100) Value Type: INTEGER <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.8">
	 * RFC5545:PERCENT-COMPLETE</a>
	 */
	public static final String VTODO_PERC_COMPLETE = "PERCENT-COMPLETE:";

	/**
	 * Date/time when task was created. I don't see much difference between
	 * CREATED and DTSTAMP< br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.7.1">
	 * RFC5545:CREATED </a>< br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.7.2">
	 * RFC5545:DTSTAMP </a> DTSTAMP: The value MUST be specified in the UTC time
	 * format.
	 */
	public static final String DTSTAMP = "DTSTAMP:";

	/**
	 * When Task was created Not used in this application <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.7.2"> RFC5545:
	 * DTSTAMP </a>
	 */
	public static final String CREATED = "CREATED:";

	/**
	 * Date/time when task was last modified <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.7.3"> RFC5545:
	 * Last-Mod </a>
	 */
	public static final String LAST_MOD = "LAST-MODIFIED:";

	/**
	 * Numeric Priority <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.9"> RFC5545:
	 * PRIORITY </a>
	 */
	public static final String PRIORITY = "PRIORITY:";

	/**
	 * Free text location where this task happens <br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.1.7"> RFC5545:
	 * LOCATION </a>
	 */
	public static final String LOCATION = "LOCATION:";

	/**
	 * URI for more information on this task <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.4.6"> RFC5545: URI
	 * </a>
	 */
	public static final String URI = "URI:";

	/**
	 * Geo-location as by GPS (FLOAT,FLOAT) <br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.1.6"> RFC5545:
	 * GEO </a>
	 */
	public static final String GEO_LOC = "GEO:";

	/**
	 * Date/time when task will be started (scheduled) the first time - if not
	 * set explicitly, equal to created.<br />
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.2.4"> RFC5545:
	 * DTSTART </a>
	 */
	public static final String START = "DTSTART:";
	/**
	 * Expected end of Task/DUE <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.2.2"> RFC5545 DTEND
	 * </a>
	 */
	public static final String DTEND = "DTEND:";

	/**
	 * Deadline/due date when task has to be complete, will be mapped to DTEND
	 * since DUE is only defined for VTODO < br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.2.3">RFC5545 DUE
	 * </a>
	 */
	public static final String VTODO_DUE = "DUE:";

	/**
	 * Transparency - if Event will block calendar or not. is only defined for
	 * VEVENT < br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.2.7">RFC5545
	 * TRANSPARENCY </a>
	 */
	public static final String VEVENT_TRANSP = "TRANSP:";

	/**
	 * EXPECTED Duration (estimated) - Will not be used since either
	 * START+DURATION or DUE can be used. DUE is only defined for VTODO < br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.2.5">RFC5545
	 * DURATION </a>
	 */
	public static final String DURATION = "DURATION:";

	/**
	 * Contains references to Categories <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.2">RFC5545:
	 * CATEGORIES</a>
	 */
	public static final String CATEGORIES = "CATEGORIES:";

	/**
	 * STATUS of Task/VTODO item. <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.11> RFC5545: VTODO
	 * STATUS</a>
	 * 
	 */
	public static final String STATUS = "STATUS:";

	/**
	 * STATUS of Task/VTODO item. <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.11> RFC5545: VTODO
	 * STATUS</a> "NEEDS-ACTION" ;Indicates to-do needs action
	 */
	public static final String STATUS_NEEDS_ACTION = "NEEDS-ACTION";

	/**
	 * "COMPLETED" ;Indicates to-do completed.
	 */
	public static final String STATUS_COMPLETE = "COMPLETED";

	/**
	 * "CANCELLED" ;Indicates to-do was cancelled.
	 */
	public static final String STATUS_CANCELLED = "CANCELLED";
	/**
	 * "IN-PROCESS" ;Indicates to-do in rocess of.
	 */
	public static final String STATUS_IN_PROGRESS = "IN-PROCESS";

}
