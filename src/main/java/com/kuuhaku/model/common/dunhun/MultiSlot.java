package com.kuuhaku.model.common.dunhun;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MultiSlot<T> implements Iterable<T> {
	private final List<T> slots;
	private final int size;

	public MultiSlot(int size) {
		this.slots = new ArrayList<>(size);
		this.size = size;
	}

	public T get(int index) {
		if (index >= slots.size()) return null;
		return slots.get(index);
	}

	public boolean add(T entry) {
		if (slots.size() >= size || entry == null) return false;
		return slots.add(entry);
	}

	public boolean remove(T entry) {
		return slots.remove(entry);
	}

	public List<T> getEntries() {
		return slots;
	}

	public int getSize() {
		return size;
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return slots.iterator();
	}
}
