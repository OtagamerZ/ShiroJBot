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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class JSONObject implements Iterable<Map.Entry<String, JsonElement>> {
	private final JsonObject obj;

	public JSONObject() {
		obj = new JsonObject();
	}

	public JSONObject(JsonObject json) {
		obj = json;
	}

	public JSONObject(String source) {
		this(JSONUtils.parseJSONObject(source));
	}

	public JSONObject(Map<?, ?> m) {
		this(JSONUtils.toJSON(m));
	}

	public JSONObject(Object bean) {
		this(JSONUtils.toJSON(bean));
	}

	public Iterator<Map.Entry<String, JsonElement>> iterator() {
		return obj.entrySet().iterator();
	}

	public JsonElement get(String key) {
		return obj.get(key);
	}

	public JsonElement get(String key, JsonElement or) {
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
			return get(key).getAsBoolean();
		} catch (NullPointerException e) {
			return false;
		}
	}

	public boolean getBoolean(String key, boolean or) {
		try {
			return Helper.getOr(get(key).getAsBoolean(), false) || or;
		} catch (NullPointerException e) {
			return or;
		}
	}

	public BigInteger getBigInteger(String key) {
		try {
			return get(key).getAsBigInteger();
		} catch (NullPointerException e) {
			return null;
		}
	}

	public BigInteger getBigInteger(String key, BigInteger or) {
		try {
			return Helper.getOr(get(key).getAsBigInteger(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public BigDecimal getBigDecimal(String key) {
		try {
			return get(key).getAsBigDecimal();
		} catch (NullPointerException e) {
			return null;
		}
	}

	public BigDecimal getBigDecimal(String key, BigDecimal or) {
		try {
			return Helper.getOr(get(key).getAsBigDecimal(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public double getDouble(String key) {
		try {
			return get(key).getAsDouble();
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public double getDouble(String key, double or) {
		try {
			return Helper.getOr(get(key).getAsDouble(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public float getFloat(String key) {
		try {
			return get(key).getAsFloat();
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public float getFloat(String key, float or) {
		try {
			return Helper.getOr(get(key).getAsFloat(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public Number getNumber(String key) {
		try {
			return get(key).getAsNumber();
		} catch (NullPointerException e) {
			return null;
		}
	}

	public Number getNumber(String key, Number or) {
		try {
			return Helper.getOr(get(key).getAsNumber(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public int getInt(String key) {
		try {
			return get(key).getAsInt();
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public int getInt(String key, int or) {
		try {
			return Helper.getOr(get(key).getAsInt(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public JSONArray getJSONArray(String key) {
		try {
			return new JSONArray(get(key).getAsJsonArray());
		} catch (NullPointerException e) {
			return null;
		}
	}

	public JSONArray getJSONArray(String key, JSONArray or) {
		JsonArray ja = get(key).getAsJsonArray();
		return ja == null ? or : new JSONArray(ja);
	}

	public JSONObject getJSONObject(String key) {
		try {
			return new JSONObject(get(key).getAsJsonObject());
		} catch (NullPointerException e) {
			return null;
		}
	}

	public JSONObject getJSONObject(String key, JSONObject or) {
		try {
			JsonObject jo = get(key).getAsJsonObject();
			return jo == null ? or : new JSONObject(jo);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public long getLong(String key) {
		try {
			return get(key).getAsLong();
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public long getLong(String key, long or) {
		try {
			return Helper.getOr(get(key).getAsLong(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public String getString(String key) {
		try {
			return get(key).getAsString();
		} catch (NullPointerException e) {
			return "";
		}
	}

	public String getString(String key, String or) {
		try {
			return Helper.getOr(get(key).getAsString(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public boolean has(String key) {
		return obj.has(key);
	}

	public JSONObject increment(String key) {
		JsonElement je = get(key);
		if (je == null) return this;

		if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isNumber()) {
			int n = je.getAsInt() + 1;
			obj.addProperty(key, n);
		}

		return this;
	}

	public boolean isNull(String key) {
		return get(key) == null;
	}

	public Iterator<String> keys() {
		return obj.keySet().iterator();
	}

	public Set<String> keySet() {
		return obj.keySet();
	}

	protected Set<Map.Entry<String, JsonElement>> entrySet() {
		return obj.entrySet();
	}

	public int size() {
		return obj.size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public JSONArray names() {
		return new JSONArray(obj.keySet());
	}

	public JSONObject put(String key, boolean value) {
		obj.addProperty(key, value);

		return this;
	}

	public JSONObject put(String key, Collection<?> value) {
		obj.add(key, JSONUtils.parseJSONElement(JSONUtils.toJSON(value)));

		return this;
	}

	public JSONObject put(String key, double value) {
		obj.addProperty(key, value);

		return this;
	}

	public JSONObject put(String key, float value) {
		obj.addProperty(key, value);

		return this;
	}

	public JSONObject put(String key, int value) {
		obj.addProperty(key, value);

		return this;
	}

	public JSONObject put(String key, long value) {
		obj.addProperty(key, value);

		return this;
	}

	public JSONObject put(String key, Map<?, ?> value) {
		obj.add(key, JSONUtils.parseJSONElement(JSONUtils.toJSON(value)));

		return this;
	}

	public JSONObject put(String key, Object value) {
		obj.add(key, JSONUtils.parseJSONElement(JSONUtils.toJSON(value)));

		return this;
	}

	public JSONObject putOnce(String key, Object value) {
		if (!obj.has(key))
			obj.add(key, JSONUtils.parseJSONElement(JSONUtils.toJSON(value)));

		return this;
	}

	public JSONObject putOpt(String key, Object value) {
		if (obj.has(key) && !get(key).isJsonNull() && value != null)
			obj.add(key, JSONUtils.parseJSONElement(JSONUtils.toJSON(value)));

		return this;
	}

	public JsonElement remove(String key) {
		return obj.remove(key);
	}

	public Map<String, JsonElement> toMap() {
		return obj.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public JsonObject getObj() {
		return obj;
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(obj);
	}
}
