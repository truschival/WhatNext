/******************************************************************************
 * \filename TaskListActivity.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Main Application, entry point and storage for global data
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/

package de.ruschival.WhatNext.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.ruschival.WhatNext.Category;
import de.ruschival.WhatNext.IntentConstants;
import de.ruschival.WhatNext.LeastLaxityScheduler;
import de.ruschival.WhatNext.ListRunnable;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.Task;
import de.ruschival.WhatNext.TimeConversion;
import de.ruschival.WhatNext.RFC554.IvcalFieldTags;
import de.ruschival.WhatNext.RFC554.VcalConverter;
import de.ruschival.WhatNext.db.TaskDBService;
import de.ruschival.WhatNext.reminder.ReminderEventReceiver;

/**
 * @author Thomas Ruschival
 */
public class TaskListActivity extends Activity implements IntentConstants {
	public final String TAG = TaskListActivity.class.getCanonicalName();

	/** Numeric ID for dialog asking deletion */
	public static final int DIALOG_ID_DELETE = 0x200;
	/** Numeric ID for dialog selecting categories */
	public static final int DIALOG_ID_CATEGORIES = 0x300;

	/** RequestCode for PauseDialogActivity */
	public static final int PAUSE_REQ = 0x1000;
	/** RequestCode for TaskEditActivity */
	public static final int TASK_EDIT_REQ = 0x2000;

	/** Category Selection Dialog */
	private CategorySelectionDialog categoryDialog;

	/**
	 * Main list Elements
	 */
	private ImageButton newTaskBtn;
	private Button categoryBtn;
	private ListView listview;

	private final Handler uiThreadHandler = new Handler();
	private final TaskListUpdateRunnable uiListRunnable = new TaskListUpdateRunnable();

	/**
	 * Inner Class ViewHolder to minimize findByID() calls
	 */
	static class ViewHolder {
		TextView taskname;
		TextView due;
		TextView progress;
		TextView slack;
		ImageView taskStateBtn;
	}

	/**
	 * Button Listener to start/stop tasks
	 */
	private class StartButtonListener implements OnClickListener {
		private Task task;

		public StartButtonListener(Task task) {
			this.task = task;
		}

		@Override
		public void onClick(View v) {
			taskDBbinder.startTask(task);
			listadapter.notifyDataSetChanged();
		}
	}

	/**
	 * Button Listener to start/stop tasks
	 */
	private class StopButtonListener implements OnClickListener {
		private Task task;

		public StopButtonListener(Task task) {
			this.task = task;
		}

