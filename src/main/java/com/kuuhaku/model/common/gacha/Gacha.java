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

package com.kuuhaku.model.common.gacha;

import com.kuuhaku.model.common.RandomList;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public abstract class Gacha<T> {
	private final RandomList<T> pool;
	private final int prizeCount;

	public Gacha(RandomList<T> pool, int prizeCount) {
		this.pool = pool;
		this.prizeCount = prizeCount;
	}

	public final List<T> getPool() {
		return List.copyOf(pool.getValues());
	}

	public final int getPrizeCount() {
		return prizeCount;
	}

	@SuppressWarnings("unchecked")
	public final List<T> draw() {
		List<T> out = Arrays.asList((T[]) Array.newInstance(pool.getClass().getComponentType(), prizeCount));
		out.replaceAll(t -> pool.get());

		return out;
	}
}
