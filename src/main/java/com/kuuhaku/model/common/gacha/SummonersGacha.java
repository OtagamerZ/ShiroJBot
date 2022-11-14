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
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.util.Spawn;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;

public class SummonersGacha extends Gacha<String> {
	public SummonersGacha() {
		this(DAO.queryAllUnmapped("""
				SELECT c.id
				     , CASE
				           WHEN s.card_id IS NOT NULL THEN 6.0 - get_rarity_index(c.rarity)
				           WHEN e.card_id IS NOT NULL THEN (5.0 - e.tier) / 2
				           ELSE 0.025
				    END
				FROM card c
				         LEFT JOIN senshi s ON c.id = s.card_id AND get_type(c.id) & 2 = 2 AND get_rarity_index(c.rarity) BETWEEN 1 AND 5 AND NOT has(tags, 'FUSION')
				         LEFT JOIN evogear e ON c.id = e.card_id AND e.tier > 0
				         LEFT JOIN field f ON c.id = f.card_id AND NOT f.effect
				WHERE (s.card_id IS NOT NULL OR e.card_id IS NOT NULL OR f.card_id IS NOT NULL)
				ORDER BY 2
				"""));
	}

	private SummonersGacha(List<Object[]> pool) {
		super(6200, Currency.CR, 3,
				new RandomList<>(2.5 - Spawn.getRarityMult()) {{
					for (Object[] card : pool) {
						add((String) card[0], NumberUtils.toDouble(String.valueOf(card[1])));
					}
				}}
		);
	}
}
