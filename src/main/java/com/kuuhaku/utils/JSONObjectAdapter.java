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

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Iterator;

public class JSONObjectAdapter implements JsonSerializer<JSONObject>, JsonDeserializer<JSONObject> {
	@Override
	public JsonElement serialize(JSONObject src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null) {
			return null;
		}

		JsonObject jo = new JsonObject();
		Iterator<String> keys = src.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JsonElement value = src.get(key);

			jo.add(key, value);
		}

		return jo;
	}

	@Override
	public JSONObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (json == null) {
			return null;
		}

		return new JSONObject(json.toString());
	}
}
