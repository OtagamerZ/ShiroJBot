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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.annotations.GachaType;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.util.Spawn;

import java.util.List;

@GachaType(value = "premium", price = 1, prizes = 5, currency = Currency.GEM)
public class PremiumGacha extends Gacha {
	public PremiumGacha() {
		this(DAO.queryAllUnmapped("""
				SELECT x.id
				     , x.weight
				FROM (
				     SELECT c.id
				          , get_weight(c.id) AS weight
				     FROM card c
				     ) x
				WHERE x.weight IS NOT NULL
				ORDER BY x.weight, x.id
				"""));
	}

	private PremiumGacha(List<Object[]> pool) {
		super(new RandomList<>(1 / (Spawn.getRarityMult() / 1.75)), pool);
	}

	@Override
	public List<String> draw() {
		List<String> out = super.draw();
		List<String> aux = super.draw();

		for (int i = 0; i < out.size(); i++) {
			String first = out.get(i);
			String second = aux.get(i);

			if (rarityOf(second) > rarityOf(first)) {
				out.set(i, second);
			}
		}

		return out;
	}
}
