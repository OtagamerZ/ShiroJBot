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

public class JSONArrayAdapter extends TypeAdapter<JSONArray> implements JsonSerializer<JSONArray>, JsonDeserializer<JSONArray> {
	@Override
	public JsonElement serialize(JSONArray src, Type typeOfSrc, JsonSerializationContext context) {
		return src.getArr();
	}

	@Override
	public JSONArray deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return new JSONArray(json);
	}

	@Override
	public void write(JsonWriter out, JSONArray value) throws IOException {
		out.endArray();
		out.name("arr");
		out.value(value.toString());
		out.endArray();
	}

	@Override
	public JSONArray read(JsonReader in) throws IOException {
		JSONArray jo;

		in.beginArray();
		jo = new JSONArray(in.nextString());
		in.endArray();

		return jo;
	}
}
