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

package com.kuuhaku.utils;

import java.util.*;

public class JSONObject implements JSONWrapper, Iterable<Map.Entry<String, Object>> {
	private final Map<String, Object> map;

	public JSONObject() {
		map = new HashMap<>();
	}

	public JSONObject(Map<String, Object> map) {
		this.map = map;
	}

	public JSONObject(String json) {
		this(JSONUtils.toMap(json));
	}

	public JSONObject(Object bean) {
		this(JSONUtils.toJSON(bean));
	}

	public Iterator<Map.Entry<String, Object>> iterator() {
		return map.entrySet().iterator();
	}

	public Object get(String key) {
		return map.get(key);
	}

	public Object get(String key, Object or) {
		return Helper.getOr(get(key), or);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, String key) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equals(getString(key)))
				.findFirst()
				.orElse(null);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, String key, E or) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equals(getString(key)))
				.findFirst()
				.orElse(or);
	}

	public boolean getBoolean(String key) {
		try {
			return (boolean) get(key);
		} catch (NullPointerException e) {
			return false;
		}
	}

	public boolean getBoolean(String key, boolean or) {
		try {
			return Helper.getOr(get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public double getDouble(String key) {
		try {
			return (double) get(key);
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public double getDouble(String key, double or) {
		try {
			return Helper.getOr(get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public float getFloat(String key) {
		try {
			return (float) get(key);
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public float getFloat(String key, float or) {
		try {
			return Helper.getOr(get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public int getInt(String key) {
		try {
			return (int) get(key);
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public int getInt(String key, int or) {
		try {
			return Helper.getOr(get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public long getLong(String key) {
		try {
			return (long) get(key);
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public long getLong(String key, long or) {
		try {
			return Helper.getOr(get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public String getString(String key) {
		try {
			return (String) get(key);
		} catch (NullPointerException e) {
			return "";
		}
	}

	public String getString(String key, String or) {
		try {
			return Helper.getOr(get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public JSONArray getJSONArray(String key) {
		try {
			return new JSONArray(get(key));
		} catch (NullPointerException e) {
			return null;
		}
	}

	public JSONArray getJSONArray(String key, JSONArray or) {
		List<?> ja = (List<?>) get(key);
		return ja == null ? or : new JSONArray(ja);
	}

	public JSONObject getJSONObject(String key) {
		try {
			return new JSONObject(get(key));
		} catch (NullPointerException e) {
			return null;
		}
	}

	public JSONObject getJSONObject(String key, JSONObject or) {
		try {
			Map<?, ?> jo = (Map<?, ?>) get(key);
			return jo == null ? or : new JSONObject(jo);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public boolean has(String key) {
		return map.containsKey(key);
	}

	public JSONObject increment(String key) {
		Object obj = get(key);
		if (obj == null) return this;

		if (obj instanceof Number n) {
			map.put(key, n.longValue() + 1);
		}

		return this;
	}

	public Iterator<String> keys() {
		return map.keySet().iterator();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	protected Set<Map.Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	public JSONArray names() {
		return new JSONArray(map.keySet());
	}

	public JSONObject put(String key, Object value) {
		map.put(key, value);

		return this;
	}

	public JSONObject putOnce(String key, Object value) {
		map.putIfAbsent(key, value);

		return this;
	}

	public JSONObject putOpt(String key, Object value) {
		if (map.get(key) == null && value != null)
			map.put(key, value);

		return this;
	}

	public Object remove(String key) {
		return map.remove(key);
	}

	public JSONArray toJSONArray() {
		return new JSONArray(map.values());
	}

	public Map<String, Object> toMap() {
		return map;
	}

	@Override
	public Map<String, Object> getContent() {
		return map;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(map);
	}
}
