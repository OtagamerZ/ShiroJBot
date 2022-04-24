package com.kuuhaku.model.common;

import javax.annotation.Nonnull;
import java.util.*;

public class RandomList<T> {
	private final NavigableMap<Double, T> map = new TreeMap<>();
	private final Random rng;
	private final double fac;
	private double total = 0;

	public RandomList() {
		this(new Random(), 1);
	}

	public RandomList(Random rng) {
		this(rng, 1);
	}

	public RandomList(double fac) {
		this(new Random(), 1);
	}

	public RandomList(Random rng, double fac) {
		this.rng = rng;
		this.fac = fac;
	}

	public void add(@Nonnull T item) {
		add(item, 1);
	}

	public void add(@Nonnull T item, double weight) {
		if (weight <= 0) return;

		total += weight;
		map.put(total, item);
	}

	public T get() {
		if (map.isEmpty()) return null;

		return map.higherEntry(Math.pow(rng.nextDouble(), fac) * total).getValue();
	}

	public void remove(@Nonnull T item) {
		Iterator<Map.Entry<Double, T>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Double, T> entry = it.next();
			if (entry.getValue().equals(item)) {
				total -= entry.getKey();
				it.remove();
				break;
			}
		}
	}
}
