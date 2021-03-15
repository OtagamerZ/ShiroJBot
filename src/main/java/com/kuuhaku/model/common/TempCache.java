/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kuuhaku.model.enums.Expiration;
import com.kuuhaku.utils.Helper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TempCache<K, V> implements Map<K, V> {
	Cache<K, V> cache;

	public TempCache() {
		this.cache = CacheBuilder.newBuilder().build();
	}

	public TempCache(int max) {
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(max)
				.build();
	}

	public TempCache(int time, TimeUnit unit) {
		this.cache = CacheBuilder.newBuilder()
				.expireAfterWrite(time, unit)
				.build();
	}

	public TempCache(int time, TimeUnit unit, Expiration type) {
		this.cache = switch (type) {
			case READ -> CacheBuilder.newBuilder()
					.expireAfterAccess(time, unit)
					.build();
			case WRITE -> CacheBuilder.newBuilder()
					.expireAfterWrite(time, unit)
					.build();
			case BOTH -> CacheBuilder.newBuilder()
					.expireAfterAccess(time, unit)
					.expireAfterWrite(time, unit)
					.build();
		};
	}

	public TempCache(int max, int time, TimeUnit unit) {
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(max)
				.expireAfterWrite(time, unit)
				.build();
	}

	public TempCache(int max, int time, TimeUnit unit, Expiration type) {
		this.cache = switch (type) {
			case READ -> CacheBuilder.newBuilder()
					.maximumSize(max)
					.expireAfterAccess(time, unit)
					.build();
			case WRITE -> CacheBuilder.newBuilder()
					.maximumSize(max)
					.expireAfterWrite(time, unit)
					.build();
			case BOTH -> CacheBuilder.newBuilder()
					.maximumSize(max)
					.expireAfterAccess(time, unit)
					.expireAfterWrite(time, unit)
					.build();
		};
	}

	public Cache<K, V> getCache() {
		return cache;
	}

	@Override
	public int size() {
		return (int) cache.size();
	}

	@Override
	public boolean isEmpty() {
		return cache.size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return cache.asMap().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return cache.asMap().containsValue(value);
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public V get(Object key) {
		return cache.getIfPresent(key);
	}

	@Nullable
	@Override
	public V put(K key, V value) {
		cache.put(key, value);
		return value;
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public V remove(Object key) {
		V value = cache.getIfPresent(key);
		cache.invalidate(key);
		return value;
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		cache.putAll(m);
	}

	@Override
	public void clear() {
		cache.invalidateAll();
	}

	@NotNull
	@Override
	public Set<K> keySet() {
		return cache.asMap().keySet();
	}

	@NotNull
	@Override
	public Collection<V> values() {
		return cache.asMap().values();
	}

	@NotNull
	@Override
	public Set<Entry<K, V>> entrySet() {
		return cache.asMap().entrySet();
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public V getOrDefault(Object key, V defaultValue) {
		return Helper.getOr(cache.getIfPresent(key), defaultValue);
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		cache.asMap().forEach(action);
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		cache.asMap().replaceAll(function);
	}

	@Nullable
	@Override
	public V putIfAbsent(K key, V value) {
		if (cache.getIfPresent(key) == null)
			cache.put(key, value);
		return value;
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean remove(Object key, Object value) {
		if (cache.getIfPresent(key) == null) {
			cache.invalidate(key);
			return true;
		} else {
			cache.invalidate(key);
			return false;
		}
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		if (cache.getIfPresent(key) == oldValue) {
			cache.put(key, newValue);
			return true;
		} else {
			return false;
		}
	}

	@Nullable
	@Override
	public V replace(K key, V value) {
		V old = cache.getIfPresent(key);
		cache.put(key, value);
		return old;
	}

	@Override
	public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
		cache.put(key, mappingFunction.apply(key));
		return cache.getIfPresent(key);
	}

	@Override
	public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		V value = cache.getIfPresent(key);
		if (value == null) return null;

		cache.put(key, remappingFunction.apply(key, value));
		return cache.getIfPresent(key);
	}

	@Override
	public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		V value = cache.getIfPresent(key);
		cache.put(key, remappingFunction.apply(key, value));
		return cache.getIfPresent(key);
	}

	@Override
	public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		V old = cache.getIfPresent(key);
		cache.put(key, remappingFunction.apply(old, value));
		return cache.getIfPresent(key);
	}
}
