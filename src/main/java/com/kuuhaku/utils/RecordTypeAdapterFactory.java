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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;

public class RecordTypeAdapterFactory implements TypeAdapterFactory {

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) type.getRawType();
		if (!clazz.isRecord()) {
			return null;
		}
		TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

		return new TypeAdapter<>() {
			@Override
			public void write(JsonWriter out, T value) throws IOException {
				delegate.write(out, value);
			}

			@Override
			public T read(JsonReader reader) throws IOException {
				if (reader.peek() == JsonToken.NULL) {
					reader.nextNull();
					return null;
				} else {
					RecordComponent[] recordComponents = clazz.getRecordComponents();
					HashMap<String, TypeToken<?>> typeMap = new HashMap<>();

					for (RecordComponent recordComponent : recordComponents) {
						typeMap.put(recordComponent.getName(), TypeToken.get(recordComponent.getGenericType()));
					}

					HashMap<String, Object> argsMap = new HashMap<>();
					reader.beginObject();
					while (reader.hasNext()) {
						String name = reader.nextName();
						argsMap.put(name, gson.getAdapter(typeMap.get(name)).read(reader));
					}
					reader.endObject();

					var argTypes = new Class<?>[recordComponents.length];
					var args = new Object[recordComponents.length];
					for (int i = 0; i < recordComponents.length; i++) {
						argTypes[i] = recordComponents[i].getType();
						args[i] = argsMap.get(recordComponents[i].getName());
					}

					Constructor<T> constructor;
					try {
						constructor = clazz.getDeclaredConstructor(argTypes);
						constructor.setAccessible(true);
						return constructor.newInstance(args);
					} catch (NoSuchMethodException | InstantiationException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException(e);
					}
				}
			}
		};
	}
}