package com.kuuhaku.model.common;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class InfiniteIterator<T> implements ListIterator<T>, Iterable<T> {
	private final List<T> elements;
	private T current;
	private int index = -1;
	private int hash = 0;

	public InfiniteIterator(List<T> elements) {
		this.elements = elements;
	}

	@Override
	public boolean hasNext() {
		return !elements.isEmpty();
	}

	@Override
	public T next() {
		int code = elements.hashCode();
		if (hash != code) {
			index = elements.indexOf(current);
			hash = code;
		}

		return current = elements.get(++index % elements.size());
	}

	@Override
	public boolean hasPrevious() {
		return !elements.isEmpty();
	}

	@Override
	public T previous() {
		int code = elements.hashCode();
		if (hash != code) {
			index = elements.indexOf(current);
			hash = code;
		}

		if (--index < 0) index = elements.size() - 1;
		return current = elements.get(index);
	}

	@Override
	public int nextIndex() {
		return (index + 1) % elements.size();
	}

	@Override
	public int previousIndex() {
		if (index - 1 < 0) return elements.size() - 1;
		return index - 1;
	}

	@Override
	public void remove() {
		elements.remove(index);
	}

	@Override
	public void set(T t) {
		elements.set(index, t);
	}

	@Override
	public void add(T t) {
		elements.add(index + 1, t);
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return this;
	}
}
