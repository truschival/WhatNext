/******************************************************************************
 * \filename IntentConstants.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 * 
 * \brief Constants for Intent action and return codes gobal to WhatNext
 * 
 ******************************************************************************/

package de.ruschival.WhatNext;

import java.util.ArrayList;
import java.util.Calendar;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;
import de.ruschival.WhatNext.db.TaskDBService;
import de.ruschival.WhatNext.ui.PauseDialogActivity;
import de.ruschival.WhatNext.ui.TaskListActivity;

public class WidgetDataService extends Service implements IntentConstants {
    /**
     * Tag for logging
     */
    public final String TAG = WidgetDataService.class.getCanonicalName();

    /**
     * Intent Constants
     */
    public static final String TASK_START_ACTION = "de.ruschival.WhatNext.WidgetDataService.TASK_START";

    public static final String TASK_STOP_ACTION = "de.ruschival.WhatNext.WidgetDataService.TASK_STOP";

    public static final String UPDATE_WIDGET_ACTION = "de.ruschival.WhatNext.WidgetDataService.UPDATE_WIDGET";

    /**
     * Binder Object to interact with TaskDBService
     */
    private TaskDBService.TaskDBServiceBinder taskDBbinder;

    /**
     * Action what todo when the connection is established
     */
    private int action = 0;

    private long taskID;

    /**
     * List of Active tasks
     */
    int[] allWidgetIds;

    /**
     * WidgetManager Handle
     */
    AppWidgetManager widgetMgr;

