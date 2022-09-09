package com.kuuhaku.model.common;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiFunction;

public class RandomList<T> {
	private final NavigableMap<Double, T> map = new TreeMap<>();
	private final Random rng;
	private final BiFunction<Double, Double, Double> randGen;
	private final double fac;
	/*
	Curve bias, low values tend toward 1, while high values tend toward 0
	 */
	private double total = 0;

	public RandomList() {
		this(new Random(), 1);
	}

	public RandomList(Random rng) {
		this(rng, 1);
	}

	public RandomList(double fac) {
		this(new Random(), fac);
	}

	public RandomList(Random rng, double fac) {
		this(rng, Math::pow, fac);
	}

	public RandomList(BiFunction<Double, Double, Double> randGen, double fac) {
		this(new Random(), randGen, fac);
	}

	public RandomList(Random rng, BiFunction<Double, Double, Double> randGen, double fac) {
		this.rng = rng;
		this.randGen = randGen;
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

		return map.higherEntry(randGen.apply(rng.nextDouble(), fac) * total).getValue();
	}

	public T remove() {
		T t = get();
		if (t == null) return null;

		remove(t);

		return t;
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
