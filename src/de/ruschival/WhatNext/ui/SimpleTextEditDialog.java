/******************************************************************************
 * \filename SimpleTextEditDialog.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief 
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 *****************************************************************************/
package de.ruschival.WhatNext.ui;

import de.ruschival.WhatNext.R;
import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author ruschi
 * 
 */
public class SimpleTextEditDialog extends Dialog {
	/**
	 * Form elements
	 */
	public Button saveBtn;
	public EditText text;

	/**
	 * default constructor
	 * 
	 * @param context
	 * @param hasTitle
	 *            if the title will be displayed
	 */
	public SimpleTextEditDialog(Context context, boolean hasTitle) {
		super(context);
		if (!hasTitle) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.dialog_simple_textedit);
		text = (EditText) findViewById(R.id.editText);
		saveBtn = (Button) findViewById(R.id.saveBtn);
	}

}
