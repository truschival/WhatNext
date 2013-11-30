/******************************************************************************
 * \filename CategoryTable.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief
 * 
 * Originally created on Nov 4, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                        $
 * $LastChangedDate:: 2012-08-05 18:06:43 -0300 (Sun, 05 Aug 2012#$
 * $Revision:: 59                                                 $
 *-----------------------------------------------------------------------------  
 */
package de.ruschival.WhatNext.db;


/**
 * @author ruschi
 * 
 */
public final class CategoryTable{
	/** Primary table name */
	public static final String TABLE_NAME = "Categories";
	
	/**  Primary key index  */
	public  static final String COL_ID = "_id";
	/**  category short name  */
	public  static final String COL_NAME = "name";
	/**  Color for Category  */
	public  static final String COL_COLOR = "color";	
	
	/**
	 * Column indexes for Basic columns present in all arrays
	 */
	public static final int IDX_ID = 0;
	public static final int IDX_NAME = 1;
	public static final int IDX_COLOR = 2;

	/** Array for query of all columns */
	public static final String[] ALL_COLUMNS = new String[] { COL_ID, COL_NAME, COL_COLOR };
	
	/** 
	 * SQL-Statement for table creation
	 */
	public static final String STMT_CREATE = " CREATE TABLE " + TABLE_NAME + "(" 
				+ COL_ID    + " INTEGER primary key autoincrement, " 
				+ COL_NAME   + " NVARCHAR(35) not null, "
				+ COL_COLOR  + " INTEGER  ) ";

	/** 
	 * Statement for dropping table 
	 */
	public static final String STMT_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
	
	/**
	 * Insert a Category
	 */
	public static final String STMT_INSERT_CATEGORY = " INSERT into " + TABLE_NAME + "(" 
				+ COL_NAME + ", "+ COL_COLOR + ") values (?,?)";

	/**
	 * Update Category
	 */
	public static final String STMT_UPDATE_CATEGORY = " Update " + TABLE_NAME + " set " 
				+ COL_NAME + "=?, "+ COL_COLOR + "=? "+
				"WHERE " + COL_ID + "= ?" ;
	
}
