/******************************************************************************
 * \filename ReminderEventReceiver.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Receiver for BroadcastEvent from AlarmManager
 * 
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Thomas Ruschival -
 * @date Dec 1, 2011 ReminderEventReceiver
 */
public class ReminderEventReceiver extends BroadcastReceiver {
	/**
	 * Tag for logging
	 */
	public static String TAG = ReminderEventReceiver.class.getCanonicalName();
	/**
	 * Intent Name specific to this application
	 */
	public static final String REMINDER_EVENT_ACTION = "de.ruschival.WhatNext.reminder.REMINDER_EVENT_ACTION";

	/**
	 * Default Constructor
	 */
	public ReminderEventReceiver() {

	}

	/**
	 * Trigger AlarmManager
	 */
	public void registerAlarm() {

	}

	/**
	 * Simply dispatches Event to service
	 * 
	 * @param context
	 * @param intent
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent startIntent = new Intent(context, ReminderService.class);
		context.startService(startIntent);
	}

}
