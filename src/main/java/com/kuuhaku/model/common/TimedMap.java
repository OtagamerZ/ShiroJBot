package com.kuuhaku.model.common;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TimedMap<V> implements Iterable<Map.Entry<V, Integer>> {
	private final Map<V, Integer> values = new HashMap<>();

	public void set(V value, int time) {
		values.compute(value, (k, v) -> {
			int newTime = v == null ? time : Math.max(v, time);
			if (newTime <= 0) {
				return null;
			}

			return newTime;
		});
	}

	public void add(V value, int time) {
		values.compute(value, (k, v) -> {
			int newTime = v == null ? time : v + time;
			if (newTime <= 0) {
				return null;
			}

			return newTime;
		});
	}

	public void addAll(TimedMap<V> other) {
		for (Map.Entry<V, Integer> entry : other) {
			add(entry.getKey(), entry.getValue());
		}
	}

	public Set<V> getValues() {
		return values.keySet();
	}

	public int getTime(V value) {
		return values.getOrDefault(value, 0);
	}

	public void remove(V value) {
		values.remove(value);
	}

	public void reduceTime() {
		reduceTime(1);
	}

	public void reduceTime(int amount) {
		Iterator<Map.Entry<V, Integer>> it = values.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<V, Integer> e = it.next();
			e.setValue(e.getValue() - amount);

			if (e.getValue() <= 0) {
				it.remove();
			}
		}
	}

	public void reduceTime(V value, int amount) {
		int time = getTime(value);
		if (time > 0) {
			values.put(value, time - amount);
		} else {
			remove(value);
		}
	}

	@Override
	public @NotNull Iterator<Map.Entry<V, Integer>> iterator() {
		return values.entrySet().iterator();
	}
}
