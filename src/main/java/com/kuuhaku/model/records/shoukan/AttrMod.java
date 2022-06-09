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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.interfaces.Drawable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public record AttrMod(Drawable source, int index, double value, AtomicInteger expiration) {
	public AttrMod(Drawable source, int index, double value, int expiration) {
		this(source, index, value, new AtomicInteger(expiration));
	}

	public AttrMod(Drawable source, int index, double value) {
		this(source, index, value, null);
	}

	public boolean isExpired() {
		return expiration != null && expiration.get() <= 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (index < 0) return false;
		AttrMod attrMod = (AttrMod) o;
		return index == attrMod.index && Objects.equals(source, attrMod.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, index);
	}
}
