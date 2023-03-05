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
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;

@GachaType(value = "premium", price = 1, prizes = 5, currency = Currency.GEM)
public class PremiumGacha extends Gacha {
	public PremiumGacha(User u) {
		this(DAO.queryAllUnmapped("""
				SELECT x.id
				     , x.weight
				FROM (
				     SELECT c.id
				          , get_weight(c.id, ?1) AS weight
				     FROM card c
				     ) x
				WHERE x.weight IS NOT NULL
				ORDER BY x.weight, x.id
				""", u.getId()));
	}

	private PremiumGacha(List<Object[]> pool) {
		super(new RandomList<>(2.5 * Spawn.getRarityMult()), pool);
	}

	@Override
	public List<String> draw() {
		GachaType type = getClass().getAnnotation(GachaType.class);
		if (type == null) return List.of();

		List<String> out = new ArrayList<>();
		for (int i = 0; i < type.prizes(); i++) {
			String roll = null;
			for (int j = 0; j < 10; j++) {
				String id = pool.get();
				if (roll == null || rarityOf(id) > rarityOf(roll)) {
					roll = id;
				}
			}

			out.add(roll);
		}

		return out;
	}
}
