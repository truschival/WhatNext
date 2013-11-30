/******************************************************************************
 * \filename Category.java
 * Copyright (c) 2011 - Thomas Ruschival (thomas@ruschival.de)
 * 
 * \brief TaskCategory/Group
 * 
 * SPDX-License-Identifier:      GPL-2.0+
 *
 ******************************************************************************/
package de.ruschival.WhatNext;

/**
 * @author ruschi
 * 
 */
public class Category implements Comparable<Category> {
	/** Name of this category */
	public String name;
	/** Key/id in database */
	public Long id;
	/** color as marker */
	public int color=0xDEADBEEF;

	/**
	 * Simple Constructor with name only
	 * @param name
	 */
	public Category(String name){
		this.name = name;
	}
	
	/**
	 * Qualified Constructor for building from database
	 * @param id
	 * @param name
	 */
	public Category(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Return name of category
	 * 
	 * @return category.name
	 */
	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int compareTo(Category other) {
		if (other.id < id) {
			return -1;
		} else {
			return 1;
		}
	}

}
