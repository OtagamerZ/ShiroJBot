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

import com.kuuhaku.Constants;
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.exceptions.TargetException;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.intellij.lang.annotations.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedScriptManager<T> {
	private final Map<String, Object> context = new HashMap<>();
	private final JSONObject storedProps = new JSONObject();
	private final AtomicInteger propHash = new AtomicInteger();
	private final Object parent;

	@Language("Groovy")
	private String code;

	public CachedScriptManager(T parent) {
		this.parent = parent;
	}

	public CachedScriptManager<T> forScript(@Language("Groovy") String code) {
		this.code = code;
		return this;
	}

	public CachedScriptManager<T> withConst(String key, Object value) {
		if (value == null || context.containsKey(key)) return this;
		context.put(key, value);

		return this;
	}

	public CachedScriptManager<T> withVar(String key, Object value) {
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
		@Language("Groovy")
		String code = "/* " + parent + " */\n" + this.code;

		try {
			Utils.exec(code, context);
		} catch (Exception e) {
			if (!(e instanceof TargetException || e instanceof ActivationException)) {
				Constants.LOGGER.warn("Failed to execute " + parent + " effect\n" + code, e);
			}

			throw e;
		}
	}

	public JSONObject getStoredProps() {
		return storedProps;
	}

	public AtomicInteger getPropHash() {
		return propHash;
	}
}
