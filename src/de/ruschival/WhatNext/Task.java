/******************************************************************************
 * \filename Task.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 *
 * SPDX-License-Identifier:      GPL-2.0+
 * 
 * \brief Storage class for tasks
 * 
 ******************************************************************************/

/**
 * Package / Namespace
 */
package de.ruschival.WhatNext;

import static de.ruschival.WhatNext.TimeConversion.MS_TO_H;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Task is the representation of a item to do, can be scheduled (by comparable)
 */

public class Task {
	/**
	 * Key for passing Task IDs between activities
	 */
	public static final String INTENT_EXTRA_TASKID = "de.ruschival.WhatNext.TaskID";

	/**
	 * Priorities of tasks
	 */
	public enum Priority {
		VERY_HIGH, HIGH, MEDIUM, LOW
	};

	/**
	 * @enum State of task numeric representation of a task's state the actual
	 *       task is computed based on progress, wcet, laxity etc. Only for
	 *       suspension of a task the state is set actively.
	 */
	public enum State {
		READY, RUNNING, DONE, OVERDUE, FUTURE, RUNNING_OVERDUE
	};

	/************** For synchronization ***************************/
	/**
	 * Date/time when task was last modified <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.7.3"> RFC5545:
	 * Last-Mod </a>
	 */
	public long lastmodified;

	/**
	 * Date/time when task was created. I don't see much difference between
	 * CREATED and DTSTAMP< br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.7.1"> RFC5545:
	 * CREATED </a>< br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.7.2"> RFC5545:
	 * DTSTAMP </a> DTSTAMP: The value MUST be specified in the UTC time format.
	 */
	public long dtstamp;

	/**************/

	/**
	 * Unique ID primary key in DB
	 */
	public Long id;

	/**
	 * Flag if Task is to be scheduled or not suspended == true will make the
	 * scheduler ignore this task even if start is in past
	 */
	private boolean suspended;

	/**
	 * percentage of committed work (mapped to 0-1) <br />
	 * Mapped to X-Property PROGRESS <br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.1.8"> RFC5545:
	 * PERCENT-COMPLETE</a>
	 */
	private float progress;

	/**
	 * Estimated time to completion in ms ... well nobody estimates that
	 * precisely but it makes life easier in calculations. Mapped to X-Property
	 * WCET
	 */
	private long wcet;

	/**
	 * Time in ms that has been spent on this task Mapped to X-Property ACTUAL
	 */
	private long actual;

	/**
	 * Name of this task (short description) <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.12"> RFC5545:
	 * SUMMARY </a>
	 */
	public String name;

	/**
	 * Detailed free text description <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.5"> RFC5545:
	 * DESCRIPTION </a>
	 */
	public String desc;

	/**
	 * User assigned priority / relevance <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.9"> RFC5545:
	 * PRIORITY</a>
	 */
	private Priority priority;

	/**
	 * Date/time when task will be started (scheduled) the first time - if not
	 * set explicitly, equal to created.<br />
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.2.2"> RFC5545:
	 * DTSTART </a>
	 */
	private Calendar start;

	/**
	 * Deadline/due date when task has to be complete, will be mapped to DTEND
	 * since DUE is only defined for VTODO < br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.2.2"> RFC5545
	 * DTEND </a>
	 */
	private Calendar due;

	/**
	 * Free text location where this task happens <br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.1.7"> RFC5545:
	 * LOCATION </a>
	 */
	public String location = "";

	/**
	 * Geo-location as by GPS (FLOAT,FLOAT) <br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.8.1.6"> RFC5545:
	 * GEO </a>
	 */
	public String geoloc;

	/**
	 * Email address (free text) of person who should do the task <br/>
	 * <a href="http://tools.ietf.org/html/rfc5545#section-3.2.5">RFC5545:
	 * DELEGATED-TO</a> Unfortunately not part of VTODO
	 */
	private String delegated = "";

	/**
	 * Relationship Child-Parent task to build Trees of subtasks
	 */
	private Task parent;

	/**
	 * Unordered Set of subtasks
	 */
	private HashSet<Task> subtasks;

	/**
	 * List of reminders for this task
	 */
	private ArrayList<Reminder> reminders;

	/**
	 * Contains references to Categories <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.1.2">RFC5545:
	 * CATEGORIES</a>
	 */
	private HashMap<Long, Category> categories;

	/**
	 * URI for more information on this task <a
	 * href="http://tools.ietf.org/html/rfc5545#section-3.8.4.6"> RFC5545: URI
	 * </a>
	 */
	public String uri;

	/**
	 * Date/time when task was resumed. on start this will be set to current
	 * time in ms, on stop the difference will be added to actual and resumed is
	 * cleared
	 */
	public long resumed;

