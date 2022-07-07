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

public class JSONObject extends HashMap<String, Object> {
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
		this(String.valueOf(bean));
	}

	public Iterator<Entry<String, Object>> iterator() {
		return entrySet().iterator();
	}

	public Object get(String key) {
		Object o = super.get(key);
		if (o == null) throw new NullPointerException();

		return o;
	}

	public Object get(String key, Object or) {
		return Utils.getOr(get(key), or);
	}

	public <T> T get(Class<T> klass, String key) {
		try {
			return klass.cast(get(key));
		} catch (NullPointerException | ClassCastException e) {
			return null;
		}
	}

	public <T> T get(Class<T> klass, String key, T or) {
		return Utils.getOr(get(klass, key), or);
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
		} catch (ClassCastException e) {
			return get(key) != null;
		}
	}

	public boolean getBoolean(String key, boolean or) {
		try {
			return Utils.getOr((boolean) get(key), or);
		} catch (NullPointerException e) {
			return or;
		}
	}

	public double getDouble(String key) {
		try {
			return ((Number) get(key)).doubleValue();
		} catch (NullPointerException e) {
			return 0;
		} catch (ClassCastException e) {
			try {
				return Double.parseDouble((String) get(key));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public double getDouble(String key, double or) {
		try {
			return Utils.getOr(((Number) get(key)).doubleValue(), or);
		} catch (NullPointerException e) {
			return or;
		} catch (ClassCastException e) {
			try {
				return Double.parseDouble((String) get(key));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public float getFloat(String key) {
		try {
			return ((Number) get(key)).floatValue();
		} catch (NullPointerException e) {
			return 0;
		} catch (ClassCastException e) {
			try {
				return Float.parseFloat((String) get(key));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public float getFloat(String key, float or) {
		try {
			return Utils.getOr(((Number) get(key)).floatValue(), or);
		} catch (NullPointerException e) {
			return or;
		} catch (ClassCastException e) {
			try {
				return Float.parseFloat((String) get(key));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public int getInt(String key) {
		try {
			return ((Number) get(key)).intValue();
		} catch (NullPointerException e) {
			return 0;
		} catch (ClassCastException e) {
			try {
				return Integer.parseInt((String) get(key));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public int getInt(String key, int or) {
		try {
			return Utils.getOr(((Number) get(key)).intValue(), or);
		} catch (NullPointerException e) {
			return or;
		} catch (ClassCastException e) {
			try {
				return Integer.parseInt((String) get(key));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public long getLong(String key) {
		try {
			return ((Number) get(key)).longValue();
		} catch (NullPointerException e) {
			return 0;
		} catch (ClassCastException e) {
			try {
				return Long.parseLong((String) get(key));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public long getLong(String key, long or) {
		try {
			return Utils.getOr(((Number) get(key)).longValue(), or);
		} catch (NullPointerException e) {
			return or;
		} catch (ClassCastException e) {
			try {
				return Long.parseLong((String) get(key));
			} catch (NumberFormatException ex) {
				return or;
			}
		}
	}

	public String getString(String key) {
		try {
			return String.valueOf(get(key));
		} catch (NullPointerException e) {
			return "";
		}
	}

	public String getString(String key, String or) {
		try {
			return Utils.getOr(String.valueOf(get(key)), or);
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
		try {
			List<?> ar = (List<?>) get(key);
			return ar == null ? or : new JSONArray(ar);
		} catch (NullPointerException e) {
			return or;
		}
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

	public JSONArray toJSONArray() {
		return new JSONArray(values());
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}
}
