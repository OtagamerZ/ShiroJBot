package com.kuuhaku.utils.collections;

import com.kuuhaku.utils.helpers.CollectionHelper;

import java.util.ArrayList;

public class RandomList<T> extends ArrayList<T> {
	public T get() {
		if (this.isEmpty()) return null;

		return CollectionHelper.getRandomEntry(this);
	}
}