		public void onClick(View v) {
			Intent intent = new Intent(TaskListActivity.this, PauseDialogActivity.class);
			intent.putExtra(Task.INTENT_EXTRA_TASKID, task.id);
			startActivityForResult(intent, PAUSE_REQ);
		}
	}

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
			// Log.v(TAG, "Disconnected from service");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder servicebinder) {
			// Log.v(TAG, "Connected to service");
			taskDBbinder = (TaskDBService.TaskDBServiceBinder) servicebinder;
			categoryDialog.taskDBbinder = taskDBbinder;
			/** update Category List */
			taskDBbinder.getCategoryList(categoryDialog.uiThreadHandler, categoryDialog.uiListRunnable);
			/** update Task List */
			taskDBbinder.getTaskList(uiThreadHandler, uiListRunnable);

		}
	};

	public class TaskListAdapter extends ArrayAdapter<Task> {
		/**
		 * Temporary filtered subset of all tasks
		 */
		private List<Task> filteredList;
		private Object filteredListLock = new Object();
		/**
		 * Internal container since we override List
		 */
		private List<Task> fullList;
		private Object fullListLock = new Object();

		/**
		 * Test Filter for States
		 */
		private ArrayList<Category> categoryFilter;

		/**
		 * Filter to retrieve subsets of tasks
		 */
		private TaskFilter filter;

		/**
		 * Boolean to allow for automatic updates if dataset changes
		 */
		private boolean mNotifyOnChange = true;

		/**
		 * Inner Class taking care of Filtering
		 */
		private class TaskFilter extends Filter {
			@Override
			protected FilterResults performFiltering(CharSequence filterString) {
				FilterResults results = new FilterResults();
				if (filterString == null) {
					List<Task> list = fullList;
					results.values = list;
					results.count = list.size();
				} else {
					final List<Task> values = fullList;
					final int count = values.size();
					final List<Task> newValues = new ArrayList<Task>(count);
					/** iterate over task set */
					for (Task task : values) {
						/** iterate over categoryFilter */
						for (Category cat : categoryFilter) {
							/** if task is already in newValues we can skip this */
							if (!newValues.contains(task) && task.getCategories().containsKey(cat.id)) {
								/**
								 * add the task if the category is within task's
								 * category list
								 */
								newValues.add(task);
							}
						}
					}
					Collections.sort(newValues, new LeastLaxityScheduler());
					results.values = newValues;
					results.count = newValues.size();
				}
				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence prefix, FilterResults results) {
				synchronized (filteredListLock) {
					filteredList = (List<Task>) results.values;
				}
				// Let the adapter know about the updated list
				if (results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		}

		/**
		 * Default Constructor
		 */
		public TaskListAdapter(Context ctx, int resId, int size) {
			super(ctx, resId);
			this.fullList = new ArrayList<Task>(size);
		}

		/**
		 * Also override this method
		 */
		public void setNotifyOnChange(boolean notifyOnChange) {
			super.setNotifyOnChange(notifyOnChange);
			this.mNotifyOnChange = notifyOnChange;
		}

		/**
		 * Adds the specified object at the end of the array. NOTE: Override all
		 * methods that operate on underlying collection since we implement our
		 * own
		 * 
		 * @param task
		 *            The task to add at the end of the array.
		 */
		@Override
		public void add(Task task) {
			synchronized (fullListLock) {
				fullList.add(task);
			}
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}

		/**
		 * Adds all tasks to the local list NOTE: Override all methods that
		 * operate on underlying collection since we implement our own
		 * 
		 * @param taskList
		 *            list of tasks to add
		 */
		public void addAll(List<Task> taskList) {
			synchronized (fullListLock) {
				boolean tmp = mNotifyOnChange;
				mNotifyOnChange = false;
				for (Task task : taskList) {
					fullList.add(task);
				}
				mNotifyOnChange = tmp;
			}
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}

		/**
		 * Inserts the sepcified object at the specified index in the array.
		 * NOTE: Override all methods that operate on underlying collection
		 * since we implement our own
		 * 
		 * 
		 * @param task
		 *            The object to insert into the array.
		 * @param index
		 *            The index at which the object must be inserted.
		 */
		@Override
		public void insert(Task task, int index) {
			synchronized (fullListLock) {
				fullList.add(index, task);
			}
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}

		/**
		 * Removes the specified object from the array. NOTE: Override all
		 * methods that operate on underlying collection since we implement our
		 * own
		 * 
		 * @param task
		 *            The task to remove.
		 */
		@Override
		public void remove(Task task) {
			synchronized (fullListLock) {
				fullList.remove(task);
			}
			synchronized (filteredListLock) {
				if (filteredList != null) {
					filteredList.remove(task);
				}
			}
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}

		/**
		 * Remove all elements from the list. NOTE: Override all methods that
		 * operate on underlying collection since we implement our own
		 */
		@Override
		public void clear() {
			synchronized (fullListLock) {
				fullList.clear();
			}
			synchronized (filteredListLock) {
				filteredList = null;
			}
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}

		/**
		 * Get Task-item at position NOTE: Override all methods that operate on
		 * underlying collection since we implement our own
		 * 
		 * @param index
		 *            position of item in arraylist
		 * @return task Task
		 */
		@Override
		public Task getItem(int index) {
			Task result;
			synchronized (filteredListLock) {
				if (filteredList != null) {
					result = filteredList.get(index);
				} else {
					synchronized (fullListLock) {
						result = fullList.get(index);
					}
				}
			}
			return result;
		}

		/**
		 * Get index of Task-item. NOTE: Override all methods that operate on
		 * underlying collection since we implement our own
		 * 
		 * @param task
		 *            Task object to look for
		 * @return integer position in array
		 */
		@Override
		public int getPosition(Task task) {
			int pos = 0;
			synchronized (filteredListLock) {
				if (filteredList != null) {
					pos = filteredList.indexOf(task);
				} else {
					synchronized (fullListLock) {
						pos = fullList.indexOf(task);
					}
				}
			}
			return pos;
		}

		/**
		 * Get number of items in list (not storage size) NOTE: Override all
		 * methods that operate on underlying collection since we implement our
		 * own
		 * 
		 * @return integer number of items in list
		 */
		@Override
		public int getCount() {
			int retval = 0;
			synchronized (filteredListLock) {
				if (filteredList != null) {
					retval = filteredList.size();
				} else {
					synchronized (fullListLock) {
						retval = fullList.size();
					}
				}
			}
			return retval;
		}

		/**
		 * Render view of a list item, called by framework
		 */
		@Override
		public View getView(int position, View viewContext, ViewGroup vg) {
			Context appctx = getContext().getApplicationContext();

			/**
			 * Instance of the ViewHolder
			 */
			ViewHolder viewProxy;

			// Recycle existing view if passed as parameter
			// This will save memory and time on Android
			// This only works if the base layout for all classes are the same
			View rowView = viewContext;
			if (rowView == null) {
				LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
				        Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.task_list_row, null);
				viewProxy = new ViewHolder();
				viewProxy.taskname = (TextView) rowView.findViewById(R.id.taskName);
				viewProxy.due = (TextView) rowView.findViewById(R.id.dueDate);
				viewProxy.progress = (TextView) rowView.findViewById(R.id.progress);
				viewProxy.slack = (TextView) rowView.findViewById(R.id.slack);
				viewProxy.taskStateBtn = (ImageView) rowView.findViewById(R.id.taskStateBtn);
				rowView.setTag(viewProxy);
			} else {
				viewProxy = (ViewHolder) rowView.getTag();
			}
			/** Get the task and its state for this listItem */
			Task task = getItem(position);
			Task.State state = task.getState();

			/** values */
			viewProxy.taskname.setText(task.name);
			if (task.getDue() != null) {
				long delta = task.getDue().getTimeInMillis() - System.currentTimeMillis();
				// TODO: only show time for "today"
				if (Math.abs(delta) >= TimeConversion.MS_TO_DAY / 2) {
					viewProxy.due.setText(DateFormat.format("dd.MM", task.getDue()));
				} else {
					viewProxy.due.setText(DateFormat.format("kk:mm", task.getDue()));
				}
			} else {
				viewProxy.due.setText("----");
			}
			/** progress */
			int prog = (int) (task.getProgress() * 100);
			viewProxy.progress.setText(String.format("%d", prog));

			/** Slack only for incomplete tasks with due date */
			if (state != Task.State.DONE && task.getDue() != null) {
				long slackTime = task.getLaxity(Calendar.getInstance().getTimeInMillis());
				int remainingWork = task.getRemainingWork();
				if (slackTime < (0.5 * remainingWork)) {
					viewProxy.slack.setTextColor(appctx.getResources().getColor(R.color.orange));
				} else if (slackTime < remainingWork) {
					viewProxy.slack.setTextColor(appctx.getResources().getColor(R.color.yellow));
				} else {
					viewProxy.slack.setTextColor(appctx.getResources().getColor(R.color.green));
				}

				String approxSlack = TimeConversion.getApproximateString(slackTime);
				viewProxy.slack.setText(approxSlack);

			} else {
				viewProxy.slack.setVisibility(View.INVISIBLE);
			}

			/** Change Button Image and Text Appearance accordingly */
			viewProxy.taskStateBtn.setEnabled(true);

			switch (state) {
			case RUNNING:
				viewProxy.taskStateBtn.setImageResource(R.drawable.pause);
				viewProxy.taskname.setTextColor(appctx.getResources().getColor(R.color.lightgrey));
				viewProxy.taskname.setTextAppearance(appctx, R.style.TaskNameItalic);
				viewProxy.taskStateBtn.setOnClickListener(new StopButtonListener(task));
				break;
			case READY:
				viewProxy.taskStateBtn.setImageResource(R.drawable.play);
				viewProxy.taskname.setTextColor(appctx.getResources().getColor(R.color.lightgrey));
				viewProxy.taskname.setTextAppearance(appctx, R.style.TaskName);
				viewProxy.taskStateBtn.setOnClickListener(new StartButtonListener(task));
				break;
			case DONE:
				viewProxy.taskStateBtn.setImageResource(R.drawable.done);
				viewProxy.taskStateBtn.setEnabled(false);
				viewProxy.taskname.setTextColor(appctx.getResources().getColor(R.color.lightgrey));
				viewProxy.taskname.setTextAppearance(appctx, R.style.TaskName);
				break;
			case OVERDUE:
				viewProxy.taskStateBtn.setImageResource(R.drawable.play_red);
				viewProxy.taskname.setTextColor(appctx.getResources().getColor(R.color.red));
				viewProxy.slack.setTextColor(appctx.getResources().getColor(R.color.red));
				viewProxy.taskname.setTextAppearance(appctx, R.style.TaskName);
				viewProxy.taskStateBtn.setOnClickListener(new StartButtonListener(task));
				break;
			case RUNNING_OVERDUE:
				viewProxy.taskStateBtn.setImageResource(R.drawable.ic_alert);
				viewProxy.taskname.setTextColor(appctx.getResources().getColor(R.color.red));
				viewProxy.taskname.setTextAppearance(appctx, R.style.TaskNameItalic);
				viewProxy.slack.setTextColor(appctx.getResources().getColor(R.color.red));
				viewProxy.taskStateBtn.setOnClickListener(new StopButtonListener(task));
				break;
			case FUTURE:
				viewProxy.taskStateBtn.setImageResource(R.drawable.play);
				viewProxy.taskname.setTextColor(appctx.getResources().getColor(R.color.darkgrey));
				viewProxy.taskname.setTextAppearance(appctx, R.style.TaskName);
				viewProxy.taskStateBtn.setOnClickListener(new StartButtonListener(task));
				break;
			}

			/** Change Background */
			if (prog >= 0 && prog < 25) {
				viewProxy.taskStateBtn.setBackgroundResource(R.drawable.progressbackground_00);
			}
			if (prog >= 25 && prog < 50) {
				viewProxy.taskStateBtn.setBackgroundResource(R.drawable.progressbackground_25);
			}
			if (prog >= 50 && prog < 75) {
				viewProxy.taskStateBtn.setBackgroundResource(R.drawable.progressbackground_50);
			}
			if (prog >= 75 && prog <= 99) {
				viewProxy.taskStateBtn.setBackgroundResource(R.drawable.progressbackground_100);
			}

			/** mark as complete */
			if (prog == 100) {
				viewProxy.taskStateBtn.setBackgroundResource(R.drawable.progressbackground_00);
			}
			return rowView;
		}

		/**
		 * Sort the task set according to scheduler rules
		 */
		public void reschedule() {
			if (filteredList != null) {
				synchronized (filteredListLock) {
					Collections.sort(filteredList, new LeastLaxityScheduler());
				}
			} else {
				synchronized (fullListLock) {
					Collections.sort(fullList, new LeastLaxityScheduler());
				}
			}
		}

		/**
		 * Return Filter of TaskList
		 * 
		 * @return filter
		 */
		@Override
		public Filter getFilter() {
			if (filter == null) {
				filter = new TaskFilter();
			}
			return filter;
		}

		/**
		 * Update FilterElements
		 * 
		 * @param list
		 *            task-categories to include in result set
		 */
		public void setFilterItems(ArrayList<Category> list) {
			this.categoryFilter = list;
		}
	}

	/**
	 * Currently selected task in list
	 */
	private Task selectedTask;

	/**
	 * List adapter for activity
	 */
	private TaskListAdapter listadapter;

	/**
	 * Runnable Class to allow asynchronous update of ListView
	 * 
	 * @author ruschi
	 */
	public class TaskListUpdateRunnable extends ListRunnable<Task> {
		@Override
		public void run() {
			listadapter.setNotifyOnChange(false);
			listadapter.clear();
			listadapter.addAll(content);
			updateCategories();
			checkLaxity();
			listadapter.setNotifyOnChange(true);
			listadapter.notifyDataSetChanged();
		}
	}

	/**
	 * Called when the activity is first created.
	 * */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_list);
		listview = (ListView) findViewById(R.id.listView);
		listadapter = new TaskListAdapter(this, R.layout.task_list_row, 10);
		listview.setAdapter(listadapter);
		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View clickedView, int position, long id) {
				Intent intent = new Intent(TaskListActivity.this, TaskShowActivity.class);
				long taskid = listadapter.getItem(position).id;
				intent.putExtra(Task.INTENT_EXTRA_TASKID, taskid);
				startActivityForResult(intent, ITC_EDIT_TASK);
			}

		});

		/** PlusButton */
		newTaskBtn = (ImageButton) findViewById(R.id.newTaskBtn);
		/** new Task on Plus-Button */
		newTaskBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
				startActivityForResult(intent, ITC_NEW_TASK);
			}
		});

		/** category Dialog */
		categoryDialog = new CategorySelectionDialog(this, false);
		categoryBtn = (Button) findViewById(R.id.categoryBtn);
		categoryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				categoryDialog.show();
			}
		});
		categoryDialog.getSaveButton().setText(R.string.sa_filter);
		categoryDialog.getSaveButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateCategories();
				categoryDialog.listadapter.notifyDataSetChanged();
				categoryDialog.dismiss();
			}
		});

		/** Context menu on tasklist items */
		registerForContextMenu(listview);

		if (savedInstanceState != null) {
			long[] marked = savedInstanceState.getLongArray("categories");
			categoryDialog.setSelectedItemIDs(marked);
		}
		/** Start the reminder service */
		Intent myIntent = new Intent(ReminderEventReceiver.REMINDER_EVENT_ACTION);
		sendBroadcast(myIntent);

	}

	/**
	 * Build string for categories button
	 */
	public void updateCategories() {
		/** StringBuilder for ButtonText */
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		/** Get original array of categories */
		ArrayList<Category> categorylist = taskDBbinder.getCategories();
		/** new filter for Adapter */
		ArrayList<Category> filterElements = new ArrayList<Category>(2);

		/** Get Checked Categories */
		SparseBooleanArray checkedCategories = categoryDialog.getCategoryList().getCheckedItemPositions();

		for (int i = 0; i < categorylist.size(); i++) {
			if (checkedCategories.get(i)) {
				if (first) {
					first = false;
				} else {
					builder.append(",");
				}
				filterElements.add(categorylist.get(i));
				builder.append(categorylist.get(i).name);
			}
		}
		listadapter.setFilterItems(filterElements);

		if (filterElements.isEmpty()) {
			// Reset Filter
			listadapter.getFilter().filter(null);
			categoryBtn.setText(R.string.sa_choose_categories);
		} else {
			// Apply Filter
			listadapter.getFilter().filter("filter");
			categoryBtn.setText(builder);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		listadapter.clear();
		// Log.v(TAG, "onResume()");
		final Intent intent = new Intent(this, TaskDBService.class);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		// Log.v(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		// Log.v(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		/** Get Checked Categories */
		int listlen = categoryDialog.getCategoryList().getCount();

		SparseBooleanArray checkedCategories = categoryDialog.getCategoryList().getCheckedItemPositions();
		/** Get original array of categories */
		long[] marked = new long[checkedCategories.size()];
		int j = 0;
		for (int i = 0; i < listlen; i++) {
			if (checkedCategories.get(i)) {
				marked[j++] = ((Category) categoryDialog.getCategoryList().getItemAtPosition(i)).id;
			}
		}
		outState.putLongArray("categories", marked);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.opt_m_tasklist, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.opti_new:
			Toast.makeText(this, R.string.sa_new_task, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
			startActivityForResult(intent, ITC_NEW_TASK);
			return true;
			// case R.id.menu_i_share:

			// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ctx_m_task, menu);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		selectedTask = listadapter.getItem(info.position);
		if (selectedTask != null) {
			// Default, enable visibility
			menu.findItem(R.id.ctxi_run).setVisible(true);
			menu.findItem(R.id.ctxi_pause).setVisible(true);

			switch (selectedTask.getState()) {
			case RUNNING:
			case RUNNING_OVERDUE:
				menu.findItem(R.id.ctxi_run).setVisible(false);
				break;
			case DONE:
				menu.findItem(R.id.ctxi_run).setVisible(false);
				menu.findItem(R.id.ctxi_pause).setVisible(false);
				menu.findItem(R.id.ctxi_mcomplete).setEnabled(false);
				break;
			case READY:
			case OVERDUE:
				menu.findItem(R.id.ctxi_pause).setVisible(false);
				break;
			case FUTURE:
				menu.findItem(R.id.ctxi_pause).setVisible(false);
				break;
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		/** intent for dispatching new activities */
		Intent intent;

		if (selectedTask != null) {
			switch (item.getItemId()) {
			case R.id.ctxi_run:
				if (taskDBbinder.startTask(selectedTask)) {
					Toast.makeText(this, R.string.sa_run, Toast.LENGTH_SHORT).show();
					listadapter.reschedule();
					listadapter.notifyDataSetChanged();
				}
				break;
			case R.id.ctxi_edit:
				Intent i = new Intent(this, TaskEditActivity.class);
				i.putExtra(Task.INTENT_EXTRA_TASKID, selectedTask.id);
				startActivity(i);
				break;
			case R.id.ctxi_pause:
				intent = new Intent(this, PauseDialogActivity.class);
				intent.putExtra(Task.INTENT_EXTRA_TASKID, selectedTask.id);
				startActivityForResult(intent, PAUSE_REQ);
				break;
			case R.id.ctxi_mcomplete:
				taskDBbinder.markComplete(selectedTask);
				Toast.makeText(this, R.string.sa_complete, Toast.LENGTH_SHORT).show();
				listadapter.reschedule();
				listadapter.notifyDataSetChanged();
				break;
			case R.id.ctxi_delete:
				showDialog(DIALOG_ID_DELETE);
				break;
			case R.id.ctxi_share:
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType(IvcalFieldTags.MIME_TYPE);
				String filename = selectedTask.name.replaceAll(" ", "_") + IvcalFieldTags.FILE_EXTENSION;
				File file = new File(getExternalFilesDir(null), filename);
				Uri absoluteFileUri = Uri.fromFile(file);
				try {
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(VcalConverter.getTaskiCal(selectedTask).getBytes());
					fos.close();
					intent.putExtra(Intent.EXTRA_STREAM, absoluteFileUri);
					startActivity(Intent.createChooser(intent, getString(R.string.sa_share)));
				} catch (Exception exc) {
					Toast.makeText(this, "Exception", Toast.LENGTH_SHORT).show();
				}
				break;
			default:
			}
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
			builder.setPositiveButton(R.string.sl_yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (selectedTask != null) {
						taskDBbinder.deleteTask(selectedTask);
						listadapter.remove(selectedTask);
						listadapter.sort(new LeastLaxityScheduler());
						selectedTask = null;
					}
					dialog.dismiss();
				}
			});
			builder.setNegativeButton(R.string.sl_no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			return builder.create();
		default:
			return null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case PAUSE_REQ:
				listadapter.notifyDataSetChanged();
				break;
			case TASK_EDIT_REQ:
				listadapter.notifyDataSetChanged();
				break;
			default:
			}
		}
	}

	/**
	 * Most simple Check if at least topmost item is schedulable
	 */
	public void checkLaxity() {
		if (listadapter.getCount() > 0) {
			Task task = listadapter.getItem(0);
			if (task.getLaxity(System.currentTimeMillis()) <= 0) {
				Toast.makeText(this, R.string.err_will_miss_deadline, Toast.LENGTH_SHORT).show();
			}
		}
	}
}