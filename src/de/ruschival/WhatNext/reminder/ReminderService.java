/******************************************************************************
 * \filename ReminderService.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Service refreshed periodically, will setup AlarmManager for next event
 * 
 * Originally created on Dec 1, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                             $
 * $LastChangedDate:: 2012-08-12 19:24:55 -0300 (Sun, 12 Aug 2012)     $
 * $Revision:: 62                                                      $
 *----------------------------------------------------------------------------- 
 */
package de.ruschival.WhatNext.reminder;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.Reminder;
import de.ruschival.WhatNext.Task;
import de.ruschival.WhatNext.TimeConversion;
import de.ruschival.WhatNext.db.TaskDBService;
import de.ruschival.WhatNext.ui.TaskShowActivity;

/**
 * @author Thomas Ruschival ReminderService
 */
public class ReminderService extends Service {
	/**
	 * Tag for logging
	 */
	public static String TAG = ReminderService.class.getCanonicalName();

	/**
	 * Pending Intent of the last invocation of AlarmManager
	 */
	private PendingIntent reminderIntent;

	/**
	 * Period between database polls in ms
	 */
	public static final int updatePeriod = 55 * 1000;

	/**
	 * Enable / disable Alarms
	 */
	private boolean alarmsEnable = true;

	/**
	 * Binder for database
	 */
	private TaskDBService.TaskDBServiceBinder taskDBbinder;

	/**
	 * ServiceConnection Object to handle connect-/ disconnect
	 */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder servicebinder) {
			taskDBbinder = (TaskDBService.TaskDBServiceBinder) servicebinder;
			updateNotifications();
		}
	};

	/**
	 * Initialize Variables see also: @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		Intent intentToFire = new Intent(ReminderEventReceiver.REMINDER_EVENT_ACTION);
		reminderIntent = PendingIntent.getBroadcast(this, 0, intentToFire, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	protected void updateNotifications() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				ArrayList<Reminder> reminders = taskDBbinder.getPendingReminders(updatePeriod);

				for (Reminder rem : reminders) {
					setNotification(rem);
				}
			}
		}).start();
	}

	/**
	 * 
	 * 
	 * see also: @see android.app.Service#onStartCommand(android.content.Intent,
	 * int, int)
	 * 
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (alarmsEnable) {
			int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
			long timeToRefresh = SystemClock.elapsedRealtime() + updatePeriod;

			alarmMgr.setInexactRepeating(alarmType, timeToRefresh, updatePeriod, reminderIntent);
			if (taskDBbinder == null || !taskDBbinder.isBinderAlive()) {
				Intent dbConnectionIntent = new Intent(this, TaskDBService.class);
				bindService(dbConnectionIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			} else {
				updateNotifications();
			}
		} else {
			alarmMgr.cancel(reminderIntent);
		}
		return Service.START_NOT_STICKY;
	}

	/**
	 * @param intent
	 * @return
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * To show notification for the alarm on time that is set as reminder
	 * 
	 * @param reminder
	 *            information for this notification
	 */
	private void setNotification(Reminder reminder) {
		final NotificationManager notificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final int remid = reminder.getId().intValue();
		/*
		 * Notification Object TODO: API 11 allows use of Notification Builder
		 */
		final Notification notification = new Notification();
		notification.ledARGB = 0x7f001fff;
		notification.ledOnMS = 350;
		notification.ledOffMS = 800;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.icon = R.drawable.ic_alert;
		notification.vibrate = new long[] { 10, 300, 300, 400, 300, 300 };
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent notificationIntent = new Intent(this, TaskShowActivity.class);
		Long taskID = reminder.getTaskId();
		notificationIntent.putExtra(Task.INTENT_EXTRA_TASKID, taskID);
		notificationIntent.putExtra(Reminder.INTENT_EXTRA_REMINDERID, remid);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
		        taskID.intValue(), notificationIntent, 0);

		String delta = TimeConversion.getTimeString(reminder.getDelta());
		String unit = TimeConversion.getUnitString(reminder.getDelta(), this);

		StringBuilder notificationText = new StringBuilder(35).append(reminder.taskName).append(" ")
		        .append(getString(R.string.sl_due_in)).append(" ").append(delta).append(" ").append(unit);

		notification
		        .setLatestEventInfo(this, getString(R.string.s_app_name), notificationText, contentIntent);
		notificationMgr.notify(remid, notification);
	}

}
