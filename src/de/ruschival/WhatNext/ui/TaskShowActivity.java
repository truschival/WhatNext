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

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.Reminder;
import de.ruschival.WhatNext.Task;
import de.ruschival.WhatNext.RFC554.IvcalFieldTags;
import de.ruschival.WhatNext.RFC554.VcalConverter;
import de.ruschival.WhatNext.db.TaskDBService;

/**
 * @author ruschi Activity to show details of single task
 */
public class TaskShowActivity extends Activity {
	/**
	 * Tag for logging
	 */
	public final String TAG = TaskShowActivity.class.getCanonicalName();

	/**
	 * Task Object for manipulating temporary form data
	 */
	private Task task;
	private Long taskID;

	/** Numeric ID for dialog asking deletion */
	public static final int DIALOG_ID_DELETE = 0;


	/**
	 * Binder Object to interact with TaskDBService
	 */
	private TaskDBService.TaskDBServiceBinder taskDBbinder;

	private class PauseDialog extends Dialog {
		/**
		 * Dialog Elements
		 */
		private TextView _progressField;
		private EditText _actualField;
		private SeekBar _progSeek;
		private Button _pauseSaveBtn;
		private Button _cancelBtn;

		public PauseDialog(Context context) {
			super(context);
			/** Pause-Dialog */
			setContentView(R.layout.dialog_pause);
			_pauseSaveBtn = (Button) findViewById(R.id.pauseSaveBtn);
			_cancelBtn = (Button) findViewById(R.id.cancelBtn);
			_actualField = (EditText) findViewById(R.id.actual);
			_progressField = (TextView) findViewById(R.id.progress);
			_progSeek = (SeekBar) findViewById(R.id.progSeek);
			_progSeek.setOnSeekBarChangeListener(progSeekChangedListener);
			/** Dialog Buttons */
			_pauseSaveBtn.setOnClickListener(pauseUpdateListener);
			_cancelBtn.setOnClickListener(dismissListener);
		}

		SeekBar.OnSeekBarChangeListener progSeekChangedListener = new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					_progressField.setText(Integer.toString(progress));
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

		/**
		 * Listener for Save+Update button on Pause dialog
		 */
		private View.OnClickListener pauseUpdateListener = new View.OnClickListener() {
			public void onClick(View view) {
				long delta = Task.stringToLong(_actualField.getText()
						.toString());
				taskDBbinder.stopTask(task, delta);
				setTaskIcon(task);
				dismiss();
			}
		};

		/**
		 * Listener for dismissing dialog
		 */
		private View.OnClickListener dismissListener = new View.OnClickListener() {
			public void onClick(View view) {
				hide();
			}
		};

