/******************************************************************************
 * \filename TaskDBAdapter.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief Proxy-object for Database accesses
 * 
 * Originally created on Oct 19, 2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                        $
 * $LastChangedDate:: 2012-08-14 18:42:08 -0300 (Tue, 14 Aug 2012#$
 * $Revision:: 63                                                 $
 *-----------------------------------------------------------------------------  
 */
package de.ruschival.WhatNext.db;

import java.util.ArrayList;
import java.util.Collections;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import de.ruschival.WhatNext.Category;
import de.ruschival.WhatNext.LeastLaxityScheduler;
import de.ruschival.WhatNext.ListRunnable;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.Reminder;
import de.ruschival.WhatNext.Task;
import de.ruschival.WhatNext.WidgetDataService;

/**
 * @author ruschi Proxy-object for Database accesses
 * @version $Revision:: 63 $
 */
public class TaskDBService extends Service {
    private final IBinder taskDBServiceBinder = new TaskDBServiceBinder();

    public class TaskDBServiceBinder extends Binder implements ITaskDBService {
        @Override
        public ArrayList<Task> getTasks() {
            return _getTasks();
        }

        @Override
        public Task getTaskByID(long id) {
            return _getTaskByID(id);
        }

        @Override
        public void deleteTask(Task task) {
            if (task != null){
                _deleteTask(task);
            }
        }

        @Override
        public void insertTask(Task task) {
            if (task != null){
                _insertTaskMT(task);
            }
        }

        @Override
        public void updateTask(Task task) {
            if (task != null){
                _updateTaskMT(task);
            }
        }

        @Override
        public boolean startTask(Task task) {
            if (task != null){
                return _startTask(task);
            } else{
                return false;
            }
        }

        @Override
        public boolean stopTask(Task task, long delta) {
            if (task != null){
                return _stopTask(task, delta);
            } else{
                return false;
            }
        }

        @Override
        public boolean markComplete(Task task) {
            if (task != null){
                return _markComplete(task);
            } else{
                return false;
            }
        }

        @Override
        public void suspendTask(Task task, boolean suspend) {
            if (task != null){
                _suspendTask(task, suspend);
            }
        }

        @Override
        public void getTaskList(Handler receiver, ListRunnable<Task> callback) {
            _getTaskListMT(receiver, callback);
        }

        @Override
        public ArrayList<Category> getCategories() {
            return _getCategories();
        }

        @Override
        public void getCategoryList(Handler receiver, ListRunnable<Category> callback) {
            _getCategoryListMT(receiver, callback);
        }

        @Override
        public void insertCategory(Category category) {
            _insertCategoryMT(category);
        }

        @Override
        public void updateCategory(Category category) {
            _updateCategoryMT(category);
        }

        @Override
        public void deleteCategory(Category category) {
            _deleteCategoryMT(category);
        }

        @Override
        public ArrayList<Reminder> getReminders(long taskID) {
            return _getReminders(taskID);
        }

        @Override
        public void deleteReminder(long reminderID) {
            _deleteReminder(reminderID);
        }

        @Override
        public long insertReminder(Reminder reminder) {
            return _insertReminder(reminder);
        }

        @Override
        public ArrayList<Reminder> getPendingReminders(int delta) {
            return _getPendingReminders(delta);
        }

        @Override
        public ArrayList<Task> getTaskOverview() {
            return _getTaskOverview();
        }

    }

    /**
     * Internal class taking care for correct database instantiation
     */
    private class TaskDB extends SQLiteOpenHelper {
        @SuppressWarnings("unused")
        public final String TAG = TaskDB.class.getSimpleName();
        /**
         * Database Information (file name)
         */
        public static final String DB_NAME = "WhatNext.db";
        /**
         * Database information (version)
         */
        public static final int DB_VERSION = 6;

        /**
         * Default Constructor
         */
        public TaskDB(Context ctx) {
            super(ctx, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TaskTable.STMT_CREATE);
            db.execSQL(CategoryTable.STMT_CREATE);
            db.execSQL(TaskCategoryTable.STMT_CREATE);
            db.execSQL(ReminderTable.STMT_CREATE);

            String categories[] = getApplicationContext().getResources().getStringArray(
                    R.array.DefaultCategories);

            for (String cat : categories){
                String stmt = " INSERT into " + CategoryTable.TABLE_NAME + "("
                        + CategoryTable.COL_NAME + ") values ('" + cat + "')";
                db.execSQL(stmt);
            }
        }

