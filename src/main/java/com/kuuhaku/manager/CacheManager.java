/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import groovy.lang.Script;
import org.openjdk.jol.vm.VM;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class CacheManager {
	private final Cache<String, byte[]> resource = Caffeine.newBuilder()
			.expireAfterAccess(30, TimeUnit.MINUTES)
			.maximumWeight(512 * 1024 * 1024)
			.weigher((k, v) -> (int) Math.min(VM.current().sizeOf(v), Integer.MAX_VALUE))
			.build();

	private final Cache<String, String> locale = Caffeine.newBuilder()
			.expireAfterAccess(30, TimeUnit.MINUTES)
			.maximumSize(128)
			.build();

	private final Cache<String, Class<? extends Script>> script = Caffeine.newBuilder()
			.expireAfterAccess(30, TimeUnit.MINUTES)
			.maximumSize(128)
			.build();

	private final Cache<String, Pattern> pattern = Caffeine.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS)
			.maximumSize(128)
			.build();

	public Cache<String, byte[]> getResourceCache() {
		return resource;
	}

	public byte[] computeResource(String key, BiFunction<String, byte[], byte[]> mapper) {
		byte[] bytes = mapper.apply(key, resource.getIfPresent(key));
		if (bytes == null) return null;

		resource.put(key, bytes);
		return bytes;
	}

	public Cache<String, String> getLocaleCache() {
		return locale;
	}

	public String computeLocale(String key, BiFunction<String, String, String> mapper) {
		String value = mapper.apply(key, locale.getIfPresent(key));
		if (value == null) return null;

		locale.put(key, value);
		return value;
	}

	public Cache<String, Class<? extends Script>> getScriptCache() {
		return script;
	}

	public Class<? extends Script> computeScript(String key, BiFunction<String, Class<? extends Script>, Class<? extends Script>> mapper) {
		Class<? extends Script> value = mapper.apply(key, script.getIfPresent(key));
		if (value == null) return null;

		script.put(key, value);
		return value;
	}

	public Cache<String, Pattern> getPatternCache() {
		return pattern;
	}

	public Pattern computePattern(String key, BiFunction<String, Pattern, Pattern> mapper) {
		Pattern value = mapper.apply(key, pattern.getIfPresent(key));
		if (value == null) return null;

		pattern.put(key, value);
		return value;
	}
}
