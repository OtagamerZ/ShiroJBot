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
import org.apache.commons.lang3.math.NumberUtils;
import org.reflections8.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class Gacha {
	private static final Reflections refl = new Reflections("com.kuuhaku.model.common.gacha");
	private static final Set<Class<?>> gachas = refl.getTypesAnnotatedWith(GachaType.class);

	protected final RandomList<String> pool;

	public Gacha(List<Object[]> pool) {
		this(new RandomList<>(1 / (Spawn.getRarityMult() / 2.5)), pool);
	}

	public Gacha(RandomList<String> pool, List<Object[]> data) {
		this.pool = pool;
		for (Object[] card : data) {
			this.pool.add((String) card[0], NumberUtils.toDouble(String.valueOf(card[1])));
		}
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

	public static Set<Class<?>> getGachas() {
		return gachas;
	}
}
