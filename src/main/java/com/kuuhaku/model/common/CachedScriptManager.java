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

package com.kuuhaku.model.common;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.shoukan.Props;
import com.kuuhaku.util.Utils;
import org.intellij.lang.annotations.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedScriptManager {
	private final Map<String, Object> context = new HashMap<>();
	private final Props storedProps = new Props();
	private final AtomicInteger propHash = new AtomicInteger();

	@Language("Groovy")
	private String code;
	private Drawable<?> owner;

	public CachedScriptManager() {
		context.put("props", storedProps);
	}

	public CachedScriptManager forScript(@Language("Groovy") String code) {
		this.code = code;
		return this;
	}

	public CachedScriptManager withConst(String key, Object value) {
		context.putIfAbsent(key, value);
		return this;
	}

	public CachedScriptManager withVar(String key, Object value) {
		context.put(key, value);
		return this;
	}

	public CachedScriptManager assertOwner(Drawable<?> owner, Runnable elseDo) {
		if (!Objects.equals(this.owner, owner)) {
			storedProps.clear();
			propHash.set(0);

			this.owner = owner;
			elseDo.run();
		}

		return this;
	}

	public void run() {
		Utils.exec(owner.toString(), code, context);
	}

	public Props getStoredProps() {
		return storedProps;
	}

	public AtomicInteger getPropHash() {
		return propHash;
	}
}
