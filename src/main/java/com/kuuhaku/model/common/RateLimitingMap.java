/*
 * This file is part of Shiro J Bot.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RateLimitingMap<K, V> extends HashMap<K, Map<V, Long>> {

	public V ratelimit(K key, V value) {
		Map<V, Long> timeout = Collections.singletonMap(value, System.currentTimeMillis());
		return Objects.requireNonNull(super.put(key, timeout)).keySet().iterator().next();
	}

	public V getAuthorIfNotExpired(K key, int time, TimeUnit unit) {
		Entry<V, Long> entry = super.get(key).entrySet().iterator().next();
		return expired(entry.getValue(), time, unit) ? null : entry.getKey();
	}

	public void clearExpired(int time, TimeUnit unit) {
		Object[] entries = super.entrySet().stream().filter(e -> expired(e.getValue().entrySet().iterator().next().getValue(), time, unit)).map(Entry::getKey).toArray(Object[]::new);
	}

	private boolean expired(long timestamp, int time, TimeUnit unit) {
		return unit.convert(timestamp - unit.toMillis(time), TimeUnit.MILLISECONDS) > time;
	}
}