    /**
     * ServiceConnection Object to handle connect-/ disconnect
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Log.v(TAG,
            // "Disconnected from service");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder servicebinder) {
            // Log.v(TAG,
            // "Connected to service");
            taskDBbinder = (TaskDBService.TaskDBServiceBinder) servicebinder;
            Task task;
            /** update Task List */
            if (action == ITC_START_TASK){
                task = taskDBbinder.getTaskByID(taskID);
                taskDBbinder.startTask(task);
            } else{
                updateWidget(taskDBbinder.getTaskOverview());
            }
            unbindService(serviceConnection);
            stopSelf();
        }
    };

    /**
     * @see android.app.Service#onBind(Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @see android.app.Service#onBind(Intent)
     */
    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return true;
    }

    /**
     * @see android.app.Service#onStart(Intent,int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        action = 0;
        widgetMgr = AppWidgetManager.getInstance(WidgetDataService.this.getApplicationContext());
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)){
            allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        } else{
            allWidgetIds = widgetMgr.getAppWidgetIds(new ComponentName(getApplicationContext(),
                    WidgetListProvider.class));
        }
        if (intent.hasExtra(TASK_START_ACTION)){
            taskID = intent.getLongExtra(Task.INTENT_EXTRA_TASKID, 0);
            action = intent.getIntExtra(TASK_START_ACTION, 0);
        }

        final Intent cnxDbIntent = new Intent(this, TaskDBService.class);
        bindService(cnxDbIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        return START_NOT_STICKY;
    }

    private void updateWidget(ArrayList<Task> tasklist) {

        for (int widgetId : allWidgetIds){
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_row);
            if (!tasklist.isEmpty()){
                Task task = tasklist.get(0);
                /* Task name */
                remoteViews.setTextViewText(R.id.taskName, task.name);
                /** progress */
                int prog = (int) (task.getProgress() * 100);
                remoteViews.setTextViewText(R.id.progress, String.format("%d", prog));
                /** Slack */
                long slackTime = task.getLaxity(Calendar.getInstance().getTimeInMillis());
                int remainingWork = task.getRemainingWork();
                if (slackTime < (0.5 * remainingWork)){
                    remoteViews.setTextColor(R.id.slack, getResources().getColor(R.color.orange));
                } else if (slackTime < remainingWork){
                    remoteViews.setTextColor(R.id.slack, getResources().getColor(R.color.yellow));
                } else{
                    remoteViews.setTextColor(R.id.slack, getResources().getColor(R.color.green));
                }
                remoteViews.setViewVisibility(R.id.progress, View.VISIBLE);
                String approxSlack = TimeConversion.getApproximateString(slackTime);
                remoteViews.setTextViewText(R.id.slack, approxSlack);
                remoteViews.setViewVisibility(R.id.slack, View.VISIBLE);
                if (task.getDue() != null){
                    long delta = task.getDue().getTimeInMillis() - System.currentTimeMillis();
                    if (Math.abs(delta) >= TimeConversion.MS_TO_DAY / 2){
                        remoteViews.setTextViewText(R.id.dueDate,
                                DateFormat.format("dd.MM", task.getDue()));
                    } else{
                        remoteViews.setTextViewText(R.id.dueDate,
                                DateFormat.format("kk:mm", task.getDue()));
                    }
                    remoteViews.setViewVisibility(R.id.dueDate, View.VISIBLE);
                } else{
                    remoteViews.setTextViewText(R.id.dueDate, "----");
                }
                setTaskIcon(remoteViews, task.id, task.getState());
            } else{
                /* No Pending Tasks */
                remoteViews.setTextViewText(R.id.taskName,
                        getResources().getString(R.string.s_nothing_todo));
                remoteViews.setViewVisibility(R.id.slack, View.INVISIBLE);
                remoteViews.setViewVisibility(R.id.dueDate, View.INVISIBLE);
                remoteViews.setViewVisibility(R.id.progress, View.INVISIBLE);
                
                /* Nothing to do, tasklist empty */
                remoteViews.setViewVisibility(R.id.taskIcon_disabled, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.taskIcon, View.INVISIBLE);
                
            }
            /* intent to start list from widget */
            Intent intent = new Intent(this, TaskListActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widgetRow, pendingIntent);
            widgetMgr.updateAppWidget(widgetId, remoteViews);
        }
    }

    /**
     * Update Task Control Button icon according to state
     * 
     * @param task
     */
    private void setTaskIcon(RemoteViews view, long id, Task.State state) {
        Intent intent;
        PendingIntent pendingIntent;
        view.setViewVisibility(R.id.taskIcon_disabled, View.GONE);
        view.setViewVisibility(R.id.taskIcon, View.VISIBLE);
        
        switch (state) {
        case RUNNING:
            intent = new Intent(this, PauseDialogActivity.class);
            intent.putExtra(Task.INTENT_EXTRA_TASKID, id);
            pendingIntent = PendingIntent.getActivity(this, ITC_PAUSE_TASK, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.taskIcon, pendingIntent);
            view.setImageViewResource(R.id.taskIcon, R.drawable.pause);
            break;
        case FUTURE:
        case READY:
            intent = new Intent(this, WidgetDataService.class);
            intent.putExtra(Task.INTENT_EXTRA_TASKID, id);
            intent.putExtra(TASK_START_ACTION, ITC_START_TASK);
            pendingIntent = PendingIntent.getService(this, ITC_START_TASK, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.taskIcon, pendingIntent);
            view.setImageViewResource(R.id.taskIcon, R.drawable.play);
            break;
        case DONE:
            view.setViewVisibility(R.id.taskIcon_disabled, View.VISIBLE);
            view.setViewVisibility(R.id.taskIcon, View.GONE);
            break;
        case OVERDUE:
            intent = new Intent(this, WidgetDataService.class);
            intent.putExtra(Task.INTENT_EXTRA_TASKID, id);
            intent.putExtra(TASK_START_ACTION, ITC_START_TASK);
            pendingIntent = PendingIntent.getService(this, ITC_START_TASK, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.taskIcon, pendingIntent);
            view.setImageViewResource(R.id.taskIcon, R.drawable.play_red);
            break;
        case RUNNING_OVERDUE:
            intent = new Intent(this, PauseDialogActivity.class);
            intent.putExtra(Task.INTENT_EXTRA_TASKID, id);
            pendingIntent = PendingIntent.getActivity(this, ITC_PAUSE_TASK, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.taskIcon, pendingIntent);
            view.setImageViewResource(R.id.taskIcon, R.drawable.ic_alert);
            break;
        }

    }

}
