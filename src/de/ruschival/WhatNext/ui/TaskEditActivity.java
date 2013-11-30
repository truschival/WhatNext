/******************************************************************************
 * \filename TaskEditActivity.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief
 * 
  * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext.ui;

import java.util.ArrayList;
import java.util.Calendar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import de.ruschival.WhatNext.Category;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.Reminder;
import de.ruschival.WhatNext.Task;
import de.ruschival.WhatNext.TimeConversion;
import de.ruschival.WhatNext.db.TaskDBService;

/**
 * @author ruschi
 * 
 */
public class TaskEditActivity extends Activity {
	public final String TAG = TaskEditActivity.class.getCanonicalName();

	/** DatePicker Dialog ID for Selection of StartDate */
	private static final int START_DATE_DIALOG_ID = 0;
	/** DatePicker Dialog ID for Selection of Start Time */
	private static final int START_TIME_DIALOG_ID = 1;
	/** DatePicker Dialog ID for Selection of Due Date */
	private static final int DUE_DATE_DIALOG_ID = 2;
	/** DatePicker Dialog ID for Selection of Due Time */
	private static final int DUE_TIME_DIALOG_ID = 3;
	/** Dialog ID for validation of due time/date at save */
	private static final int INVALID_STARTTIME_DIALOG_ID = 4;
	/** Dialog ID for validation at save */
	private static final int NO_NAME_DIALOG_ID = 5;

	/**
	 * @author Thomas Ruschival ReminderElement
	 */
	public class ReminderElement {
		/**
		 * Layout to host
		 */
		private RelativeLayout layout;

		/**
		 * Reference to minus Button
		 */
		private ImageButton deleteReminderBtn;
		/**
		 * Reference to time GUI element
		 */
		private TextView timeField;

		/**
		 * Reference to timeUnit GUI element
		 */
		private TextView timeUnitField;

		/**
		 * Reminder shown/deleted by this ReminderElement
		 */
		public Reminder reminder;

