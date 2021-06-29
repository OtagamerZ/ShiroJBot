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

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import dev.zacsweers.moshix.records.RecordsJsonAdapterFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONUtils {
	private static final Moshi moshi = new Moshi.Builder()
			.add(new RecordsJsonAdapterFactory())
			.add(OffsetDateTime.class, new Rfc3339DateJsonAdapter())
			.build();

	public static String toJSON(Object o) {
		return moshi.adapter(Object.class).toJson(o);
	}

	public static <T> T fromJSON(String json, Class<T> klass) {
		try {
			return moshi.adapter(klass).nullSafe().fromJson(json);
		} catch (IOException e) {
			Helper.logger(JSONUtils.class).warn(e + " | " + e.getStackTrace()[0]);
			Helper.logger(JSONUtils.class).warn(json);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(String json) {
		try {
			return (Map<String, Object>) moshi.adapter(Types.newParameterizedType(Map.class, String.class, Object.class)).fromJson(json);
		} catch (IOException e) {
			Helper.logger(JSONUtils.class).warn(e + " | " + e.getStackTrace()[0]);
			Helper.logger(JSONUtils.class).warn(json);
			return new HashMap<>();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Object> toList(String json) {
		try {
			return (List<Object>) moshi.adapter(Types.newParameterizedType(List.class, Object.class)).fromJson(json);
		} catch (IOException e) {
			Helper.logger(JSONUtils.class).warn(e + " | " + e.getStackTrace()[0]);
			Helper.logger(JSONUtils.class).warn(json);
			return new ArrayList<>();
		}
	}

	public static Map<String, Object> toMap(Object o) {
		return toMap(toJSON(o));
	}

	public static List<Object> toList(Object o) {
		return toList(toJSON(o));
	}
}
