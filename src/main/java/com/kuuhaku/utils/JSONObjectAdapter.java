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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

public class JSONObjectAdapter extends TypeAdapter<JSONObject> implements JsonSerializer<JSONObject>, JsonDeserializer<JSONObject> {
	@Override
	public JsonElement serialize(JSONObject src, Type typeOfSrc, JsonSerializationContext context) {
		return src.getObj();
	}

	@Override
	public JSONObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return new JSONObject(json);
	}

	@Override
	public void write(JsonWriter out, JSONObject value) throws IOException {

	}

	@Override
	public JSONObject read(JsonReader in) throws IOException {
		return new JSONObject();
	}
}