        /**
         * Will be executed on schema update and unfortunately destroys data TODO: write
         * import/export of existing data!
         */
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 6){ // TODO: CHECK for future releases
                db.execSQL("ALTER TABLE " + TaskTable.TABLE_NAME + " ADD COLUMN "
                        + TaskTable.COL_PARENT);
            } else{
                /** Drop old table */
                db.execSQL(TaskTable.STMT_DROP);
                db.execSQL(CategoryTable.STMT_DROP);
                db.execSQL(TaskCategoryTable.STMT_DROP);
                db.execSQL(ReminderTable.STMT_DROP);
                /** Initialize new Table */
                db.execSQL(TaskTable.STMT_CREATE);
                db.execSQL(CategoryTable.STMT_CREATE);
                db.execSQL(TaskCategoryTable.STMT_CREATE);
                db.execSQL(ReminderTable.STMT_CREATE);
            }
        }
    }

    /**
     * database instance
     */
    private TaskDB dbInstance;

    /**
     * Open database Connection
     */
    private SQLiteDatabase db;

    /**
     * cached version of all Categories
     */
    private ArrayList<Category> categories;
    private Object catLock = new Object();

    /**
     * Cursor of last getTasks()
     */
    private Cursor cur_Taskset;

    /**
     * Precompiled insert statement for full task information
     */
    private SQLiteStatement stmtTaskInsert;

    /**
     * Precompiled insert statement for full task information
     */
    private SQLiteStatement stmtTaskUpdate;

    /**
     * Precompiled insert statement for category
     */
    private SQLiteStatement stmtCategoryInsert;

    /**
     * Precompiled insert statement for mapping task<-> Category
     */
    private SQLiteStatement stmtInsertMapping;

    /**
     * Precompiled statement for deletion of mapping task<-> Category (by task id)
     */
    private SQLiteStatement stmtDeleteMappingTaskID;
    /**
     * Precompiled statement for deletion of mapping task<-> Category (by category id)
     */
    private SQLiteStatement stmtDeleteMappingCatID;

    /**
     * Constructor, build the Database and open a connection
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // allTasks = new ArrayList<Task>(10);
        categories = new ArrayList<Category>(5);
        /** open database connection */
        getDBconnection();
        precacheDataMT();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return taskDBServiceBinder;
    }

    /**
     * Helper Function to built data objects from Cursor
     * 
     * @param cur
     *            "Select EDIT_COLUMNS from TaskTable"
     * @return task from cursor
     */
    private Task buildTaskFromCursorAtPosition(Cursor cur) {
        Task task = null;
        /** Build task with basic information */
        task = new Task(cur.getLong(TaskTable.IDX_ID), cur.getString(TaskTable.IDX_NAME),
                cur.getInt(TaskTable.IDX_PRIO), cur.getInt(TaskTable.IDX_STATE),
                cur.getFloat(TaskTable.IDX_PROG), cur.getLong(TaskTable.IDX_WCET),
                cur.getLong(TaskTable.IDX_ACT), cur.getLong(TaskTable.IDX_RES),
                cur.getLong(TaskTable.IDX_START), cur.getLong(TaskTable.IDX_DUE));

        /** Add Information */
        task.desc = cur.getString(TaskTable.IDX_DESC);
        task.location = cur.getString(TaskTable.IDX_LOC);

        /** Synchronization information */
        task.dtstamp = cur.getLong(TaskTable.IDX_CREA);
        task.lastmodified = cur.getLong(TaskTable.IDX_LAST);

        return task;
    }

    private void getCategoriesForTask(Task task) {
        /** Mark CategoryIDs in Task */
        Cursor categoryCursor = db.query(TaskCategoryTable.TABLE_NAME,
                new String[] { TaskCategoryTable.COL_CID }, TaskCategoryTable.COL_TID + "='"
                        + task.id + "'", null, null, null, TaskCategoryTable.COL_CID);
        /** Add category if it exists */
        while (categoryCursor.moveToNext()){
            long catid = categoryCursor.getLong(0);
            synchronized (catLock){
                for (Category category : categories){
                    if (category.id == catid){
                        task.getCategories().put(category.id, category);
                        break;
                    }
                }
            }
        }
        categoryCursor.close();
    }

    /**
     * Delete task entries from category table
     * 
     * @param taskid
     */
    private void deleteTaskCategoryMapping(Long taskid) {
        /** Clear all categories */
        if (stmtDeleteMappingTaskID == null){
            stmtDeleteMappingTaskID = db
                    .compileStatement(TaskCategoryTable.STMT_DELETE_MAPPING_TASK_ID);
        }
        stmtDeleteMappingTaskID.bindLong(1, taskid);
        stmtDeleteMappingTaskID.execute();
    }

    /**
     * Retrieve all reminders for given task from table
     * 
     * @param taskID
     * @return list of reminders
     */
    private ArrayList<Reminder> _getReminders(long taskID) {
        ArrayList<Reminder> list = new ArrayList<Reminder>();
        Cursor cur = db.query(ReminderTable.TABLE_NAME, ReminderTable.ALL_COLUMNS, new String(
                ReminderTable.COL_TID + "='" + taskID + "'"), null, null, null,
                ReminderTable.COL_DELTA, null);
        while (cur.moveToNext()){
            list.add(new Reminder(cur.getLong(ReminderTable.IDX_ID), cur
                    .getLong(ReminderTable.IDX_TID), cur.getInt(ReminderTable.IDX_DELTA), cur
                    .getInt(ReminderTable.IDX_STATE)));
        }
        cur.close();
        return list;
    }

    /**
     * Create a new reminder record in the database
     * 
     * @param reminder
     * @return
     */
    private long _insertReminder(Reminder reminder) {
        ContentValues content = new ContentValues();
        content.put(ReminderTable.COL_TID, reminder.getTaskId());
        content.put(ReminderTable.COL_DELTA, reminder.getDelta());
        content.put(ReminderTable.COL_STATE, reminder.getState());
        long id = db.insert(ReminderTable.TABLE_NAME, null, content);
        reminder.setId(id);
        return id;
    }

    /**
     * Delete the given reminder from table
     * 
     * @param reminderID
     */
    private void _deleteReminder(long reminderID) {
        db.delete(ReminderTable.TABLE_NAME, ReminderTable.COL_ID + " = '" + reminderID + "'", null);
    }

    /**
     * Retrieve Reminders within the next delta ms to be dispatched by NotificationManager
     * 
     * @param delta
     *            usually updateFrequency of service
     * @return
     */
    public ArrayList<Reminder> _getPendingReminders(int delta) {
        ArrayList<Reminder> reminders = new ArrayList<Reminder>();
        long now = System.currentTimeMillis();
        long lower = now - delta;

        final String query = String.format("SELECT " + " rem." + ReminderTable.COL_ID + " , "
                + " rem." + ReminderTable.COL_TID + " , " + " rem." + ReminderTable.COL_DELTA
                + " as delta, " + " rem." + ReminderTable.COL_STATE + " as state, " + " tsk."
                + TaskTable.COL_NAME + " , " + " tsk." + TaskTable.COL_PROG + " as prog, "
                + " tsk." + TaskTable.COL_DUE + " as due " + " FROM " + ReminderTable.TABLE_NAME
                + " rem ," + TaskTable.TABLE_NAME + " tsk " + " WHERE rem.state < 2  "
                + " AND prog < 1.0 " + " AND tsk." + TaskTable.COL_ID + "= rem."
                + ReminderTable.COL_TID + " AND due-delta <= %d" + " AND due-delta > %d ", now,
                lower);

        Cursor cur = db.rawQuery(query, null);

        while (cur.moveToNext()){
            Reminder rem = new Reminder(cur.getLong(0), cur.getLong(1), cur.getInt(2),
                    cur.getInt(3));
            rem.taskName = cur.getString(4);

            reminders.add(rem);
        }
        cur.close();
        return reminders;
    }

    /**
     * Allows access to database for query (and manipulation if desired)
     * 
     * @return writable database connection
     * @throws SQLiteException
     */
    private SQLiteDatabase getDBconnection() throws SQLiteException {
        /** Normal case */
        if (db != null && db.isOpen()){
            return db;
        } else{
            if (dbInstance == null){
                dbInstance = new TaskDB(this);
            }
            if (db == null){
                db = dbInstance.getWritableDatabase();
            }
        }
        return db;
    }

    /**
     * Build an ArrayList of all task in Table Select EDIT_COLUMNS from TaskTable does not spawn a
     * new Thread
     * 
     * @return ArrayList<Task> - all tasks from table (sorted by least Laxity)
     */
    private ArrayList<Task> _getTasks() {
        ArrayList<Task> allTasks = new ArrayList<Task>(5);

        cur_Taskset = db.query(TaskTable.TABLE_NAME, TaskTable.ALL_COLUMNS, null, null, null, null,
                null);
        while (cur_Taskset.moveToNext()){
            Task task = buildTaskFromCursorAtPosition(cur_Taskset);
            /** retrieve categories for this task */
            getCategoriesForTask(task);
            /** Retrieve Reminders */
            task.setReminders(_getReminders(task.id));
            allTasks.add(task);
        }
        cur_Taskset.close();

        Collections.sort(allTasks, new LeastLaxityScheduler());
        return allTasks;
    }

    /**
     * Build an ArrayList of all task in Table Select EDIT_COLUMNS from TaskTable does not spawn a
     * new Thread
     * 
     * @return ArrayList<Task> - all tasks from table (sorted by least Laxity)
     */
    private ArrayList<Task> _getTaskOverview() {
        ArrayList<Task> allTasks = new ArrayList<Task>(5);
        String selection = TaskTable.COL_PROG + " < '1.0' ";

        Cursor cur = db.query(TaskTable.TABLE_NAME, TaskTable.ALL_COLUMNS, selection, null, null,
                null, null);
        while (cur.moveToNext()){
            Task task = buildTaskFromCursorAtPosition(cur);
            allTasks.add(task);
        }
        cur.close();

        Collections.sort(allTasks, new LeastLaxityScheduler());
        return allTasks;
    }

    /**
     * Update a task<-> CategoryMapping by deleting and inserting new values
     * 
     * @param task
     */
    private void insertTaskCategoryMapping(Task task) {
        /** insert Categories */
        if (stmtInsertMapping == null){
            stmtInsertMapping = db.compileStatement(TaskCategoryTable.STMT_INSERT_MAPPING);
        }
        for (Category cat : task.getCategories().values()){
            stmtInsertMapping.bindLong(1, task.id);
            stmtInsertMapping.bindLong(2, cat.id);
            stmtInsertMapping.execute();
            stmtInsertMapping.clearBindings();
        }
    }

    private void precacheDataMT() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (catLock){
                    /** Fill Categories */
                    _getCategories();
                }
                // synchronized (taskLock) {
                // /** fill Tasks */
                // _getTasks();
                // }
            }
        }).start();
    }

    /**
     * Delete Category from Table
     * 
     * @param category
     *            to be removed
     */
    private void _deleteCategoryMT(final Category category) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (category != null){
                    db.delete(CategoryTable.TABLE_NAME, CategoryTable.COL_ID + "='" + category.id
                            + "'", null);
                    /** Clear all categories */
                    if (stmtDeleteMappingCatID == null){
                        stmtDeleteMappingCatID = db
                                .compileStatement(TaskCategoryTable.STMT_DELETE_MAPPING_CAT_ID);
                    }
                    stmtDeleteMappingCatID.bindLong(1, category.id);
                    stmtDeleteMappingCatID.execute();
                    /* add category to local list */
                    synchronized (catLock){
                        categories.remove(category);
                    }
                }
            }
        }).start();
    }

    /**
     * Delete a single task from table
     * 
     * @param task
     *            Task to remove
     */
    private void _deleteTask(Task task) {
        if (task != null){
            // delete mapping //
            deleteTaskCategoryMapping(task.id);
            db.delete(TaskTable.TABLE_NAME, TaskTable.COL_ID + "='" + task.id + "'", null);
            db.delete(ReminderTable.TABLE_NAME, ReminderTable.COL_TID + "='" + task.id + "'", null);
            updateWidgets();
        }
    }

    /**
     * Retrieve the complete list of categories form database Runs in caller thread
     * 
     * @return ArrayList of categories
     */
    private ArrayList<Category> _getCategories() {
        synchronized (catLock){
            if (categories.isEmpty()){
                Cursor cur = db.query(CategoryTable.TABLE_NAME, CategoryTable.ALL_COLUMNS, null,
                        null, null, null, null);
                while (cur.moveToNext()){
                    Category cat = new Category(cur.getLong(CategoryTable.IDX_ID),
                            cur.getString(CategoryTable.IDX_NAME));
                    categories.add(cat);
                }
                cur.close();
            }
        }
        return categories;
    }

    /**
     * Spawn a new Thread and retrieve a sorted list of tasks. The list will be be sorted and placed
     * in callbacks.content
     * 
     * @param receiver
     *            Handler object or caller thread
     * @param callback
     *            Runnable object of caller thread
     */
    private void _getCategoryListMT(final Handler receiver, final ListRunnable<Category> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (categories){
                    callback.content = new ArrayList<Category>(_getCategories());
                }
                receiver.post(callback);
            }
        }).start();
    }

    /**
     * Retrieve a record from database, create a Task object and return it to the caller based
     * 
     * @param id
     *            - unique id (primary key in database)
     * @return task from ArrayList with matching ID
     */
    private Task _getTaskByID(long id) {
        Task task = null;
        cur_Taskset = db.query(TaskTable.TABLE_NAME, TaskTable.ALL_COLUMNS, TaskTable.COL_ID
                + " = '" + id + "' ", null, null, null, null);
        if (cur_Taskset.moveToFirst()){
            task = buildTaskFromCursorAtPosition(cur_Taskset);
            getCategoriesForTask(task);
            task.setReminders(_getReminders(task.id));
        }
        cur_Taskset.close();
        return task;
    }

    /**
     * Spawn a new Thread and retrieve a sorted list of tasks. The list will be be sorted and placed
     * in callbacks.content
     * 
     * @param receiver
     *            Handler object or caller thread
     * @param callback
     *            Runnable object of caller thread
     */
    private void _getTaskListMT(final Handler receiver, final ListRunnable<Task> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                callback.content = new ArrayList<Task>(_getTasks());
                receiver.post(callback);
            }
        }).start();
    }

    /**
     * Insert a category as record in database. The category Object is not Created in this class or
     * Database, thus we add it to our list
     * 
     * @param category
     *            new category
     */
    private void _insertCategoryMT(final Category category) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (stmtCategoryInsert == null){
                    stmtCategoryInsert = db.compileStatement(CategoryTable.STMT_INSERT_CATEGORY);
                }
                stmtCategoryInsert.bindString(1, category.name);
                stmtCategoryInsert.bindLong(2, category.color);
                category.id = stmtCategoryInsert.executeInsert();
                /* add category to local list */
                synchronized (catLock){
                    categories.add(category);
                }
            }
        }).start();
    }

    /**
     * Insert a task object as record in database. The tasks id is updated Spawns a new Thread
     * 
     * @param task
     *            Task to insert
     */
    private void _insertTaskMT(final Task task) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (stmtTaskInsert == null){
                    stmtTaskInsert = db.compileStatement(TaskTable.STMT_INSERT_TASK);
                }
                stmtTaskInsert.bindString(1, task.name);
                stmtTaskInsert.bindLong(2, task.getPriorityOrdinal());
                int suspended = task.isSuspended() ? 1 : 0;
                stmtTaskInsert.bindLong(3, suspended);

                stmtTaskInsert.bindDouble(4, task.getProgress());
                stmtTaskInsert.bindLong(5, task.getWcet());
                stmtTaskInsert.bindLong(6, task.getActual());

                stmtTaskInsert.bindLong(7, task.getStart().getTimeInMillis());
                long due = 0L;
                if (task.getDue() != null){
                    due = task.getDue().getTimeInMillis();
                }
                stmtTaskInsert.bindLong(8, due);
                /* additional info */
                stmtTaskInsert.bindString(9, task.desc);
                stmtTaskInsert.bindString(10, task.location);

                task.id = stmtTaskInsert.executeInsert();
                // synchronized (taskLock) {
                // allTasks.add(task);
                // }
                /** update categories */
                insertTaskCategoryMapping(task);
                /** insert Reminders */
                for (Reminder reminder : task.getReminders()){
                    reminder.setTaskId(task.id);
                    _insertReminder(reminder);
                }
                updateWidgets();
            }
        }).start();
    }

    /**
     * Shortcut to set progress to 100% and update actual if task was running calls
     * Task.markComplete and updates DB record
     * 
     * @param task
     * @return true
     */
    private boolean _markComplete(Task task) {
        boolean retval = task.markComplete();
        ContentValues content = new ContentValues();
        content.put(TaskTable.COL_PROG, 1.0f);
        content.put(TaskTable.COL_STATE, 0);
        if (retval){
            // if task was running, clear resumed time stamp and write actual
            content.put(TaskTable.COL_RES, 0);
            content.put(TaskTable.COL_ACT, task.getActual());
        }
        db.update(TaskTable.TABLE_NAME, content, TaskTable.COL_ID + "='" + task.id + "'", null);
        updateWidgets();
        return retval;
    }

    /**
     * Start execution of a task and update the resumed field in table
     * 
     * @param task
     *            Task to update
     * @return true if task could be started (was not running)
     */
    private boolean _startTask(Task task) {
        if (task.start()){
            ContentValues content = new ContentValues();
            content.put(TaskTable.COL_RES, task.resumed);
            content.put(TaskTable.COL_STATE, 0);
            db.update(TaskTable.TABLE_NAME, content, TaskTable.COL_ID + "='" + task.id + "'", null);
            updateWidgets();
            return true;
        } else{
            return false;
        }
    }

    /**
     * Stop execution of a task and clear the resumed field in table
     * 
     * @param task
     * @param delta
     *            - time in ms to add to actual
     * @return true if task could be stopped (was running)
     */
    private boolean _stopTask(Task task, long delta) {
        if (task.stopAndUpdate(delta)){
            ContentValues content = new ContentValues();
            content.put(TaskTable.COL_RES, 0);
            if (delta >= 0){
                content.put(TaskTable.COL_ACT, task.getActual());
                content.put(TaskTable.COL_PROG, task.getProgress());
            }
            db.update(TaskTable.TABLE_NAME, content, TaskTable.COL_ID + "='" + task.id + "'", null);
            updateWidgets();
            return true;
        } else{
            return false;
        }
    }

    /**
     * Set/clear the suspended flag in the given task will call stop if the task was running
     * 
     * @param task
     * @param suspend
     *            flag task as suspended
     */
    private void _suspendTask(Task task, boolean suspend) {
        ContentValues content = new ContentValues();
        if (suspend && task.resumed > 0){
            _stopTask(task, 0);
        }
        task.suspend(suspend);
        int suspended = suspend ? 1 : 0;
        content.put(TaskTable.COL_STATE, suspended);
        db.update(TaskTable.TABLE_NAME, content, TaskTable.COL_ID + "='" + task.id + "'", null);
        updateWidgets();
    }

    /**
     * update category in table and local list. We are not sure if the caller has the SAME
     * object(-references) in his list, thus we look it up in our list and update it
     * 
     * @param category
     *            object to update the name
     */
    private void _updateCategoryMT(final Category category) {
        if (category.id != null && category.name.length() != 0){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ContentValues content = new ContentValues();
                    content.put(CategoryTable.COL_NAME, category.name);
                    db.update(CategoryTable.TABLE_NAME, content, CategoryTable.COL_ID + "='"
                            + category.id + "'", null);
                    /* add category to local list */
                    synchronized (catLock){
                        for (Category catInList : categories){
                            if (catInList.id.longValue() == category.id.longValue()){
                                catInList.name = category.name.toString();
                            }
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * Update a task record in database, called when editing task information. Spawns a new thread
     * 
     * @param task
     *            to update
     */
    private void _updateTaskMT(final Task task) {
        if (task.id != null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    deleteTaskCategoryMapping(task.id);
                    insertTaskCategoryMapping(task);

                    /** update Task Table */
                    if (stmtTaskUpdate == null){
                        stmtTaskUpdate = db.compileStatement(TaskTable.STMT_UPDATE_TASK);
                    }
                    stmtTaskUpdate.bindString(1, task.name);
                    stmtTaskUpdate.bindLong(2, task.getPriorityOrdinal());
                    int suspended = task.isSuspended() ? 1 : 0;
                    stmtTaskUpdate.bindLong(3, suspended);

                    stmtTaskUpdate.bindDouble(4, task.getProgress());
                    stmtTaskUpdate.bindLong(5, task.getWcet());
                    stmtTaskUpdate.bindLong(6, task.getActual());

                    stmtTaskUpdate.bindLong(7, task.getStart().getTimeInMillis());

                    long due = 0L;
                    if (task.getDue() != null){
                        due = task.getDue().getTimeInMillis();
                    }
                    stmtTaskUpdate.bindLong(8, due);
                    /* additional info */
                    stmtTaskUpdate.bindString(9, task.desc);
                    stmtTaskUpdate.bindString(10, task.location);

                    if (task.getParent() != null){
                        stmtTaskUpdate.bindLong(11, task.getParent().id);
                    } else{
                        stmtTaskUpdate.bindNull(11);
                    }
                    /* Where clause */
                    stmtTaskUpdate.bindLong(12, task.id);
                    stmtTaskUpdate.execute();

                    /** delete all reminders of this task */
                    db.delete(ReminderTable.TABLE_NAME, ReminderTable.COL_TID + "='" + task.id
                            + "'", null);
                    /** Update (insert) reminders */
                    for (Reminder reminder : task.getReminders()){
                        _insertReminder(reminder);
                    }
                    updateWidgets();
                }
            }).start();
        }
    }

    /**
     * Method to update widgets when database content changes
     */
    private void updateWidgets() {
        Intent intent = new Intent(this, WidgetDataService.class);
        startService(intent);
    }

}