		/**
		 * Show dialog with values from selectedTask
		 */
		@Override
		public void show() {
			_actualField.setText(Task.longToString(task.getWorkingTime()));
			_progressField.setText(String.format("%d",
					(int) (task.getProgress() * 100.0)));
			_progSeek.setProgress((int) (task.getProgress() * 100.0));
			super.show();
		}
	}

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
			// have we been called with a bundle for editing a task?
			if (taskID != null) {
				task = taskDBbinder.getTaskByID(taskID);
			} else {
				finish();
			}
			/** fill data in TextViews */
			updateForm();
		}
	};

	/** References to textfields */
	private TextView nameField;
	private TextView actualField;
	private TextView wcetField;
	private TextView dueDateTimeField;
	private TextView startDateTimeField;
	private TextView descField;
	private TextView progressField;
	private TextView categoriesField;
	private TextView prioField;
	// private TextView delegatedField;
	private TextView locationField;
	private ImageButton taskStateBtn;
	private ImageButton backBtn;
	private PauseDialog pauseDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.task_show);

		/* Get Fields */
		nameField = (TextView) findViewById(R.id.taskName);
		actualField = (TextView) findViewById(R.id.actual);
		wcetField = (TextView) findViewById(R.id.wcet);
		dueDateTimeField = (TextView) findViewById(R.id.dueDateTime);
		startDateTimeField = (TextView) findViewById(R.id.startDateTime);
		descField = (TextView) findViewById(R.id.taskDesc);
		progressField = (TextView) findViewById(R.id.progress);
		categoriesField = (TextView) findViewById(R.id.categories);
		prioField = (TextView) findViewById(R.id.prio);
		// delegatedField = (TextView) findViewById(R.id.delegate);
		locationField = (TextView) findViewById(R.id.location);
		taskStateBtn = (ImageButton) findViewById(R.id.taskStateBtn);
		backBtn = (ImageButton) findViewById(R.id.backBtn);

		Intent intent = getIntent();

		/** back Button */
		backBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent toListIntent = new Intent(TaskShowActivity.this,
						TaskListActivity.class);
				toListIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(toListIntent);
			}
		});

		/** pause dialog */
		pauseDialog = new PauseDialog(this);

		if (savedInstanceState == null) {
			// new instance! -> check extras in Bundle
			if (intent.hasExtra(Task.INTENT_EXTRA_TASKID)) {
				taskID = intent.getExtras().getLong(Task.INTENT_EXTRA_TASKID);
			}
			if (intent.hasExtra(Reminder.INTENT_EXTRA_REMINDERID)) {
				backBtn.setVisibility(View.VISIBLE);
			}
		} else {
			taskID = savedInstanceState.getLong(Task.INTENT_EXTRA_TASKID);
		}

		taskStateBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				switch (task.getState()) {
				case RUNNING:
				case RUNNING_OVERDUE:
					pauseDialog.show();
					updateForm();
					break;
				case OVERDUE:
				case FUTURE:
				case READY:
					taskDBbinder.startTask(task);
					updateForm();
					break;
				case DONE:
					break;
				}
			}
		});

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.hasExtra(Task.INTENT_EXTRA_TASKID)) {
			taskID = intent.getExtras().getLong(Task.INTENT_EXTRA_TASKID);
		}
	}

	/**
	 * see also: @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		final Intent intent = new Intent(this, TaskDBService.class);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		pauseDialog.dismiss();
		if (isFinishing()) {
			task = null;
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (taskID != null) {
			outState.putLong(Task.INTENT_EXTRA_TASKID, taskID);
		}
		super.onSaveInstanceState(outState);
	}

	/**
	 * Fill the textfields with data from task
	 */
	private void updateForm() {
		nameField.setText(task.name);
		actualField.setText(Task.longToString(task.getActual()));
		prioField
				.setText(getResources().getTextArray(R.array.TaskPriorities)[task
						.getPriorityOrdinal()]);
		descField.setText(task.desc);
		wcetField.setText(Task.longToString(task.getWcet()));
		progressField.setText(String.format("%d",
				(int) (task.getProgress() * 100.0)));
		startDateTimeField.setText(DateFormat.format("dd/MM/yyyy - kk:mm",
				task.getStart()));
		if (task.getDue() != null) {
			dueDateTimeField.setText(DateFormat.format("dd/MM/yyyy - kk:mm",
					task.getDue()));
		} else {
			dueDateTimeField.setText("---");
		}
		categoriesField.setText(task.getCategoriesString());
		locationField.setText(task.location);
		taskStateBtn.setEnabled(true);
		/** emphasize State */
		setTaskIcon(task);
	}

	/**
	 * Update Taskicon of layout according to task state
	 * 
	 * @param task
	 */
	private void setTaskIcon(Task task) {
		switch (task.getState()) {
		case RUNNING:
			taskStateBtn.setImageResource(R.drawable.pause);
			break;
		case FUTURE:
		case READY:
			taskStateBtn.setImageResource(R.drawable.play);
			break;
		case DONE:
			taskStateBtn.setImageResource(R.drawable.done);
			taskStateBtn.setEnabled(false);
			break;
		case OVERDUE:
			taskStateBtn.setImageResource(R.drawable.play_red);
			break;
		case RUNNING_OVERDUE:
			taskStateBtn.setImageResource(R.drawable.ic_alert);
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ctx_m_task, menu);
		switch (task.getState()) {
		case RUNNING:
		case RUNNING_OVERDUE:
			menu.findItem(R.id.ctxi_run).setEnabled(false);
			break;
		case DONE:
			menu.findItem(R.id.ctxi_run).setEnabled(false);
			menu.findItem(R.id.ctxi_pause).setEnabled(false);
			menu.findItem(R.id.ctxi_mcomplete).setEnabled(false);
			break;
		case READY:
		case OVERDUE:
			menu.findItem(R.id.ctxi_pause).setEnabled(false);
			break;
		case FUTURE:
			menu.findItem(R.id.ctxi_pause).setEnabled(false);
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ctxi_run:
			if (taskDBbinder.startTask(task)) {
				Toast.makeText(getApplicationContext(), R.string.sa_run,
						Toast.LENGTH_SHORT).show();
			}
			finish();
			break;
		case R.id.ctxi_edit:
			Intent i = new Intent(this, TaskEditActivity.class);
			i.putExtra(Task.INTENT_EXTRA_TASKID, task.id);
			startActivity(i);
			break;
		case R.id.ctxi_pause:
			pauseDialog.show();
			break;
		case R.id.ctxi_mcomplete:
			taskDBbinder.markComplete(task);
			finish();
			break;
		case R.id.ctxi_delete:
			showDialog(DIALOG_ID_DELETE);
			break;
		case R.id.ctxi_share:
			Intent dataIntent = new Intent(Intent.ACTION_SEND);
			dataIntent.setType(IvcalFieldTags.MIME_TYPE);
			String filename = task.name.replaceAll(" ", "_")
					+ IvcalFieldTags.FILE_EXTENSION;

			File file = new File(getExternalFilesDir(null), filename);
			Uri absoluteFileUri = Uri.fromFile(file);

			try {
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(VcalConverter.getTaskiCal(task).getBytes());
				fos.close();
				dataIntent.putExtra(Intent.EXTRA_STREAM, absoluteFileUri);
				startActivity(Intent.createChooser(dataIntent,
						getString(R.string.sa_share)));
			} catch (Exception exc) {
				Toast.makeText(getApplicationContext(), "Exception",
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
		}
		return true;
	}

	/**
	 * Build Dialogs by ID
	 */
	public AlertDialog onCreateDialog(int dialogid) {
		switch (dialogid) {
		case DIALOG_ID_DELETE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.confirm_delete_task);
			builder.setPositiveButton(R.string.sl_yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							taskDBbinder.deleteTask(task);
							dismissDialog(DIALOG_ID_DELETE);
							finish();
						}
					});
			builder.setNegativeButton(R.string.sl_no,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		default:
			return null;
		}
	}
}
