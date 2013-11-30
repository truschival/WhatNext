/******************************************************************************
 * \filename Reminder.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 *
 * SPDX-License-Identifier:      GPL-2.0+
 *
 * \brief Defines a Reminder / Alarm for a task
 * 
 ******************************************************************************/
package de.ruschival.WhatNext;

/**
 * @author Thomas Ruschival
 *         Reminder
 */
public class Reminder {
	/**
	 * Key for passing Reminder IDs between activities
	 */
	public static final String INTENT_EXTRA_REMINDERID = "de.ruschival.WhatNext.ReminderID";
	
	
	/**
	 * State of an alarm as in RFC5454
	 */
	/** Normal state, alarm is waiting for its time to be dispatched */
	public static final int SCHEDULED = 0;
	/** Alarm was suppressed */
	public static final int SUSPENDED = 1;
	/** Alarm was confirmed by user */
	public static final int DISMISSED = 2;

	/**
	 * Delta relative to task's due date (time in ms)
	 */
	private long delta;

	/**
	 * State (int) if alarm is scheduled/suspended/dismissed
	 */
	private int state;

	/**
	 * Id of task to which this reminder belongs
	 */
	private Long taskId;
	
	/**
	 * Name of the task for this reminder, used in Notifications
	 * @see{de.ruschival.WhatNext.ReminderService}
	 */
	public String taskName;

	/**
	 * Unique ID of this alarm
	 */
	private Long id;

	/**
	 * Constructor for Reminder
	 * 
	 * @param id
	 *            unique id from database
	 * @param taskid
	 *            unique id task for this reminder
	 * @param delta
	 *            time difference to task's due date
	 * @param state
	 *            of reminder
	 */
	public Reminder(Long id, long taskid, int delta, int state) {
		this.id = id;
		this.taskId = taskid;
		this.delta = delta;
		this.state = state;
	}

	/**
	 * Constructor for Reminder without ID
	 * 
	 * @param taskid
	 *            unique id task for this reminder
	 * @param delta
	 *            time difference to task's due date
	 */
	public Reminder(Long taskid, long delta) {
		this(delta);
		if (taskid != null) {
			this.taskId = taskid;
		}
	}

	/**
	 * Most simple constructor for a reminder
	 * 
	 * @param delta
	 *            time difference to task's due date
	 */
	public Reminder(long delta) {
		this.delta = delta;
		this.state = SCHEDULED;
	}

	/**
	 * @return the delta
	 */
	public long getDelta() {
		return delta;
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @return the task_id
	 */
	public Long getTaskId() {
		return taskId;
	}

	/**
	 * Set TaskID after Reminder has been created
	 * @param taskid id of task for this reminder
	 */
	public void setTaskId(Long taskid) {
		taskId = taskid;
	}
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the newly generated ID after insert
	 * 
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}
}
