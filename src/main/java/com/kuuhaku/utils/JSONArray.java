/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

public class JSONArray implements JSONWrapper, Iterable<Object> {
	private final List<Object> arr;

	public JSONArray() {
		arr = new ArrayList<>();
	}

	public JSONArray(List<Object> list) {
		arr = list;
	}

	public JSONArray(String json) {
		this(JSONUtils.toList(json));
	}

	public JSONArray(Collection<?> collection) {
		this(JSONUtils.toJSON(collection));
	}

	public JSONArray(Object array) {
		this(JSONUtils.toJSON(array));
	}

	public Iterator<Object> iterator() {
		return arr.iterator();
	}

	public Object get(int index) {
		return arr.get(index);
	}

	public Object get(int index, Object or) {
		return Helper.getOr(get(index), or);
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
		try {
			return (boolean) get(index);
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
	}

	public boolean getBoolean(int index, boolean or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public double getDouble(int index) {
		try {
			return (double) get(index);
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public double getDouble(int index, double or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public float getFloat(int index) {
		try {
			return (float) get(index);
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public float getFloat(int index, float or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public int getInt(int index) {
		return (int) get(index);
	}

	public int getInt(int index, int or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public long getLong(int index) {
		try {
			return (long) get(index);
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public long getLong(int index, long or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public String getString(int index) {
		try {
			return (String) get(index);
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}

	public String getString(int index, String or) {
		try {
			return Helper.getOr(get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public JSONArray getJSONArray(int index) {
		try {
			return new JSONArray(get(index));
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public JSONArray getJSONArray(int index, JSONArray or) {
		try {
			List<?> ja = (List<?>) get(index);
			return ja == null ? or : new JSONArray(ja);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public JSONObject getJSONObject(int index) {
		try {
			return new JSONObject(get(index));
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public JSONObject getJSONObject(int index, JSONObject or) {
		try {
			Map<?, ?> jo = (Map<?, ?>) get(index);
			return jo == null ? or : new JSONObject(jo);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public boolean contains(Object value) {
		return toList().stream().anyMatch(value::equals);
	}

	public JSONArray increment(int index) {
		Object obj = get(index);
		if (obj == null) return this;

		if (obj instanceof Number n) {
			arr.set(index, n.longValue() + 1);
		}

		return this;
	}

	public String join(String separator) {
		List<String> out = new ArrayList<>();
		for (Object elem : arr) {
			out.add(elem.toString());
		}

		return String.join(separator, out);
	}

	public JSONArray put(Object value) {
		arr.add(value);

		return this;
	}

	public Object remove(int index) {
		return arr.remove(index);
	}

	public JSONObject toJSONObject(JSONArray names) {
		JSONObject out = new JSONObject();
		for (int i = 0; i < names.size(); i++) {
			out.put(names.getString(i), arr.get(i));
		}

		return out;
	}

	public List<Object> toList() {
		return arr;
	}

	@Override
	public Object getContent() {
		return arr;
	}

	@Override
	public int size() {
		return arr.size();
	}

	@Override
	public boolean isEmpty() {
		return arr.isEmpty();
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(arr);
	}
}
