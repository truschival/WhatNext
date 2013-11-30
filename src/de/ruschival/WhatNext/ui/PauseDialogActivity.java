/******************************************************************************
 * \filename WhatNextMain.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Activity to show details of single task
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import de.ruschival.WhatNext.IntentConstants;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.Task;
import de.ruschival.WhatNext.db.TaskDBService;

public class PauseDialogActivity extends Activity implements IntentConstants {
	/**
	 * Tag for logging
	 */
	public final String TAG = PauseDialogActivity.class.getCanonicalName();

	/**
	 * Task Object for manipulating temporary form data
	 */
	private Task task;
	private Long taskID;

	/**
	 * Binder Object to interact with TaskDBService
	 */
	private TaskDBService.TaskDBServiceBinder taskDBbinder;

	/**
	 * Dialog Elements
	 */
	private TextView progressField;
	private EditText actualField;
	private SeekBar progSeek;
	private Button pauseSaveBtn;
	private Button cancelBtn;
	private TextView nameField;

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
			task = taskDBbinder.getTaskByID(taskID);
			/** fill data in TextViews */
			if (task != null) {
				updateForm();
			} else {
				finish();
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		/** Pause-Dialog */
		setContentView(R.layout.dialog_pause);
		nameField = (TextView) findViewById(R.id.taskName);
		pauseSaveBtn = (Button) findViewById(R.id.pauseSaveBtn);
		cancelBtn = (Button) findViewById(R.id.cancelBtn);
		actualField = (EditText) findViewById(R.id.actual);
		progressField = (TextView) findViewById(R.id.progress);
		progSeek = (SeekBar) findViewById(R.id.progSeek);
		progSeek.setOnSeekBarChangeListener(progSeekChangedListener);
		/** Dialog Buttons */
		pauseSaveBtn.setOnClickListener(pauseUpdateListener);
		cancelBtn.setOnClickListener(dismissListener);

		Intent intent = getIntent();
		if (savedInstanceState == null) {
			// new instance! -> check extras in Bundle
			taskID = intent.getExtras().getLong(Task.INTENT_EXTRA_TASKID);
		} else {
			taskID = savedInstanceState.getLong(Task.INTENT_EXTRA_TASKID);
		}
	}

	SeekBar.OnSeekBarChangeListener progSeekChangedListener = new SeekBar.OnSeekBarChangeListener() {
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				progressField.setText(Integer.toString(progress));
				task.setProgress(progress);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Nothing
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// Nothing
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		final Intent intent = new Intent(this, TaskDBService.class);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		if (isFinishing()) {
			task = null;
			taskID = null;
		}
		super.onPause();
	}

	/**
	 * Listener for Save+Update button on Pause dialog
	 */
	private View.OnClickListener pauseUpdateListener = new View.OnClickListener() {
		public void onClick(View view) {
			long delta = Task.stringToLong(actualField.getText().toString());
			taskDBbinder.stopTask(task, delta);
			setResult(RESULT_OK);
			taskID = null;
			task = null;
			finish();
		}
	};

	/**
	 * Listener for dismissing dialog
	 */
	private View.OnClickListener dismissListener = new View.OnClickListener() {
		public void onClick(View view) {
			setResult(RESULT_CANCELED);
			taskID = null;
			task = null;
			finish();
		}
	};

	/**
	 * Show dialog with values from selectedTask
	 */
	public void updateForm() {
		nameField.setText(task.name);
		actualField.setText(Task.longToString(task.getWorkingTime()));
		progressField.setText(String.format("%d",
				(int) (task.getProgress() * 100.0)));
		progSeek.setProgress((int) (task.getProgress() * 100.0));
	}
}