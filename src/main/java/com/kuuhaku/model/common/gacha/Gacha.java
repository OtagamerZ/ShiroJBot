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

package com.kuuhaku.model.common.gacha;

import com.kuuhaku.interfaces.annotations.GachaType;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.Spawn;
import com.kuuhaku.util.Utils;
import kotlin.Pair;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class Gacha {
	private static final Reflections refl = new Reflections("com.kuuhaku.model.common.gacha");
	private static final Set<Class<?>> gachas = refl.getTypesAnnotatedWith(GachaType.class);

	protected final RandomList<String> pool;

	public Gacha(List<Object[]> pool) {
		this(new RandomList<>(Spawn.getRarityMult()), pool);
	}

	public Gacha(RandomList<String> pool, List<Object[]> data) {
		this.pool = pool;
		for (Object[] card : data) {
			this.pool.add((String) card[0], ((Number) card[1]).doubleValue());
		}
	}

	public final List<String> getPool() {
		return pool.entries().stream().map(Pair::getSecond).toList();
	}

	public final double weightOf(String value) {
		return pool.entries().parallelStream()
				.filter(e -> e.getSecond().equals(value))
				.mapToDouble(Pair::getFirst)
				.findAny()
				.orElseThrow();
	}

	public final boolean rollOutput(String a, String b, String favor) {
		if (a.equals(favor)) return false;
		else if (b.equals(favor)) return true;
		else return weightOf(b) < weightOf(a);
	}

	public List<String> draw(Account acc) {
		GachaType type = getClass().getAnnotation(GachaType.class);
		if (type == null) return List.of();

		List<String> out = new ArrayList<>();
		String fav = acc.getKawaipon().getFavCardId();
		int extra = Math.max(0, acc.getItemCount("extra_draw"));
		boolean lucky = acc.consumeItem("lucky_lodestone");
		for (int i = 0; i < type.prizes() + extra; i++) {
			if (lucky) {
				out.add(Utils.luckyRoll(pool::get, (a, b) -> rollOutput(a, b, fav)));
			} else {
				out.add(pool.get());
			}
		}

		acc.consumeItem("extra_draw", extra, true);
		return out;
	}

	public static Set<Class<?>> getGachas() {
		return gachas;
	}
}
