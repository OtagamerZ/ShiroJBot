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
import com.kuuhaku.model.enums.JsonType;

public class JSONUtils {
	private static final Gson gson = new GsonBuilder()
			.registerTypeAdapter(JSONObject.class, JSONObjectSerializer.class)
			.registerTypeAdapter(JSONArray.class, JSONArraySerializer.class)
			.create();

	public static String toJSON(Object o) {
		return gson.toJson(o);
	}

	public static <T> T fromJSON(String json, Class<T> klass) {
		return gson.fromJson(json, klass);
	}

	public static JsonObject parseJSONObject(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}

	public static JsonArray parseJSONArray(String json) {
		return JsonParser.parseString(json).getAsJsonArray();
	}

	public static JsonElement parseJSONElement(String json) {
		return JsonParser.parseString(json);
	}

	public static JsonObject parseJSONObject(Object o) {
		return parseJSONObject(gson.toJson(o));
	}

	public static JsonArray parseJSONArray(Object o) {
		return parseJSONArray(gson.toJson(o));
	}

	public static JsonElement parseJSONElement(Object o) {
		return parseJSONElement(gson.toJson(o));
	}

	public static JsonType getType(String json) {
		JsonElement je = JsonParser.parseString(json);
		return je.isJsonObject() ? JsonType.OBJECT : JsonType.ARRAY;
	}
}
