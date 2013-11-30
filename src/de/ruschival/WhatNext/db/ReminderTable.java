/******************************************************************************
 * \filename ReminderTable.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief 
 * 
 * Originally created on Dec 1, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                             $
 * $LastChangedDate:: 2012-01-17 18:43:04 -0200 (Tue, 17 Jan 2012)     $
 * $Revision:: 6                                                       $
 *----------------------------------------------------------------------------- 
 */
package de.ruschival.WhatNext.db;

/**
 * @author Thomas Ruschival
 * ReminderTable
 */
public final class ReminderTable {
	/** Primary table name */
	public static final String TABLE_NAME = "Reminder";
	
	/**  Primary key index  */
	public  static final String COL_ID = "_id";
	/**  Task ID to which this reminder belongs  */
	public  static final String COL_TID = "task_id";
	/**  Time difference to task.due when this reminder is triggered  */
	public  static final String COL_DELTA = "time_delta";	
	/**  State (int) if alarm is suspended/scheduled/dismissed  */
	public  static final String COL_STATE = "state";
	
	/**
	 * Column indexes for Basic columns present in all arrays
	 */
	public static final int IDX_ID = 0;
	public static final int IDX_TID = 1;
	public static final int IDX_DELTA = 2;
	public static final int IDX_STATE = 3;

	/** Array for query of all columns */
	public static final String[] ALL_COLUMNS = new String[] { COL_ID, COL_TID, COL_DELTA, COL_STATE };
	
	/** 
	 * SQL-Statement for table creation
	 */
	public static final String STMT_CREATE = " CREATE TABLE " + TABLE_NAME + "(" 
				+ COL_ID    + " INTEGER primary key autoincrement, " 
				+ COL_TID    + " INTEGER , " 
				+ COL_DELTA   + " INTEGER , " 
				+ COL_STATE + " INTEGER  ) ";

	/** 
	 * Statement for dropping table 
	 */
	public static final String STMT_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
	
	/**
	 * Insert a Reminder
	 */
	public static final String STMT_INSERT_REMINDER = " INSERT into " + TABLE_NAME + "(" 
				+ COL_TID + ", "+ COL_DELTA + COL_STATE + ") values (?,?,?)";

	/**
	 * Statement to retrieve pending events
	 */
	public static final String STMT_GET_PENDING_REMINDERS = "SELECT " + 
			" rem."+ReminderTable.COL_ID    + " , "+
			" rem."+ReminderTable.COL_TID   + " , "+
			" rem."+ReminderTable.COL_DELTA + " as delta, "+
			" rem."+ReminderTable.COL_STATE + " as state, "+
			" tsk."+TaskTable.COL_DUE + " as due "+
			" FROM " + ReminderTable.TABLE_NAME + " rem ," + TaskTable.TABLE_NAME + " tsk "+
			" WHERE rem.state < 2  "+
			" AND due-delta <= ?"+
			" AND due-delta > ?";
}
