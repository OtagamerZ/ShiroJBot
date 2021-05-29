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

public class JSONArray implements Iterable<JsonElement> {
	private final JsonArray arr;
	
	public JSONArray() {
		arr = new JsonArray();
	}
	
	public JSONArray(JsonArray array) {
		arr = array;
	}

	public JSONArray(String source) {
		arr = JSONUtils.parseJSONArray(source);
	}

	public JSONArray(Collection<?> collection) {
		this(JSONUtils.toJSON(collection));
	}

	public JSONArray(Object array) {
		this(JSONUtils.toJSON(array));
	}

	public Iterator<JsonElement> iterator() {
		return arr.iterator();
	}

	public JsonElement get(int index) {
		return arr.get(index);
	}

	public JsonElement get(int index, JsonElement or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, int index) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equals(getString(index)))
				.findFirst()
				.orElse(null);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, int index, E or) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equals(getString(index)))
				.findFirst()
				.orElse(or);
	}

	public boolean getBoolean(int index) {
		return get(index).getAsBoolean();
	}

	public boolean getBoolean(int index, boolean or) {
		try {
			return Helper.getOr(get(index).getAsBoolean(), false) || or;
		} catch (NullPointerException e) {
			return or;
		}
	}

	public BigInteger getBigInteger(int index) {
		return get(index).getAsBigInteger();
	}

	public BigInteger getBigInteger(int index, BigInteger or) {
		try {
			return Helper.getOr(get(index).getAsBigInteger(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public BigDecimal getBigDecimal(int index) {
		return get(index).getAsBigDecimal();
	}

	public BigDecimal getBigDecimal(int index, BigDecimal or) {
		try {
			return Helper.getOr(get(index).getAsBigDecimal(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public double getDouble(int index) {
		return get(index).getAsDouble();
	}

	public double getDouble(int index, double or) {
		try {
			return Helper.getOr(get(index).getAsDouble(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public float getFloat(int index) {
		return get(index).getAsFloat();
	}

	public float getFloat(int index, float or) {
		try {
			return Helper.getOr(get(index).getAsFloat(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public Number getNumber(int index) {
		return get(index).getAsNumber();
	}

	public Number getNumber(int index, Number or) {
		try {
			return Helper.getOr(get(index).getAsNumber(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public int getInt(int index) {
		return get(index).getAsInt();
	}

	public int getInt(int index, int or) {
		try {
			return Helper.getOr(get(index).getAsInt(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public JSONArray getJSONArray(int index) {
		return new JSONArray(get(index).getAsJsonArray());
	}

	public JSONArray getJSONArray(int index, JSONArray or) {
		JsonArray ja = get(index).getAsJsonArray();
		return ja == null ? or : new JSONArray(ja);
	}

	public JSONObject getJSONObject(int index) {
		return new JSONObject(get(index).getAsJsonObject());
	}

	public JSONObject getJSONObject(int index, JSONObject or) {
		JsonObject jo = get(index).getAsJsonObject();
		return jo == null ? or : new JSONObject(jo);
	}

	public long getLong(int index) {
		return get(index).getAsLong();
	}

	public long getLong(int index, long or) {
		try {
			return Helper.getOr(get(index).getAsLong(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public String getString(int index) {
		return get(index).getAsString();
	}

	public String getString(int index, String or) {
		try {
			return Helper.getOr(get(index).getAsString(), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public boolean isNull(int index) {
		return get(index).isJsonNull();
	}

	public String join(String separator) {
		List<String> out = new ArrayList<>();
		for (JsonElement jsonElement : arr) {
			out.add(arr.toString());
		}

		return String.join(separator, out);
	}

	public int size() {
		return arr.size();
	}

	public JSONArray put(boolean value) {
		arr.add(value);

		return this;
	}

	public JSONArray put(Collection<?> value) {
		arr.add(JSONUtils.parseJSONArray(value));

		return this;
	}

	public JSONArray put(double value) {
		arr.add(value);

		return this;
	}

	public JSONArray put(float value) {
		arr.add(value);

		return this;
	}

	public JSONArray put(int value) {
		arr.add(value);

		return this;
	}

	public JSONArray put(long value) {
		arr.add(value);

		return this;
	}

	public JSONArray put(Map<?, ?> value) {
		arr.add(JSONUtils.parseJSONObject(value));

		return this;
	}

	public JSONArray put(Object value) {
		arr.add(JSONUtils.parseJSONElement(value));

		return this;
	}

	public JsonElement remove(int index) {
		return arr.remove(index);
	}

	public JSONObject toJSONObject(JSONArray names) {
		JSONObject out = new JSONObject();
		for (int i = 0; i < names.size(); i++) {
			out.put(names.getString(i), arr.get(i));
		}

		return out;
	}

	public String toString() {
		return JSONUtils.toJSON(arr);
	}

	public List<JsonElement> toList() {
		List<JsonElement> out = new ArrayList<>();
		for (JsonElement elem : arr) {
			out.add(elem);
		}

		return out;
	}

	public boolean isEmpty() {
		return size() == 0;
	}
}
