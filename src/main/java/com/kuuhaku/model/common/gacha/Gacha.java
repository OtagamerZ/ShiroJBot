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

import com.kuuhaku.interfaces.annotations.GachaType;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.util.Spawn;
import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class Gacha {
	protected final RandomList<String> pool;

	public Gacha() {
		this(new RandomList<>(1 / (2.5 / Spawn.getRarityMult())));
	}

	public Gacha(RandomList<String> pool) {
		this.pool = pool;
	}

	public final List<String> getPool() {
		return List.copyOf(pool.values());
	}

	public final double rarityOf(String value) {
		return pool.entries().stream()
				.filter(e -> e.getSecond().equals(value))
				.mapToDouble(Pair::getFirst)
				.findFirst()
				.orElseThrow();
	}

	public List<String> draw() {
		GachaType type = getClass().getAnnotation(GachaType.class);
		if (type == null) return List.of();

		List<String> out = new ArrayList<>();
		for (int i = 0; i < type.prizes(); i++) {
			out.add(pool.get());
		}

		return List.copyOf(out);
	}
}
