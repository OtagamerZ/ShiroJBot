package com.kuuhaku.utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class LazyLoadingList<T> extends ArrayList<T> {
	private final Function<Integer, List<T>> loader;
	private final ExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private final int loadEvery;
	private int i = 0;
	private int last = 0;

	public LazyLoadingList(Function<Integer, List<T>> loader, int loadEvery) {
		this.loader = loader;
		this.loadEvery = loadEvery;
		addAll(loader.apply(i));
	}

	public LazyLoadingList(int initialCapacity, Function<Integer, List<T>> loader, int loadEvery) {
		super(initialCapacity);
		this.loader = loader;
		this.loadEvery = loadEvery;
		addAll(loader.apply(i));
	}

	public LazyLoadingList(@NotNull Collection<? extends T> c, Function<Integer, List<T>> loader, int loadEvery) {
		super(c);
		this.loader = loader;
		this.loadEvery = loadEvery;
		addAll(loader.apply(i));
	}

	public int index() {
		return i;
	}

	public void setIndex(int i) {
		this.i = Math.min(i, last);
	}

	public T current() {
		return Helper.safeGet(this, i);
	}

	public T next() {
		if (++i > last && (i % loadEvery == 0 || i >= size())) {
			exec.submit(() -> addAll(loader.apply(i)));
		}

		if (i >= size()) {
			return Helper.safeGet(this, i = size() - 1);
		}

		last = Math.max(last, i);
		return Helper.safeGet(this, i);
	}

	public T previous() {
		return Helper.safeGet(this, --i);
	}
}
