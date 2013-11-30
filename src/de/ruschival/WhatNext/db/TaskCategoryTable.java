/******************************************************************************
 * \filename CategoryTableColumns.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Column Names for Category Table
 * 
 * Originally created on Nov 4, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                        $
 * $LastChangedDate:: 2012-01-17 18:43:04 -0200 (Tue, 17 Jan 2012#$
 * $Revision:: 6                                                  $
 *-----------------------------------------------------------------------------  
 */
package de.ruschival.WhatNext.db;

/**
 * @author ruschi
 *
 */
public interface TaskCategoryTable {
	/** Primary table name */
	public static final String TABLE_NAME = "TaskCategory";
	
	/**  Foreign Key on task */
	public String COL_TID = "TASK_ID";
	/**  Foreign Key on Category  */
	public String COL_CID = "CATEGORY_ID";
	
	/** Statement for dropping table */
	public static final String STMT_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
	
	/** Statement for table creation */
	public static final String STMT_CREATE = " CREATE TABLE " + TABLE_NAME + "(" + 
				COL_TID + " INTEGER, " +
				COL_CID + " INTEGER)";
	/** insert statement */
	public static final String STMT_INSERT_MAPPING = " INSERT into " + TABLE_NAME + "(" 
				+ COL_TID + ", "+ COL_CID + ") values (?,?)";
	/** delete statement for mapping by taskID */
	public static final String STMT_DELETE_MAPPING_TASK_ID = " DELETE from " + TABLE_NAME  
				+ " WHERE " + COL_TID + "= ? ";

	/** delete statement for mapping by taskID */
	public static final String STMT_DELETE_MAPPING_CAT_ID = " DELETE from " + TABLE_NAME  
				+ " WHERE " + COL_CID + "= ? ";

}
