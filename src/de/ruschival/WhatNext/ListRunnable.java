/******************************************************************************
 * \filename ListRunnable.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 *
 * SPDX-License-Identifier:      GPL-2.0+
 *
 * \brief Interface of Runnable containing a List<>
 *  
 ******************************************************************************/
package de.ruschival.WhatNext;

import java.util.ArrayList;

/**
 * @author ruschi
 *
 */
public abstract class ListRunnable<T> implements Runnable {
	public ArrayList<T> content;
}
