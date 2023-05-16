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

package com.kuuhaku.util.json;

import com.kuuhaku.util.Utils;
import org.intellij.lang.annotations.Language;

import java.io.Serial;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONObject extends HashMap<String, Object> implements Cloneable {
	@Serial
	private static final long serialVersionUID = 6263175813447647494L;

	public JSONObject() {
	}

	public JSONObject(Map<String, Object> map) {
		putAll(map);
	}

	public JSONObject(@Language("JSON5") String json) {
		this(JSONUtils.toMap(json));
	}

	public JSONObject(Object bean) {
		this(JSONUtils.toJSON(bean));
	}

	@SafeVarargs
	public static JSONObject of(Entry<String, Object>... entries) {
		JSONObject out = new JSONObject();
		for (Entry<String, Object> entry : entries) {
			out.put(entry.getKey(), entry.getValue());
		}

		return out;
	}

	public Iterator<Entry<String, Object>> iterator() {
		return entrySet().iterator();
	}

	public Object get(String key) {
		return super.get(key);
	}

	public Object get(String key, Object or) {
		return Utils.getOr(get(key), or);
	}

	public <T> T get(Class<T> klass, String key) {
		try {
			return klass.cast(get(key));
		} catch (ClassCastException e) {
			return null;
		}
	}

	public <T> T get(Class<T> klass, String key, T or) {
		return Utils.getOr(get(klass, key), or);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, String key) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equalsIgnoreCase(getString(key)))
				.findFirst()
				.orElse(null);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, String key, E or) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equalsIgnoreCase(getString(key)))
				.findFirst()
				.orElse(or);
	}

	public boolean getBoolean(String key) {
		try {
			return (boolean) get(key, false);
		} catch (ClassCastException e) {
			return get(key) != null;
		}
	}

	public boolean getBoolean(String key, boolean or) {
		return (boolean) get(key, or);
	}

	public double getDouble(String key) {
		try {
			return ((Number) get(key, 0)).doubleValue();
		} catch (ClassCastException e) {
			try {
				return Double.parseDouble((String) get(key, "0"));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public double getDouble(String key, double or) {
		try {
			return ((Number) get(key, or)).doubleValue();
		} catch (ClassCastException e) {
			try {
				return Double.parseDouble((String) get(key, String.valueOf(or)));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public float getFloat(String key) {
		try {
			return ((Number) get(key, 0)).floatValue();
		} catch (ClassCastException e) {
			try {
				return Float.parseFloat((String) get(key, "0"));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public float getFloat(String key, float or) {
		try {
			return ((Number) get(key, or)).floatValue();
		} catch (ClassCastException e) {
			try {
				return Float.parseFloat((String) get(key, String.valueOf(or)));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public int getInt(String key) {
		try {
			return ((Number) get(key, 0)).intValue();
		} catch (ClassCastException e) {
			try {
				return Integer.parseInt((String) get(key, "0"));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public int getInt(String key, int or) {
		try {
			return ((Number) get(key, or)).intValue();
		} catch (ClassCastException e) {
			try {
				return Integer.parseInt((String) get(key, String.valueOf(or)));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public long getLong(String key) {
		try {
			return ((Number) get(key, 0)).longValue();
		} catch (ClassCastException e) {
			try {
				return Long.parseLong((String) get(key, "0"));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public long getLong(String key, long or) {
		try {
			return ((Number) get(key, or)).longValue();
		} catch (ClassCastException e) {
			try {
				return Long.parseLong((String) get(key, String.valueOf(or)));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public String getString(String key) {
		return String.valueOf(get(key, ""));
	}

	public String getString(String key, String or) {
		return String.valueOf(get(key, or));
	}

	public JSONArray getJSONArray(String key) {
		return new JSONArray(get(key, "[]"));
	}

	public JSONArray getJSONArray(String key, JSONArray or) {
		JSONArray arr = getJSONArray(key);
		return arr.isEmpty() ? or : arr;
	}

	public JSONObject getJSONObject(String key) {
		return new JSONObject(get(key, "{}"));
	}

	public JSONObject getJSONObject(String key, JSONObject or) {
		JSONObject obj = getJSONObject(key);
		return obj.isEmpty() ? or : obj;
	}

	public boolean has(String key) {
		return containsKey(key);
	}

	public JSONObject increment(String key) {
		Object obj = get(key);
		if (obj == null) return this;

		if (obj instanceof Number n) {
			put(key, n.longValue() + 1);
		}

		return this;
	}

	public Iterator<String> keys() {
		return keySet().iterator();
	}

	public JSONArray names() {
		return new JSONArray(keySet());
	}

	public JSONObject put(String key, Object value) {
		super.put(key, value);

		return this;
	}

	public JSONObject putOnce(String key, Object value) {
		putIfAbsent(key, value);

		return this;
	}

	public JSONObject putOpt(String key, Object value) {
		if (get(key) == null && value != null)
			put(key, value);

		return this;
	}

	public Object rightShift(String obj) {
		return remove(obj);
	}

	public JSONArray toJSONArray() {
		return new JSONArray(values());
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}

	@Override
	public JSONObject clone() {
		return new JSONObject(this);
	}
}
