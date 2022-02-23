package com.kuuhaku.utils;

import java.util.ArrayList;

public class RandomList<T> extends ArrayList<T> {
	public T get() {
		if (this.isEmpty()) return null;

		return Helper.getRandomEntry(this);
	}
}
