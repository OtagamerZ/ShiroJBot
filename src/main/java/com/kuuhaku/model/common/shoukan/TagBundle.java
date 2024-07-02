/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.model.records.shoukan.CardTag;

import java.util.LinkedHashSet;

public class TagBundle extends LinkedHashSet<CardTag> {
	public void add(String prefix, String name) {
		super.add(new CardTag(prefix, name));
	}

	public boolean contains(String o) {
		return isCase(o);
	}

	public boolean isCase(String o) {
		return parallelStream()
				.map(CardTag::name)
				.anyMatch(o::equalsIgnoreCase);
	}
}