		/**
		 * Constructor with full information
		 * 
		 * @param reminder
		 */
		public ReminderElement(Reminder reminder) {
			this.reminder = reminder;
			LayoutInflater inflator = getLayoutInflater();
			layout = (RelativeLayout) inflator.inflate(R.layout.reminder, null);
			timeUnitField = (TextView) layout.findViewById(R.id.timeUnitsLabel);
			timeField = (TextView) layout.findViewById(R.id.timeLabel);
			deleteReminderBtn = (ImageButton) layout
					.findViewById(R.id.deleteReminderBtn);

			String delta = TimeConversion.getTimeString(reminder.getDelta());
			String unit = TimeConversion.getUnitString(reminder.getDelta(),
					TaskEditActivity.this);

			timeUnitField.setText(unit);
			timeField.setText(delta);

			deleteReminderBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteReminderElement(ReminderElement.this);
				}
			});
		}
	}

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
	 * ServiceConnection Object to handle connect-/ disconnect
	 */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder servicebinder) {
			taskDBbinder = (TaskDBService.TaskDBServiceBinder) servicebinder;
			categoryDialog.taskDBbinder = taskDBbinder;
			// have we been called with a bundle for editing a task?
			if (taskID != null) {
				task = taskDBbinder.getTaskByID(taskID);
			} else {
				// No intent , no saved state, no taskID -> a new Task
				task = new Task();
				// give the new task a default start date
				task.setDefaultDueDate();
			}
			/** fill data in TextViews */
			updateForm();
		}
	};

	/**
	 * private edit text fields
	 */
	/** task Name */
	private EditText nameField;
	/** Seekbar for progress */
	private SeekBar progSeek;
	private TextView progressField;
	private EditText actualField;
	private EditText wcetField;
	private EditText dueDateField;
	private EditText dueTimeField;
	private EditText startTimeField;
	private EditText startDateField;
	private EditText descField;
	private CheckBox dueCheck;

	/** Tiny button to show/hide additional fields */
	private ImageView moreExpander;

	/** label for priority spinner */
	private TextView prioLabel;
	/** spinner to select task priority */
	private Spinner prioSpin;
	/** label for categories */
	private TextView categoryLabel;
	/** Category Selection dialog opens on click */
	private Button categoryBtn;

	/** label for location */
	private TextView locationLabel;
	private EditText location;

	/** label for delegatation */
	// private TextView delegatedLabel;
	// private EditText delegated;

	/** Tiny button to show/hide reminderPane */
	private ImageView reminderExpander;
	/**
	 * Layout containing reminderElements and the field for creating new
	 * Reminders
	 */
	private LinearLayout reminderListLayout;
	/** Button for new Reminder */
	private ImageButton newReminderBtn;
	/** TextField for time of new Reminder */
	private EditText timeDeltaField;
	/** TimeUnit Spinner for selecting time unit for new reminder */
	private Spinner timeUnitSpinner;

	/** List of Reminder Layouts */
	private ArrayList<ReminderElement> ReminderElements;

	/**
	 * Save Button: only this button causes a save to database
	 */
	private Button saveBtn;

	/**
	 * Dialog for category selection
	 */
	private CategorySelectionDialog categoryDialog;

	/**
	 * Listener for Date/Time fields
	 */
	private View.OnTouchListener dateTimeClickListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (v.getId()) {
			case R.id.startDate:
				showDialog(START_DATE_DIALOG_ID);
				break;
			case R.id.startTime:
				showDialog(START_TIME_DIALOG_ID);
				break;
			case R.id.dueDate:
				showDialog(DUE_DATE_DIALOG_ID);
				break;
			case R.id.dueTime:
				showDialog(DUE_TIME_DIALOG_ID);
				break;
			default:

			}
			return true;
		}
	};

	private DatePickerDialog.OnDateSetListener StartDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int month, int day) {
			Calendar start = task.getStart();
			start.set(year, month, day);
			startDateField.setText(DateFormat.format("dd/MM/yyyy",
					task.getStart()));
			hideSoftKeyboard();
		}
	};

	private TimePickerDialog.OnTimeSetListener StartTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hour, int minute) {
			Calendar start = task.getStart();
			start.set(Calendar.HOUR_OF_DAY, hour);
			start.set(Calendar.MINUTE, minute);
			startTimeField.setText(DateFormat.format("kk:mm", task.getStart()));
			hideSoftKeyboard();
		}
	};

	private DatePickerDialog.OnDateSetListener DueDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int month, int day) {
			Calendar due = task.getDue();
			if (due != null) {
				due.set(year, month, day);
				dueDateField.setText(DateFormat.format("dd/MM/yyyy",
						task.getDue()));
			}
			hideSoftKeyboard();
		}
	};

	private TimePickerDialog.OnTimeSetListener DueTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hour, int minute) {
			Calendar due = task.getDue();
			if (due != null) {
				due.set(Calendar.HOUR_OF_DAY, hour);
				due.set(Calendar.MINUTE, minute);
				dueTimeField.setText(DateFormat.format("kk:mm", task.getDue()));
			}
			hideSoftKeyboard();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_edit);
		Intent intent = getIntent();

		/** Simple fields */
		nameField = (EditText) findViewById(R.id.taskName);
		actualField = (EditText) findViewById(R.id.actual);
		wcetField = (EditText) findViewById(R.id.wcet);
		dueDateField = (EditText) findViewById(R.id.dueDate);
		dueTimeField = (EditText) findViewById(R.id.dueTime);
		startTimeField = (EditText) findViewById(R.id.startTime);
		startDateField = (EditText) findViewById(R.id.startDate);
		dueCheck = (CheckBox) findViewById(R.id.checkDue);
		descField = (EditText) findViewById(R.id.taskDesc);
		progressField = (TextView) findViewById(R.id.progress);
		progSeek = (SeekBar) findViewById(R.id.progSeek);

		/** Expander */
		moreExpander = (ImageView) findViewById(R.id.moreExpander);
		/** priority */
		prioLabel = (TextView) findViewById(R.id.prioLabel);
		prioSpin = (Spinner) findViewById(R.id.prioSpin);
		/** category Dialog */
		categoryLabel = (TextView) findViewById(R.id.categoryLabel);
		categoryDialog = new CategorySelectionDialog(this, true);
		categoryBtn = (Button) findViewById(R.id.categoryBtn);
		/** Location */
		locationLabel = (TextView) findViewById(R.id.locationLabel);
		location = (EditText) findViewById(R.id.location);
		/** delegated */
		// delegatedLabel = (TextView) findViewById(R.id.delegatedLabel);
		// delegated = (EditText) findViewById(R.id.delegated);
		/** Reminders Layout Elements */
		reminderListLayout = (LinearLayout) findViewById(R.id.reminderListLayout);
		timeDeltaField = (EditText) findViewById(R.id.timeDelta);
		timeUnitSpinner = (Spinner) findViewById(R.id.timeUnitSpin);
		newReminderBtn = (ImageButton) findViewById(R.id.newReminderBtn);
		reminderExpander = (ImageView) findViewById(R.id.reminderExpander);
		ReminderElements = new ArrayList<TaskEditActivity.ReminderElement>();
		/** save button */
		saveBtn = (Button) findViewById(R.id.saveBtn);

		/*
		 * Check if entirely new TaskEditActivity is called or if we return from
		 * saved state
		 */
		if (savedInstanceState == null) {
			// new instance! -> check extras in Bundle
			if (intent.hasExtra(Task.INTENT_EXTRA_TASKID)) {
				taskID = intent.getExtras().getLong(Task.INTENT_EXTRA_TASKID);
			}
		} else {
			taskID = savedInstanceState.getLong(Task.INTENT_EXTRA_TASKID);
		}

		/** activate listeners on Views */
		attachListeners();
	}

	@Override
	protected void onResume() {
		super.onResume();
		final Intent intent = new Intent(this, TaskDBService.class);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Called in onCreate() to attach listeners to View Elements
	 */
	public void attachListeners() {
		saveBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				/* Update local variable task before checking and saving it */
				updateVariablesFromForm();
				/* Validate name */
				if (task.name.length() < 1) {
					showDialog(NO_NAME_DIALOG_ID);
					return;
				}
				/* validate start < due */
				if (dueCheck.isChecked()) {
					if (task.getStart().after(task.getDue())) {
						showDialog(INVALID_STARTTIME_DIALOG_ID);
						return;
					}
				}

				/* write to database */
				saveToDB();
				setResult(RESULT_OK);
				finish(); /* destroy activity */
			}
		});
		dueCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				handleDueCheck(isChecked);
			}
		});
		progSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					progressField.setText(Integer.toString(progress));
					task.setProgress(progress);
					hideSoftKeyboard();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
		moreExpander.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!moreExpander.isSelected()) {
					categoryLabel.setVisibility(View.VISIBLE);
					categoryBtn.setVisibility(View.VISIBLE);
					prioLabel.setVisibility(View.VISIBLE);
					prioSpin.setVisibility(View.VISIBLE);
					locationLabel.setVisibility(View.VISIBLE);
					location.setVisibility(View.VISIBLE);
					// delegatedLabel.setVisibility(View.VISIBLE);
					// delegated.setVisibility(View.VISIBLE);
					moreExpander.setSelected(true);
				} else {
					categoryLabel.setVisibility(View.GONE);
					categoryBtn.setVisibility(View.GONE);
					prioLabel.setVisibility(View.GONE);
					prioSpin.setVisibility(View.GONE);
					locationLabel.setVisibility(View.GONE);
					location.setVisibility(View.GONE);
					moreExpander.setSelected(false);
				}
			}
		});
		categoryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				taskDBbinder.getCategoryList(categoryDialog.uiThreadHandler,
						categoryDialog.uiListRunnable);
				categoryDialog.show();
			}
		});
		categoryDialog.getSaveButton().setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						updateTaskCategories(categoryDialog.getCategoryList()
								.getCheckedItemPositions());
						categoryDialog.dismiss();
					}
				});
		reminderExpander.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!reminderExpander.isSelected()) {
					reminderListLayout.setVisibility(View.VISIBLE);
					reminderExpander.setSelected(true);
				} else {
					reminderListLayout.setVisibility(View.GONE);
					reminderExpander.setSelected(false);
				}
			}
		});
		newReminderBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				long delta = Integer.parseInt(timeDeltaField.getText()
						.toString());
				long unit = TimeConversion.TIMEUNIT_CONSTANTS[(int) timeUnitSpinner
						.getSelectedItemId()];
				delta = delta * unit;
				Reminder reminder = new Reminder(task.id, delta);
				task.add(reminder);
				addReminderElement(reminder);
				hideSoftKeyboard();
			}
		});
		startDateField.setOnTouchListener(dateTimeClickListener);
		dueDateField.setOnTouchListener(dateTimeClickListener);
		startTimeField.setOnTouchListener(dateTimeClickListener);
		dueTimeField.setOnTouchListener(dateTimeClickListener);

	}

	/**
	 * read all values form Form into local variables. This method deals with
	 * fields that are not set by special listeners
	 */
	public void updateVariablesFromForm() {
		task.name = nameField.getText().toString();
		task.setActual(Task.stringToLong(actualField.getText().toString()));
		task.setWcet(Task.stringToLong(wcetField.getText().toString()));
		task.setProgress(progSeek.getProgress());
		task.desc = descField.getText().toString();
		task.location = location.getText().toString();
		// task.delegated = delegated.getText().toString();
		task.setPriority((int) prioSpin.getSelectedItemId());
		/* Due date will be cleared if unchecked */
		if (!dueCheck.isChecked()) {
			task.setDueDate(null);
		}
		return;
	}

	/**
	 * Manage result from categoryDialog to update categories in Task
	 * 
	 * @param checkedCategories
	 *            categories marked in list
	 */
	private void updateTaskCategories(SparseBooleanArray checkedCategories) {
		/** Update Categories */
		task.getCategories().clear();

		/** Get original array of categories */
		ArrayList<Category> categorylist = taskDBbinder.getCategories();
		for (int i = 0; i < categorylist.size(); i++) {
			Category cat = categorylist.get(i);
			if (checkedCategories.get(i)) {
				task.getCategories().put(cat.id, cat);
			}
		}
		updateCategoryButtonText();
	}

	/**
	 * Called by reminderElement.deleteButton to remove itself from the layout
	 * and delete the reminder from Tasks reminderlist TODO: debug/test
	 * 
	 * @param element
	 */
	private void deleteReminderElement(ReminderElement element) {
		Reminder reminder = element.reminder;
		/* remove reminder form task's reminders */
		task.remove(reminder);
		/* if the reminder has been saved to database, delete it */
		if (reminder.getId() != null) {
			taskDBbinder.deleteReminder(reminder.getId());
		}
		/* remove the LayoutElement */
		ReminderElements.remove(element);
		reminderListLayout.removeView(element.layout);
	}

	/**
	 * Add a new ReminderElement with the information from reminder
	 * 
	 * @param reminder
	 */
	private void addReminderElement(Reminder reminder) {
		ReminderElement remEle = new ReminderElement(reminder);
		ReminderElements.add(remEle);
		reminderListLayout.addView(remEle.layout);
	}

	/**
	 * Actions Called in both onClickListener() and during updateForm() to
	 * handle duedate
	 * 
	 * @param isChecked
	 *            state of checkbox
	 */
	private void handleDueCheck(boolean isChecked) {
		if (isChecked) {
			dueTimeField.setEnabled(true);
			dueTimeField.setClickable(true);
			dueDateField.setEnabled(true);
			dueDateField.setClickable(true);
			Calendar due = task.getDue();
			if (due == null) {
				task.setDefaultDueDate();
			}
			dueDateField
					.setText(DateFormat.format("dd/MM/yyyy", task.getDue()));
			dueTimeField.setText(DateFormat.format("kk:mm", task.getDue()));
		} else {
			dueDateField.setText("-----");
			dueTimeField.setText("-----");
			dueDateField.setEnabled(false);
			dueDateField.setClickable(false);
			dueTimeField.setEnabled(false);
			dueTimeField.setClickable(false);
		}
	}

	/**
	 * Populates fields in form according to values in local variables
	 */
	private void updateForm() {
		nameField.setText(task.name);
		actualField.setText(Task.longToString(task.getActual()));
		prioSpin.setSelection(task.getPriorityOrdinal());
		descField.setText(task.desc);
		wcetField.setText(Task.longToString(task.getWcet()));
		progressField.setText(String.format("%d",
				(int) (task.getProgress() * 100.0)));
		progSeek.setProgress((int) (task.getProgress() * 100.0));
		/** StartDate */
		startDateField
				.setText(DateFormat.format("dd/MM/yyyy", task.getStart()));
		startTimeField.setText(DateFormat.format("kk:mm", task.getStart()));
		/** DueDate */
		if (task.getDue() != null) {
			dueCheck.setChecked(true);
		} else {
			dueCheck.setChecked(false);
		}
		/** location */
		location.setText(task.location);
		/** delegated */
		// delegated.setText(task.delegated);
		/** Populate Reminders */
		for (Reminder reminder : task.getReminders()) {
			addReminderElement(reminder);
		}

		handleDueCheck(dueCheck.isChecked());
		updateCategoryButtonText();
	}

	/**
	 * Update the Label on the CategoryButton to show (at least a part of) the
	 * categories
	 */
	private void updateCategoryButtonText() {
		String taskCategories = task.getCategoriesString();
		if (taskCategories.length() > 0) {
			categoryBtn.setText(taskCategories);
			ArrayList<Category> allCategories = taskDBbinder.getCategories();
			for (int i = 0; i < allCategories.size(); i++) {
				if (task.getCategories().containsKey(allCategories.get(i).id)) {
					categoryDialog.getCategoryList().setItemChecked(i, true);
				}
			}
		} else {
			categoryBtn.setText(R.string.sa_choose_categories);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (taskID != null) {
			outState.putLong(Task.INTENT_EXTRA_TASKID, taskID);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		if (isFinishing()) {
			task = null;
		} else {

		}
		super.onPause();
	}

	/**
	 * Save local variable task to database You should call
	 * updateVariblesFromForm() and validateXX() first
	 * 
	 * @return 0 if inserted, 1 if task existed and was updated
	 */
	private int saveToDB() {
		if (task.id == null) { // new task, call insert
			taskDBbinder.insertTask(task);
			return 0;
		} else { // Update
			taskDBbinder.updateTask(task);
			return 1;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
		case START_DATE_DIALOG_ID:
			if (task != null) {
				dialog = new DatePickerDialog(this, StartDateSetListener, task
						.getStart().get(Calendar.YEAR), task.getStart().get(
						Calendar.MONTH), task.getStart().get(
						Calendar.DAY_OF_MONTH));
			}
			break;
		case START_TIME_DIALOG_ID:
			if (task != null) {
				dialog = new TimePickerDialog(this, StartTimeSetListener, task
						.getStart().get(Calendar.HOUR_OF_DAY), task.getStart()
						.get(Calendar.MINUTE), true);
			}
			break;
		case DUE_DATE_DIALOG_ID:
			if (task != null) {
				dialog = new DatePickerDialog(this, DueDateSetListener, task
						.getDue().get(Calendar.YEAR), task.getDue().get(
						Calendar.MONTH), task.getDue().get(
						Calendar.DAY_OF_MONTH));
			}
			break;
		case DUE_TIME_DIALOG_ID:
			if (task != null) {
				dialog = new TimePickerDialog(this, DueTimeSetListener, task
						.getDue().get(Calendar.HOUR_OF_DAY), task.getDue().get(
						Calendar.MINUTE), true);
			}
			break;
		case INVALID_STARTTIME_DIALOG_ID:
			builder.setTitle(R.string.err_start_date);
			builder.setMessage(R.string.hint_start_date);
			builder.setPositiveButton(R.string.sl_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		case NO_NAME_DIALOG_ID:
			builder.setTitle(R.string.err_no_name);
			builder.setMessage(R.string.hint_no_name);
			builder.setPositiveButton(R.string.sl_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		}
		return dialog;
	}

	/**
	 * Hide Softkeyboard, called from ButtonListeners
	 */
	private void hideSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		IBinder binder = getCurrentFocus().getWindowToken();
		imm.hideSoftInputFromWindow(binder, 0);
	}
} // end class
