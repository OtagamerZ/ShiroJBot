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
import com.kuuhaku.model.enums.Currency;
import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class Gacha<T> {
	private final int price;
	private final Currency currency;
	private final RandomList<T> pool;
	private final int prizeCount;

	public Gacha(int price, Currency currency, int prizeCount, RandomList<T> pool) {
		this.price = price;
		this.currency = currency;
		this.prizeCount = prizeCount;
		this.pool = pool;
	}

	public int getPrice() {
		return price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public final List<T> getPool() {
		return List.copyOf(pool.values());
	}

	public final double rarityOf(T value) {
		return pool.entries().stream()
				.filter(e -> e.getSecond().equals(value))
				.mapToDouble(Pair::getFirst)
				.findFirst()
				.orElse(0);
	}

	public final int getPrizeCount() {
		return prizeCount;
	}

	public List<T> draw() {
		List<T> out = new ArrayList<>();
		for (int i = 0; i < prizeCount; i++) {
			out.add(pool.get());
		}

		return List.copyOf(out);
	}
}
