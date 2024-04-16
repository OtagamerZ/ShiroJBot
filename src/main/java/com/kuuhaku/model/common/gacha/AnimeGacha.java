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
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.User;

import java.util.Calendar;

@GachaType(value = "anime", price = 10, prizes = 5, currency = Currency.ITEM, itemCostId = "MASTERY_TOKEN")
public class AnimeGacha extends Gacha {
	public AnimeGacha(User u) {
		super(DAO.queryAllUnmapped("""
				SELECT c.id
				     , get_weight(c.id, ?1) AS weight
				FROM (
				         SELECT a.id
				         FROM anime a
				         WHERE a.visible
				           AND (SELECT count(1) FROM card c WHERE c.anime_id = a.id) > 10
				         ORDER BY hashtextextended(a.id, ?2)
				         LIMIT 1
				     ) x
				INNER JOIN card c ON c.anime_id = x.id
				WHERE get_rarity_index(c.rarity) BETWEEN 1 AND 5
				   OR CASE c.rarity
				          WHEN 'EVOGEAR' THEN (SELECT e.tier > 0 FROM evogear e WHERE e.card_id = c.id)
				          WHEN 'FIELD' THEN (SELECT NOT f.effect FROM field f WHERE f.card_id = c.id)
				   END
				ORDER BY weight, c.id
				""", u.getId(), Utils.with(Calendar.getInstance(), c -> c.get(Calendar.YEAR) + c.get(Calendar.WEEK_OF_YEAR))));
	}
}
