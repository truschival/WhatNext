/******************************************************************************
 * \filename LeastLaxityScheduler.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 *****************************************************************************/
package de.ruschival.WhatNext;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author ruschi 
 * Class to Compare 2 Tasks and by least laxity and determine
 *         overall laxtime
 */
public class LeastLaxityScheduler implements Comparator<Task> {
	/**
	 * Margin at which two laxity are considered same and Priorities are
	 * considered here 5 minutes
	 */
	public static final long LAXITY_MARGIN = 1000 * 60 * 5;

	/**
	 * Milliseconds in a day
	 */
	public static final int MS_IN_DAY = 1000 * 60 * 60 * 24;
	
	
	/**
	 * Timeinstance used for scheduling, usually now()
	 */
	private long t0;
	
	
	/**
	 * Constructor sets the schedulingTime to now();
	 */
	public LeastLaxityScheduler(){
		t0 = Calendar.getInstance().getTimeInMillis();
	}
	
	
	/**
	 * if t1 is more urgent (within Margin or higher priority), 1 if t2 more
	 * urgent or has higher priority
	 * 
	 * @param t1
	 *            Task 1 to compare with
	 * @param t2
	 *            Task 2
	 * @return -1,0,1
	 */
	@Override
	public int compare(Task t1, Task t2) {
		if (t1.getLaxity(t0) < t2.getLaxity(t0)) {
			return -1;
		}
		return 1;
	}

	/**
	 * Check Schedulability of TaskSet without taking task.start into account.
	 * 
	 * @param taskset
	 *            list of open tasks
	 * @return true if under 100% Utilization
	 */
	public boolean isSchedulable(List<Task> taskset) {
		// Sort the taskset by laxity
		Collections.sort(taskset, this);
		long now = Calendar.getInstance().getTimeInMillis();
		long scheduled = 0L;
		long work = 0L;
		for (Task task : taskset) {
			work = task.getRemainingWork();
			// if the work scheduled for previous tasks plus the work left for this task exceed
			// its deadline the schedule is infeasible
			if ((now + scheduled + work) > task.getDue().getTimeInMillis()) {
				return false;
			}
			scheduled += work;
		}
		return true;
	}

	

	/**
	 * Check Schedulability with respect to a daily freetime interval (sleep)
	 * 
	 * @param taskset
	 * @param unavailable
	 *            Array of Time Intervals unavailable for scheduling (lunch, night...)
	 * @return Long.MAX_VALUE if TaskSet is schedulable under given conditions or time in ms
	 *         since epoch at which point deadline is missed
	 */
	public long hasMissedDeadLineInFuture(ArrayList<Task> taskset, PeriodicBlock[] unavailable) {	
		// setup variables
		long t = Calendar.getInstance().getTimeInMillis();
		PeriodicBlock nextBlock = null; // next closest Block
		int delta; // available time until next Periodic Block
		ArrayList<Integer> remainingWork = new ArrayList<Integer>(taskset.size());


		// Check if now is within a Periodic Block, if so move t to end of the Block
		for (PeriodicBlock Bi : unavailable) {
			if (Bi.isInBlock(t)) {
				t = Bi.getComingEnd(t);
				// Since Periodic Blocks are assumed to be disjoint, there is no need to
				// recalculate nPeriods.
				break;
			}
		}
		// Sort the taskset by laxity and check if it is schedulable now or after the block
		if(hasMissedTasks(taskset,t0)){
			return t0;
		}
		for (Task task : taskset) {
			remainingWork.add(task.getRemainingWork());
		}
		
		//Try to schedule all tasks
		while (!taskset.isEmpty()) {
			// reset delta to maximum value to find new minimum
			delta = Integer.MAX_VALUE;
			// Only relevant if we have periodicBlocks
			if (unavailable.length > 0) {
				// Iterate over periodic Blocks to update nPeriods and find minimum delta to
				// start of next block
				for (PeriodicBlock Bi : unavailable) {
				
					int tmp = (int)( Bi.getComingStart(t) - t);
					// find minimum time until next start of a periodic block
					// delta = delta < tmp ? delta : tmp;
					if (tmp < delta) {
						delta = tmp;
						nextBlock = Bi;
					}
				}
			}
			// assertion that actually upcoming periodic events exist
			assert delta<Integer.MAX_VALUE : delta;
						
			// schedule work of tasks as long as free time available
			while (delta > 0 && !taskset.isEmpty()) {
				Task task = taskset.get(0);
				
				// More work todo than time left to deadline--> will fail
				if(t+remainingWork.get(0) > task.getDue().getTimeInMillis()){
					return t;
				}

				if (delta >= remainingWork.get(0)) {
					delta = (int) (delta - remainingWork.get(0));
					// task is fully scheduled, remove it
					taskset.remove(0);
					remainingWork.remove(0);
				} else {
					// "use" delta for work on first task
					remainingWork.set(0, remainingWork.get(0) - delta);
					delta = 0;
				}
			}
			// closest Block found : add its length to t
			if (nextBlock != null) {
				// move t to end of nextBlock for coming iteration while(!taskSet.isEmpty())
				t = nextBlock.getComingEnd(t);
			}
		} // while(!taskSet.isEmpty())
		return Long.MAX_VALUE;
	}

	/**
	 * Necessary Condition: taskset contains no tasks with negative laxity
	 * @param taskset (will be sorted)
	 * @param schedulingTime time instance (usually now)
	 * @return true if not violated at current time
	 */
	public boolean hasMissedTasks(List<Task> taskset, long schedulingTime){
		Collections.sort(taskset, this);
		// necessary condition: most urgent task must have positive laxity
		if(taskset.get(0).getLaxity(schedulingTime) <= 0){
			return true;
		}
		return false;
	}
	
	
	
	
	/**
	 * Check Schedulability of TaskSet under constraint of minimum freetime
	 * 
	 * @param taskset
	 *            list of open tasks
	 * @param buffer
	 *            in ms for each task, not to be scheduled
	 * @return true if under 100% Utilization
	 */
	public boolean isSchedulable(List<Task> taskset, int buffer) {
		// Sort the taskset by laxity
		Collections.sort(taskset, this);
		long now = Calendar.getInstance().getTimeInMillis();
		long scheduled = 0L;
		long work = 0L;
		for (Task task : taskset) {
			work = task.getRemainingWork();
			// if the work scheduled for previous tasks plus the work left for this task exceed
			// its deadline the schedule is infeasible
			if ((now + scheduled + work) > task.getDue().getTimeInMillis()) {
				return false;
			}
			scheduled += work + buffer;
		}
		return true;
	}

}
