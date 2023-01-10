/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import com.kuuhaku.model.records.CachedScript;
import com.kuuhaku.util.json.JSONObject;
import org.intellij.lang.annotations.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedScriptManager {
	private CachedScript cache;
	private final Map<String, Object> context = new HashMap<>();
	private final JSONObject storedProps = new JSONObject();
	private final AtomicInteger propHash = new AtomicInteger();

	public CachedScriptManager forScript(@Language("Groovy") String code) {
		if (cache == null || code.hashCode() != cache.hashCode()) {
			cache = new CachedScript(code);
		}

		return this;
	}

	public CachedScriptManager withConst(String key, Object value) {
		if (value == null || context.containsKey(key)) return this;
		context.put(key, value);

		return this;
	}

	public CachedScriptManager withVar(String key, Object value) {
		if (value == null) return this;

		context.compute(key, (k, v) -> {
			if (v == null || v.hashCode() != value.hashCode()) {
				return value;
			}

			return v;
		});

		return this;
	}

	public void run() {
		cache.run(context);
	}

	public JSONObject getStoredProps() {
		return storedProps;
	}

	public AtomicInteger getPropHash() {
		return propHash;
	}
}
