package com.kuuhaku.model.common.dunhun;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class MultiSlot<T> implements Iterable<T> {
	private final Object[] slots;
	private final int size;

	public MultiSlot(int size) {
		this.slots = new Object[size];
		this.size = size;
	}

	@SuppressWarnings("unchecked")
	public T get(int index) {
		if (index >= slots.length) return null;
		return (T) slots[index];
	}

	public boolean add(T entry) {
		if (entry == null) return false;

		int free = ArrayUtils.indexOf(slots, null);
		if (free == -1) return false;

		slots[free] = entry;
		return true;
	}

	public boolean remove(T entry) {
		int idx = ArrayUtils.indexOf(slots, entry);
		if (idx == -1) return false;

		slots[idx] = null;
		return true;
	}

	public void replace(T oldEntry, T newEntry) {
		if (Objects.equals(newEntry, oldEntry)) return;

		int idx = ArrayUtils.indexOf(slots, oldEntry);
		if (idx == -1) return;

		slots[idx] = newEntry;
	}

	@SuppressWarnings("unchecked")
	public List<T> getEntries() {
		return Stream.of(slots)
				.map(o -> (T) o)
				.toList();
	}

	public int getSize() {
		return size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public @NotNull Iterator<T> iterator() {
		return Stream.of(slots)
				.map(o -> (T) o)
				.iterator();
	}
}
