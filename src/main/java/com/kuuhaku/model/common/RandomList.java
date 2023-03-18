package com.kuuhaku.model.common;

import com.kuuhaku.Constants;
import kotlin.Pair;
import org.apache.commons.collections4.list.TreeList;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

/**
 * Curve bias:
 * <br>
 * <br>
 * 0ㅤㅤㅤㅤㅤㅤㅤㅤㅤ1.0
 * <br>
 * <----------|---------->
 * <br>
 * 1/Nㅤㅤㅤ1.0ㅤㅤㅤㅤN
 * <br>
 * <br>
 * Values >1 tend towards lower weights, values <1 tend towards higher weights
 **/
public class RandomList<T> {
	private final NavigableMap<Double, T> map = new TreeMap<>();
	private final List<Pair<Double, T>> pool = new TreeList<>();
	private final RandomGenerator rng;
	private final BiFunction<Double, Double, Double> randGen;
	private final double fac;
	private double total = 0;

	public RandomList() {
		this(Constants.DEFAULT_RNG.get(), 1);
	}

	public RandomList(RandomGenerator rng) {
		this(rng, 1);
	}

	public RandomList(double fac) {
		this(Constants.DEFAULT_RNG.get(), fac);
	}

	public RandomList(RandomGenerator rng, double fac) {
		this(rng, (a, b) -> {
			if (b < 1) {
				return 1 - Math.pow(a, b);
			}

			return Math.pow(a, 1 / b);
		}, fac);
	}

	public RandomList(BiFunction<Double, Double, Double> randGen, double fac) {
		this(Constants.DEFAULT_RNG.get(), randGen, fac);
	}

	public RandomList(RandomGenerator rng, BiFunction<Double, Double, Double> randGen, double fac) {
		this.rng = rng;
		this.randGen = randGen;
		this.fac = fac;
	}

	public void add(@NotNull T item) {
		add(item, 1);
	}

	public void add(@NotNull T item, double weight) {
		if (weight <= 0) return;

		total += weight;
		map.put(total, item);
		pool.add(new Pair<>(weight, item));
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

	public void remove(@NotNull T item) {
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

	public Collection<T> values() {
		return map.values();
	}

	public List<Pair<Double, T>> entries() {
		return pool;
	}
}
