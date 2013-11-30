/******************************************************************************
 * \filename TaskTable.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief
 * 
 * Originally created on Oct 12, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                        $
 * $LastChangedDate:: 2012-05-23 18:26:39 -0300 (Wed, 23 May 2012#$
 * $Revision:: 36                                                 $
 *-----------------------------------------------------------------------------  
 */
package de.ruschival.WhatNext.db;

/**
 * @author ruschi
 * 
 */
public final class TaskTable {
	/** Primary table name */
	public static final String TABLE_NAME = "Tasks";
	/** Primary key index */
	public static final String COL_ID = "_id";
	/** Task short name */
	public static final String COL_NAME = "name";
	/** Task description */
	public static final String COL_DESC = "desc";
	/** Location where task is due/done, whatever */
	public static final String COL_LOC = "location";
	/** State of task (running, ready, suspended...) */
	public static final String COL_STATE = "state";
	/** Estimated total time for task */
	public static final String COL_WCET = "wcet";
	/** Task priority */
	public static final String COL_PRIO = "prio";
	/** Percentage of work committed (0-1) */
	public static final String COL_PROG = "progress";
	/** Actual time spent on task */
	public static final String COL_ACT = "total";
	/** Timestamp when work on task was resumed */
	public static final String COL_RES = "resumed";
	/** time when task is first included into schedule (usually now) */
	public static final String COL_START = "start";
	/** time when task has to be committed 100% */
	public static final String COL_DUE = "due";
	/** Keep track of parent task */
	public static final String COL_PARENT = "parent";
	/** Information when task was created */
	public static final String COL_CREA = "created";
	/** last modification for synchronization */
	public static final String COL_LAST = "lastmod";

	
	/**
	 * Column indexes for Basic columns present in all arrays
	 */
	public static final int IDX_ID = 0;
	public static final int IDX_NAME = 1;
	public static final int IDX_PRIO = 2;
	public static final int IDX_STATE = 3;
	public static final int IDX_PROG = 4;
	public static final int IDX_WCET = 5;
	public static final int IDX_START = 6;
	public static final int IDX_DUE = 7;
	public static final int IDX_ACT = 8;
	public static final int IDX_RES = 9; /* timestamp, needed to compute state */

	/* Indexed of fields only needed in edit form */
	public static final int IDX_DESC = 10;
	public static final int IDX_LOC = 11;
	/* parent task */
	public static final int IDX_PARENT = 12;
	/* Indexes for synchronization */
	public static final int IDX_CREA = 13;
	public static final int IDX_LAST = 14;

	public static final String[] ALL_COLUMNS = new String[] { COL_ID, COL_NAME, COL_PRIO,
				COL_STATE, COL_PROG, COL_WCET, COL_START, COL_DUE, COL_ACT, COL_RES, COL_DESC,
				COL_LOC, COL_PARENT, COL_CREA, COL_LAST };

	/**
	 * SQL-Statement for table creation
	 */
	public static final String STMT_CREATE = " CREATE TABLE " + TABLE_NAME + "(" + COL_ID
				+ " INTEGER primary key autoincrement, " + COL_NAME + " NVARCHAR(35) not null, "
				+ COL_PRIO + " TINYINT, " + COL_STATE + " TINYINT, " + COL_PROG + " NUMERIC(5,4), "
				+ COL_WCET + " INTEGER, " + COL_START + " INTEGER, " + COL_DUE + " INTEGER, "
				+ COL_ACT + " INTEGER, " + COL_RES + " INTEGER, " + COL_DESC + " TEXT, " + COL_LOC
				+ " TEXT, " + COL_PARENT + " INTEGER, " + COL_CREA + " DATETIME, " + COL_LAST + " DATETIME ) ";

	/** Statement for dropping table */
	public static final String STMT_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;

	/**
	 * Insert a task into table with full information Resumed is cleared
	 */
	public static final String STMT_INSERT_TASK = " INSERT into " + TABLE_NAME + "(" + COL_NAME
				+ ", " + COL_PRIO + ", " + COL_STATE + ", " + COL_PROG + ", " + COL_WCET + ", "
				+ COL_ACT + ", " + COL_RES + ", " + COL_START + ", " + COL_DUE + ", " + COL_DESC
				+ ", " + COL_LOC + ", "+ COL_PARENT + ", " + COL_CREA + ", " + COL_LAST
				+ ") values (?,?,?, ?,?,?,0, ?,?, ?,?,?,strftime('%s','now')*1000,strftime('%s','now')*1000)";

	/**
	 * Update task table with full information from EditForm
	 */
	public static final String STMT_UPDATE_TASK = " Update " + TABLE_NAME + " set " + COL_NAME
				+ "=?, " + COL_PRIO + "=?, " + COL_STATE + "=?, " + COL_PROG + "=?, " + COL_WCET
				+ "=?, " + COL_ACT + "=?, " + COL_START + "=?, " + COL_DUE + "=?, " + COL_DESC
				+ "=?, " + COL_LOC + "=?, " + COL_PARENT + "=?, "+ COL_LAST + "= strftime('%s','now')*1000 " + "WHERE "
				+ COL_ID + "= ?";
}
