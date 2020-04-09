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
import java.util.concurrent.TimeUnit;

public class RateLimitingMap<K> extends HashMap<K, Map<Boolean, Long>> {

	public boolean ratelimit(K key) {
		Map<Boolean, Long> timeout = Collections.singletonMap(true, System.currentTimeMillis());
		super.put(key, timeout);
		return true;
	}

	public boolean getAuthorIfNotExpired(K key, int time, TimeUnit unit) {
		if (super.get(key) == null) return false;
		Entry<Boolean, Long> entry = super.get(key).entrySet().iterator().next();
		return expired(entry.getValue(), time, unit) ? true : entry.getKey();
	}

	public void clearExpired(int time, TimeUnit unit) {
		Object[] entries = super.entrySet().stream().filter(e -> expired(e.getValue().entrySet().iterator().next().getValue(), time, unit)).map(Entry::getKey).toArray(Object[]::new);
		for (Object entry : entries) {
			super.remove(entry);
		}
	}

	private boolean expired(long timestamp, int time, TimeUnit unit) {
		return unit.convert(timestamp - unit.toMillis(time), TimeUnit.MILLISECONDS) > time;
	}
}
