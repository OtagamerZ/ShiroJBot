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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.annotations.GachaType;
import com.kuuhaku.model.enums.Currency;
import net.dv8tion.jda.api.entities.User;

@GachaType(value = "spooky", price = 75, prizes = 3, currency = Currency.ITEM, itemCostId = "SPOOKY_CANDY")
public class SpookyGacha extends Gacha {
	public SpookyGacha(User u) {
		super(DAO.queryAllUnmapped("""
				SELECT x.id
				     , cast(x.weight + x.candies / x.mult AS INT) AS weight
				FROM (
				     SELECT c.id
				          , get_weight(c.id, ?1)                                      AS weight
				          , coalesce(cast(acc.inventory -> 'SPOOKY_CANDY' AS INT), 0) AS candies
				          , CASE c.rarity
				                WHEN 'EVOGEAR' THEN 4
				                WHEN 'FIELD' THEN 8
				                ELSE 10 / get_rarity_index(c.rarity)
				         END                                                          AS mult
				     FROM card c
				              INNER JOIN anime a ON a.id = c.anime_id
				              INNER JOIN account acc ON acc.uid = ?1
				     WHERE a.visible
				       AND is_valid_rarity(c.rarity)
				     ) x
				ORDER BY x.weight, x.id
				""", u.getId()));
	}
}
