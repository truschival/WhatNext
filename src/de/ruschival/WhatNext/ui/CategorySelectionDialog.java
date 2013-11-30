/******************************************************************************
 * \filename CategorySelectionDialog.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 *
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext.ui;

import java.util.HashSet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import de.ruschival.WhatNext.Category;
import de.ruschival.WhatNext.ListRunnable;
import de.ruschival.WhatNext.R;
import de.ruschival.WhatNext.db.TaskDBService;

/**
 * @author ruschi
 * 
 */
public class CategorySelectionDialog extends Dialog {
	public final String TAG = CategorySelectionDialog.class.getCanonicalName();
	/**
	 * Form elements
	 */
	/** listView of categories */
	private final ListView categoryList;
	/** Button to save/filter */
	private final Button saveBtn;
	/** Button for creation of category */
	private final ImageButton newBtn;
	/** modal dialog for creation/editing */
	private final SimpleTextEditDialog editDialog;
	/** initial context */
	private final Context ctx;
	/**
	 * Binder of Creating Activity to exchange data with DB
	 */
	public TaskDBService.TaskDBServiceBinder taskDBbinder;

	/** ListAdapter for category List */
	ArrayAdapter<Category> listadapter;

	/**
	 * Currently Selected Category for edit
	 */
	Category selectedCategory;

	public final Handler uiThreadHandler = new Handler();
	public final CategoryListUpdateRunnable uiListRunnable = new CategoryListUpdateRunnable();

	/**
	 * Hashset of Ids for initially marked Categories
	 */
	private HashSet<Long> initiallyMarked;

	/**
	 * Runnable Class to allow asynchronous update of ListView
	 * 
	 * @author ruschi
	 */
	public class CategoryListUpdateRunnable extends ListRunnable<Category> {
		@Override
		public void run() {
			listadapter.clear();
			listadapter.setNotifyOnChange(false);
			for (Category cat : content) {
				listadapter.add(cat);
			}
			listadapter.notifyDataSetChanged();
			selectCategories();
		}
	}

	/**
	 * Constructor will initialize listview
	 * 
	 * @param context
	 * @param hasTitle
	 *            if the title will be displayed
	 */
	public CategorySelectionDialog(Context context, boolean hasTitle) {
		super(context);
		ctx = context;
		if (!hasTitle) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.dialog_categories);
		categoryList = (ListView) findViewById(R.id.categoryList);
		saveBtn = (Button) findViewById(R.id.dialogBtn);
		newBtn = (ImageButton) findViewById(R.id.newCategoryBtn);
		/** Populate List */
		listadapter = new ArrayAdapter<Category>(ctx,
				android.R.layout.simple_list_item_multiple_choice);
		categoryList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		categoryList.setAdapter(listadapter);

		/** Build EditDialog */
		editDialog = new SimpleTextEditDialog(ctx, true);

		/** Open SimpleTextEditDialog on click on newButton */
		newBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editDialog.setTitle(R.string.sa_new_category);
				selectedCategory = null;
				editDialog.show();
			}
		});

		/**
		 * Listener on Button of modal dialog
		 */
		editDialog.saveBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editCategory();
				editDialog.dismiss();
			}
		});
		registerForContextMenu(categoryList);
		categoryList.setOnCreateContextMenuListener(this);
	}

	/**
	 * Context Menu on ListItems
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = new MenuInflater(ctx);
		inflater.inflate(R.menu.ctx_m_categorylist, menu);
	}

	@Override
	public boolean onMenuItemSelected(int aFeatureId, MenuItem item) {
		if (aFeatureId == Window.FEATURE_CONTEXT_MENU)
			return onContextItemSelected(item);
		else
			return super.onMenuItemSelected(aFeatureId, item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		selectedCategory = listadapter.getItem(info.position);
		if (selectedCategory != null) {
			switch (item.getItemId()) {
			case R.id.ctxi_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
				builder.setMessage(R.string.confirm_delete_category);
				builder.setPositiveButton(R.string.sl_yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if (selectedCategory != null) {
									taskDBbinder.deleteCategory(selectedCategory);
									listadapter.remove(selectedCategory);
									selectedCategory = null;
								}
								dialog.dismiss();
							}
						});
				builder.setNegativeButton(R.string.sl_no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				builder.create().show();
				break;
			case R.id.ctxi_edit:
				editDialog.setTitle(R.string.sa_edit_category);
				if (selectedCategory != null) {
					editDialog.text.setText(selectedCategory.name);
				}
				editDialog.show();
				break;
			}
		}
		return true;
	}

	/**
	 * Access to DialogElements
	 * 
	 * @return categoryList
	 */
	public ListView getCategoryList() {
		return categoryList;
	}

	/**
	 * Access to DialogElements
	 * 
	 * @return dialogBtn
	 */
	public Button getSaveButton() {
		return saveBtn;
	}

	/**
	 * @return the editDialog
	 */
	public Dialog getEditDialog() {
		return editDialog;
	}

	/**
	 * @return the newCategoryBtn
	 */
	public ImageButton getNewButton() {
		return newBtn;
	}

	/**
	 * Mark categories with matching IDs
	 * 
	 * @param ids
	 */
	public void setSelectedItemIDs(long[] ids) {
		initiallyMarked = new HashSet<Long>();
		for (Long id : ids) {
			initiallyMarked.add(id);
		}
	}

	public void selectCategories() {
		int listlen = categoryList.getCount();
		if (initiallyMarked != null) {
			for (int i = 0; i < listlen; i++) {
				if (initiallyMarked.contains( ((Category)categoryList.getItemAtPosition(i)).id)) {
					categoryList.setItemChecked(i, true);
				}
			}
		}
	}

	/**
	 * Edit/Create new Category using modal dialog editDialog called in listener
	 * of {@link SimpleTextEditDialog}
	 */
	private void editCategory() {
		if (selectedCategory != null) {
			selectedCategory.name = editDialog.text.getText().toString();
			taskDBbinder.updateCategory(selectedCategory);
			listadapter.notifyDataSetChanged();
		} else {
			selectedCategory = new Category(editDialog.text.getText().toString());
			taskDBbinder.insertCategory(selectedCategory);
			listadapter.add(selectedCategory);
		}
	}
}