	/**
	 * Period for re-occurence of task, relative to due date
	 */
	public long period;

	/**
	 * Flag if start/due/wcet/actual will be calculated based on subtasks or if
	 * this task will have its own values: in this case not showing accumulated
	 * subtasks values.
	 */
	public boolean accumulateSubtasks;

	/**
	 * Default constructor initializing values for a new task
	 */
	public Task() {
		/* this marks a task as "new" */
		id = null;
		/* Assign sensible default values */
		name = new String("");
		priority = Priority.MEDIUM;
		suspended = false;
		progress = 0f;
		wcet = MS_TO_H; // 1 hour
		actual = 0;
		start = Calendar.getInstance();
		/* nice default start date */
		int minutes = start.get(Calendar.MINUTE);
		int tmp = minutes / 10;
		minutes = tmp * 10;
		start.set(Calendar.MINUTE, minutes);
		/* clear resumed */
		resumed = 0;
		desc = new String("");
		location = new String("");
		categories = new HashMap<Long, Category>(1);
		reminders = new ArrayList<Reminder>(1);
		/* Delegated empty */
		delegated = new String("");
	}

	/**
	 * Constructor for creation of Task from persistent storage, with mandatory
	 * fields other fields may be filled in later
	 * 
	 * @param id
	 *            unique id from Database
	 * @param name
	 *            Short description
	 * @param prio
	 *            User assigned priority (has no effect on scheduling) (numeric
	 *            representation)
	 * @param suspended
	 *            indicator if task is suspended (numeric representation)
	 *            1=suspended, 0=active
	 * @param wcet
	 *            Worst Case Execution Time Estimate
	 * @param actual
	 *            actual time task was executed
	 * @param resumed
	 *            time stamp when task was resumed
	 * @param progress
	 *            work progress
	 * @param duems
	 *            date for deadline (ms)
	 * @param startms
	 *            date when Task is first scheduled for execution (ms)
	 */
	public Task(long id, String name, int prio, int suspended, float progress,
			long wcet, long actual, long resumed, long startms, long duems) {
		this();
		this.id = id;
		this.priority = Priority.values()[prio];
		this.name = name;
		if (suspended > 0) {
			this.suspended = true;
		} else {
			this.suspended = false;
		}
		this.progress = progress;
		this.wcet = wcet;
		this.actual = actual;
		this.resumed = resumed;
		this.start.setTimeInMillis(startms);

		/** Set/clear due date (if exists) */
		setDue(duems);
	}

	/**
	 * Copy Constructor for Task, based on other task does not copy progress or
	 * resumed actual is set to 0 since it is a new task
	 */
	public Task(Task otherTask) {
		/* this marks a task as "new" */
		id = null;
		/* Assign sensible default values */
		name = otherTask.name;
		priority = otherTask.getPriority();
		suspended = otherTask.isSuspended();
		start = otherTask.getStart();
		due = otherTask.getDue();
		wcet = otherTask.getWcet(); // 1 hour
		actual = 0;
		resumed = 0;

		desc = otherTask.desc;
		location = otherTask.location;
		categories = otherTask.getCategories();
		reminders = otherTask.getReminders();

	}

