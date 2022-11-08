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

package com.kuuhaku.model.records;

import com.kuuhaku.util.Utils;
import groovy.lang.Script;
import org.intellij.lang.annotations.Language;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public record CachedScript(@Language("Groovy") String code, AtomicReference<Script> script) {
	public CachedScript(@Language("Groovy") String code) {
		this(code, new AtomicReference<>());
	}

	public void run(Map<String, Object> variables) {
		Script ref = script.get();
		if (ref == null) {
			script.set(ref = Utils.compile(code));
		}

		for (Map.Entry<String, Object> e : variables.entrySet()) {
			ref.getBinding().setVariable(e.getKey(), e.getValue());
		}

		ref.run();
	}

	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
