/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.common;

import com.kuuhaku.util.Utils;
import kotlin.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultiMap<K, V> {
	private final Map<K, Pair<K[], V>> map;
	private final Pair<K[], V> DEFAULT_RETURN = new Pair<>(null, null);

	public MultiMap() {
		map = new HashMap<>();
	}

	public MultiMap(Supplier<Map<K, Pair<K[], V>>> supplier) {
		map = supplier.get();
	}

	public int size() {
		return (int) map.values().parallelStream().distinct().count();
	}

	public V get(K key) {
		return Utils.getOr(map.get(key), DEFAULT_RETURN).getSecond();
	}

	@SafeVarargs
	public final V put(V value, K... keys) {
		V prev = null;

		for (K key : keys) {
			if (map.containsKey(key)) {
				prev = remove(key);
			}
		}

		Pair<K[], V> wrapper = new Pair<>(keys, value);
		for (K key : keys) {
			map.put(key, wrapper);
		}

		return prev;
	}

	public V remove(Object key) {
		Pair<K[], V> prev = map.get(key);
		if (prev == null) return DEFAULT_RETURN.getSecond();

		for (K k : prev.getFirst()) {
			map.remove(k);
		}

		return Utils.getOr(prev, DEFAULT_RETURN).getSecond();
	}

	public boolean remove(Object key, Object value) {
		boolean out = false;

		Pair<K[], V> prev = map.get(key);
		if (prev == null) return false;

		for (K k : prev.getFirst()) {
			out |= map.remove(k, value);
		}

		return out;
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public List<V> values() {
		return map.values().parallelStream().map(Pair::getSecond).toList();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return map.entrySet().parallelStream()
				.map(e -> Map.entry(e.getKey(), e.getValue().getSecond()))
				.collect(Collectors.toSet());
	}

	public V getOrDefault(K key, V defaultValue) {
		return Utils.getOr(get(key), defaultValue);
	}

	@SafeVarargs
	public final V putIfAbsent(V value, K... keys) {
		for (K key : keys) {
			if (map.containsKey(key)) {
				return get(key);
			}
		}

		Pair<K[], V> wrapper = new Pair<>(keys, value);
		for (K key : keys) {
			map.put(key, wrapper);
		}

		return null;
	}
}
