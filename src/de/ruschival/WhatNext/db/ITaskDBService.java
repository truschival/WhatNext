/******************************************************************************
 * \filename ITaskDBService.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Service Interface for database access
 * 
 * Originally created on Oct 19, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                        $
 * $LastChangedDate:: 2012-07-09 20:22:16 -0300 (Mon, 09 Jul 2012#$
 * $Revision:: 42                                                 $
 *-----------------------------------------------------------------------------  
 */

package de.ruschival.WhatNext.db;

import java.util.ArrayList;

import android.os.Handler;
import de.ruschival.WhatNext.Category;
import de.ruschival.WhatNext.ListRunnable;
import de.ruschival.WhatNext.Reminder;
import de.ruschival.WhatNext.Task;

public interface ITaskDBService {

	/**
	 * Return the full taskSet as list
	 * Will run in caller thread.
	 * 
	 * @return all tasks from table (sorted)
	 */
	public abstract ArrayList<Task> getTasks();
	
	
	/**
	 * Return the set of non-completed tasks
	 * does not include category or reminders
	 * @return open tasks from table (sorted)
	 */
	public abstract ArrayList<Task> getTaskOverview();
	

	/**
	 * Retrieve a record from database, create a Task object
	 * Will run in caller thread.
	 * 
	 * @param id
	 *            - unique id (primary key in database)
	 * @return task matching with matching ID
	 */
	public abstract Task getTaskByID(long id);

	/**
	 * Delete a single task from table
	 * Will run in caller thread.
	 * 
	 * @param task
	 *            object to delete
	 */
	public abstract void deleteTask(Task task);

	/**
	 * Insert a task object as record in database. This method does not start a
	 * separate thread
	 * 
	 * @param task
	 *            task to insert, after the insert it's id is updated
	 */
	public abstract void insertTask(Task task);

	/**
	 * Update a task record in database, called when editing task information.
	 * Does not affect resumed The tasks id is updated
	 * 
	 * @param task
	 *            Task to update
	 */
	public abstract void updateTask(Task task);

	/**
	 * Start execution of a task and update the resumed field in table
	 * 
	 * @param task
	 *            Task to update
	 * @return true if task could be started (was not running)
	 */
	public abstract boolean startTask(Task task);

	/**
	 * Stop execution of a task and clear the resumed field in table
	 * 
	 * @param task
	 * @param delta
	 *            Time in ms to add to actual
	 * @return true if task could be stopped (was running)
	 */
	public abstract boolean stopTask(Task task, long delta);

	/**
	 * Shortcut to set progress to 100% and update actual if task was running
	 * calls Task.markComplete and updates DB record
	 * 
	 * @param task
	 *            Task to update
	 * @return true if successful
	 */
	public abstract boolean markComplete(Task task);

	/**
	 * Set/clear the suspended flag in the given task will call stop if the task
	 * was running
	 * 
	 * @param task
	 *            Task to update
	 * @param suspend
	 *            flag task as suspended
	 */
	public abstract void suspendTask(Task task, boolean suspend);

	/**
	 * Retrieve all reminders for this task id
	 * 
	 * @param taskID
	 * @return List of Reminders
	 */
	public abstract ArrayList<Reminder> getReminders(long taskID);

	/**
	 * Delete this reminder from Database
	 * 
	 * @param ReminderID
	 */
	public abstract void deleteReminder(long ReminderID);

	/**
	 * Store a new reminder in the Database
	 * 
	 * @param reminder
	 * @return id of new reminder
	 */
	public abstract long insertReminder(Reminder reminder);

	/**
	 * Retrieve Reminders within the next delta ms to be dispatched by NotificationManager
	 * 
	 * @param delta
	 *            usually updateFrequency of service
	 * @return
	 */
	public abstract ArrayList<Reminder> getPendingReminders(int delta);

	/**
	 * Spawn a new Thread and retrieve a sorted list of tasks. The list will be
	 * placed in callbacks.content
	 * 
	 * @param receiver
	 *            Handler object or caller thread
	 * @param callback
	 *            Runnable object of caller thread
	 */
	public abstract void getTaskList(Handler receiver, ListRunnable<Task> callback);

	/**
	 * Retrieve the complete list of categories form database
	 * Will run in caller thread.
	 * 
	 * @return List of categories
	 */
	public abstract ArrayList<Category> getCategories();

	/**
	 * Spawn a new Thread and retrieve a sorted list of tasks. The list will be
	 * be sorted and placed in callbacks.content
	 * 
	 * @param receiver
	 *            Handler object or caller thread
	 * @param callback
	 *            Runnable object of caller thread
	 */
	public void getCategoryList(final Handler receiver, final ListRunnable<Category> callback);

	/**
	 * Insert a task object as record in database. The category id is updated
	 * 
	 * @param category
	 *            new category
	 */
	public abstract void insertCategory(Category category);

	/**
	 * update category record in table, essentially updated the name
	 */
	public abstract void updateCategory(Category category);

	/**
	 * Delete a Category record from the table and clean all mappings in
	 * TaskCategory Table This method does not start a separate thread
	 * 
	 * @param category
	 *            Category object to delete
	 */
	public abstract void deleteCategory(Category category);
}