	/**
	 * Build a comma separated String of all Categories of this task
	 * 
	 * @return String
	 */
	public String getCategoriesString() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		/** iterate over categories */
		for (Category cat : categories.values()) {
			if (first) {
				first = false;
			} else {
				builder.append(", ");
			}
			builder.append(cat.name);
		}
		return builder.toString();
	}

	/**
	 * Provide access to due date/time of task returns null if no due date set!
	 * 
	 * @return due date
	 */
	public Calendar getDue() {
		return this.due;
	}

	/**
	 * Set Due date/time as calendar object
	 * 
	 * @param due
	 *            due date
	 */
	public void setDueDate(Calendar due) {
		this.due = due;
	}

	/**
	 * Set a default due date for the task in 1 week
	 */
	public void setDefaultDueDate() {
		/* Task does not have due date set */
		due = Calendar.getInstance();
		/* default due date 1 week ahead */
		due.add(Calendar.WEEK_OF_YEAR, 1);
		due.set(Calendar.MINUTE, 0);
	}

	/**
	 * Set Due date by ms or delete due by passing null
	 * 
	 * @param duems
	 *            due in ms or null to clear
	 */
	public void setDue(long duems) {
		if (duems == 0) {
			due = null;
		} else {
			if (due == null) {
				due = Calendar.getInstance();
			}
			due.setTimeInMillis(duems);
		}
	}

	/**
	 * Set the start date/time as calendar object
	 * 
	 * @param start
	 */
	public void setStart(Calendar start) {
		this.start = start;
	}

	/**
	 * Set the start date/time as Long (in ms)
	 * 
	 * @param startms
	 */
	public void setStart(Long startms) {
		this.start.setTimeInMillis(startms);
	}

	public Calendar getStart() {
		return this.start;
	}

	/**
	 * Assign progress for a percentage value, calls internally
	 * setProgress(float)
	 * 
	 * @param prog
	 *            - progress value (0 .. 100)
	 */
	public void setProgress(int prog) {
		setProgress(((float) prog) / 100.0f);
	}

	/**
	 * Assign progress
	 * 
	 * @param prog
	 *            - progress value (0.0 .. 1.0)
	 */
	public void setProgress(float prog) {
		if (prog >= 0.0 && prog <= 1.0) {
			progress = prog;
		} else {
			progress = 0.5f;
		}
	}

	/**
	 * Try to parse a meaningful long value from a string.
	 * 
	 * @param str
	 *            string to parse (hh:mm h.m)
	 * @return time in ms
	 */
	public static long stringToLong(String str) {
		float tmp = 0.0f;
		try {
			/* substitute comma */
			str = str.replaceAll(",", ".");
			/* First try to parse a float */
			if (str.contains(".")) {
				tmp = Float.parseFloat(str);
				tmp = tmp * MS_TO_H;
				return (long) tmp;
			} else {
				return Long.parseLong(str) * MS_TO_H;
			}
		} catch (NumberFormatException exc) {
			return 0L;
		}
	}

	/**
	 * Converts long value (time in ms) to a string representation with possible
	 * decimals
	 * 
	 * @param val
	 *            value to convert
	 * @return formatted string
	 */
	public static String longToString(long val) {
		float tmp = 0.0f;
		tmp = ((float) val) / MS_TO_H;
		return String.format("%.02f", tmp);
	}

	/**
	 * Set the actual time passed since start. reverts to 0 if negative
	 * 
	 * @param act
	 *            - time in ms
	 */
	public void setActual(long act) {
		if (act >= 0) {
			actual = act;
		} else {
			actual = MS_TO_H;
		}
	}

	/**
	 * Set the Worst Case Execution Time (estimate)
	 * 
	 * @param wcet
	 *            estimated time in ms
	 * 
	 */
	public void setWcet(long wcet) {
		if (wcet >= 0) {
			this.wcet = wcet;
		} else {
			this.wcet = MS_TO_H;
		}
	}

	/**
	 * Set task progress to 100% and stop it (if it was running) will eventually
	 * update actual if it was running
	 * 
	 * @return true if task was running
	 */
	public boolean markComplete() {
		progress = 1.0f;
		return stopAndUpdate(0);
	}

	/**
	 * Remove (suspend) task from schedule or add it so to say a "soft" delete
	 * 
	 * @param suspend
	 */
	public void suspend(boolean suspend) {
		this.suspended = suspend;
	}

	/**
	 * Check if task is suspended
	 * 
	 * @return suspended flag
	 */
	public boolean isSuspended() {
		return suspended;
	}

	/**
	 * @return the reminders
	 */
	public ArrayList<Reminder> getReminders() {
		return reminders;
	}

	/**
	 * Add a new Reminder to this task
	 * 
	 * @param reminder
	 *            to add (taskid will be completed in reminder)
	 */
	public void add(Reminder reminder) {
		if (id != null) {
			reminder.setTaskId(id);
		}
		reminders.add(reminder);
	}

	/**
	 * Remove Reminder from this task
	 * 
	 * @param reminder
	 */
	public void remove(Reminder reminder) {
		reminders.remove(reminder);
	}

	/**
	 * Set the Reminder as new List (does not copy)
	 * 
	 * @param reminderList
	 */
	public void setReminders(ArrayList<Reminder> reminderList) {
		this.reminders = reminderList;
	}

	/**
	 * Inquire the tasks state
	 * 
	 * @return current state based on scheduling information
	 */
	public State getState() {
		Calendar now = Calendar.getInstance();
		State currentState = State.READY;

		/** check if the task is completed !float!) */
		if (progress <= 0.99) {
			/** check if the task is overdue */
			if (due != null && now.compareTo(due) >= 0) {
				if (resumed > 0) {
					currentState = State.RUNNING_OVERDUE;
				} else {
					currentState = State.OVERDUE;
				}
			} else { // Not overdue
				if (resumed > 0) {
					currentState = State.RUNNING;
				} else { // Not Active
					if (now.compareTo(start) < 1) {
						currentState= State.FUTURE;
					} else{
						currentState = State.READY;
					}					
				}
			}
		} else { // Completed
			currentState = State.DONE;
		}
		return currentState;
	}

	/**
	 * Start execution of a task: Save the time when the work was started. If a
	 * SUSPENDED task is started, it's state cleared. If the initial start date
	 * was in the future it is set to now() Will clear suspended flag!
	 * 
	 * @return true if start successful
	 */
	public boolean start() {
		if (resumed == 0) {
			suspended = false;
			resumed = Calendar.getInstance().getTimeInMillis();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Add time difference between resumed and now to actual. Will check if
	 * resumed was set. Will change internal state variable, thus clearing
	 * eventual suspended flag!
	 * 
	 * @param delta
	 *            time difference in ms to add to actual (getWorkingTime() can
	 *            be used)
	 * @return true if task was running, false if stop was called on not-running
	 *         task
	 */
	public boolean stopAndUpdate(long delta) {
		if (resumed > 0L) {
			suspended = false;
			actual = actual + delta;
			resumed = 0;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Set Priority from integer representation
	 * 
	 * @param ordinal
	 */
	public void setPriority(int ordinal) {
		this.priority = Priority.values()[ordinal];
	}

	/**
	 * Get tasks Priority
	 * 
	 * @return current priority
	 */
	public Priority getPriority() {
		return priority;
	}

	/**
	 * Convert the priority to integer for storage in Database
	 * 
	 * @return integer representation of Priority
	 */
	public int getPriorityOrdinal() {
		return priority.ordinal();
	}

	/**
	 * Get Progress either related to subtasks or directly as estimate.
	 * 
	 * @return progress
	 */
	public float getProgress() {
		float progress = this.progress;
		long totalWork = 0;
		float workComplete = 0;
		if (accumulateSubtasks) {
			for (Task child : subtasks) {
				workComplete += child.getProgress() * child.getWcet();
				totalWork += child.getWcet();
			}
			progress = workComplete / totalWork;
		}
		return progress;
	}

	/**
	 * Accumulate or return the actually spent time on task
	 * 
	 * @return the actual
	 */
	public long getActual() {
		long actual = this.actual;
		if (accumulateSubtasks) {
			for (Task child : subtasks) {
				actual += child.getActual();
			}
		}
		return actual;
	}

	/**
	 * Accumulate or return the estimated time on task
	 * 
	 * @return the wcet
	 */
	public long getWcet() {
		long wcet = this.wcet;
		if (accumulateSubtasks) {
			for (Task child : subtasks) {
				wcet += child.getWcet();
			}
		}
		return wcet;
	}

	/**
	 * Gives the laxitity of the task without taking into account the
	 * non-working hours
	 * 
	 * @param t0
	 *            timeinstance for which the laxity is calculated, usually now()
	 * @return lax time in ms (Long.MAX_VALUE if no due date set)
	 */
	public long getLaxity(long t0) {
		State state = getState();

		/* in any case completed task are at the end of the list */
		if (state == State.DONE) {
			return Long.MAX_VALUE;
		}
		/** task without due date to the end but before completed tasks */
		if (due == null) {
			return Long.MAX_VALUE - Integer.MAX_VALUE;
		} else {
			/** Incomplete Tasks with due date are scheduled normally */
			return due.getTimeInMillis() - t0 - getRemainingWork();
		}
	}

	/**
	 * Get time difference between resumed timestamp and now
	 * 
	 * @return time difference in ms (0 if not running)
	 */
	public long getWorkingTime() {
		long diff = 0L;
		if (resumed > 0L) {
			long now = Calendar.getInstance().getTimeInMillis();
			diff = now - resumed;
		}
		return diff;
	}

	/**
	 * returns the (estimated) remaining work for this task
	 * 
	 * @return wcet*(1-progress)
	 */
	public int getRemainingWork() {
		return (int) (getWcet() * (1.0f - getProgress()));
	}

	/**
	 * @return the categories
	 */
	public HashMap<Long, Category> getCategories() {
		return categories;
	}

	/**
	 * @param categories
	 *            the categories to set
	 */
	public void setCategories(HashMap<Long, Category> categories) {
		this.categories = categories;
	}

	/**
	 * access to delegated field
	 * 
	 * @return delegated if set, empty string otherwise
	 */
	public String getDelegated() {
		if (delegated != null) {
			return delegated;
		} else {
			return new String();
		}
	}

	/**
	 * Add a Parent Task
	 * 
	 * @param parent
	 *            / Null
	 */
	public void setParent(Task parent) {
		this.parent = parent;
	}

	/**
	 * return the parent task
	 * 
	 * @return parent
	 * @reval NULL if task itself is on top level
	 */
	public Task getParent() {
		return parent;
	}

	/**
	 * Access to HashSet of all direct sub-tasks
	 * 
	 * @return subtasks
	 */
	public HashSet<Task> getSubtasks() {
		return subtasks;
	}

}
