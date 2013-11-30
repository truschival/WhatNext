/******************************************************************************
 * \filename WidgetProvider.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 * 
 * \brief Provider feeding the home-screen widget
 * 
 * Originally created on 04.04.2011 by Thomas Ruschival 
 *-----------------------------------------------------------------------------
 * $LastChangedBy:: ruschi                                    $
 * $LastChangedDate:: 2012-01-21 16:26:08 -0400 (Sat, 21 Jan #$
 * $Revision:: 8                                              $
 *----------------------------------------------------------------------------- 
 */

/**
 * Package / Namespace
 */
package de.ruschival.WhatNext;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * @author ruschi
 * 
 */

public class WidgetListProvider extends AppWidgetProvider {
	/**
	 * Intent Name to force update from other components
	 */
	public static final String ACTION_FORCE_UPDATE = "de.ruschival.WhatNext.Widgets.FORCE_UPDATE";
	
	@Override
	public void onUpdate(Context ctx, AppWidgetManager appWidgetMgr,
			int[] appWidgetIds) {
		
		ComponentName myName = new ComponentName(ctx, WidgetListProvider.class);
		/*
		 * Collect all widgetIds if we have several hanging around
		 */
		int[] allWidgetIds = appWidgetMgr.getAppWidgetIds(myName);
		Intent intent = new Intent(ctx.getApplicationContext(),
				WidgetDataService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
		/*
		 * Tell the server to update widgets
		 */
		ctx.startService(intent);
	}

}
