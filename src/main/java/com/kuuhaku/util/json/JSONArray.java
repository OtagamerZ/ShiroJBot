/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import java.util.*;

public class JSONArray extends ArrayList<Object> implements Cloneable {
	@Serial
	private static final long serialVersionUID = -2826952916406184142L;

	public JSONArray() {
	}

	public JSONArray(List<Object> list) {
		addAll(list);
	}

	public JSONArray(@Language("JSON5") String json) {
		this(JSONUtils.toList(json));
	}

	public JSONArray(Collection<?> collection) {
		this(JSONUtils.toJSON(collection));
	}

	public JSONArray(Object array) {
		this(String.valueOf(array));
	}

	public Object get(int index) {
		return super.get(index);
	}

	public Object get(int index, Object or) {
		return Utils.getOr(get(index), or);
	}

	public <T> T get(Class<T> klass, int index) {
		try {
			return klass.cast(get(index));
		} catch (ClassCastException e) {
			return null;
		}
	}

	public <T> T get(Class<T> klass, int index, T or) {
		return Utils.getOr(get(klass, index), or);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, int index) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equalsIgnoreCase(getString(index)))
				.findFirst()
				.orElse(null);
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, int index, E or) {
		if (!clazz.isEnum()) return null;
		return Arrays.stream(clazz.getEnumConstants())
				.filter(e -> e.name().equalsIgnoreCase(getString(index)))
				.findFirst()
				.orElse(or);
	}

	public boolean getBoolean(int index) {
		try {
			return (boolean) get(index);
		} catch (IndexOutOfBoundsException e) {
			return false;
		} catch (ClassCastException e) {
			return get(index) != null;
		}
	}

	public boolean getBoolean(int index, boolean or) {
		try {
			return Utils.getOr((boolean) get(index), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public double getDouble(int index) {
		try {
			return ((Number) get(index)).doubleValue();
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public double getDouble(int index, double or) {
		try {
			return Utils.getOr(((Number) get(index)).doubleValue(), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public float getFloat(int index) {
		try {
			return ((Number) get(index)).floatValue();
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public float getFloat(int index, float or) {
		try {
			return Utils.getOr(((Number) get(index)).floatValue(), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public int getInt(int index) {
		return ((Number) get(index)).intValue();
	}

	public int getInt(int index, int or) {
		try {
			return Utils.getOr(((Number) get(index)).intValue(), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public long getLong(int index) {
		try {
			return ((Number) get(index)).longValue();
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

	public long getLong(int index, long or) {
		try {
			return Utils.getOr(((Number) get(index)).longValue(), or);
		} catch (IndexOutOfBoundsException e) {
			return or;
		}
	}

	public String getString(int index) {
		try {
			return String.valueOf(get(index));
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}

	public String getString(int index, String or) {
		try {
			return Utils.getOr(String.valueOf(get(index)), or);
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

	public JSONArray increment(int index) {
		Object obj = get(index);
		if (obj == null) return this;

		if (obj instanceof Number n) {
			set(index, n.longValue() + 1);
		}

		return this;
	}

	public String join(String separator) {
		List<String> out = new ArrayList<>();
		for (Object elem : this) {
			out.add(elem.toString());
		}

		return String.join(separator, out);
	}

	public JSONArray put(Object value) {
		add(value);

		return this;
	}

	public Object rightShift(int index) {
		return remove(index);
	}

	public boolean rightShift(Object obj) {
		return remove(obj);
	}

	public JSONObject toJSONObject(JSONArray names) {
		JSONObject out = new JSONObject();
		for (int i = 0; i < names.size(); i++) {
			out.put(names.getString(i), get(i));
		}

		return out;
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}

	@Override
	public JSONArray clone() {
		return (JSONArray) super.clone();
	}
}
