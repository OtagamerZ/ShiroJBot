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

import java.time.LocalDate;
import java.time.temporal.ChronoField;

@GachaType(value = "daily", price = 3500, currency = Currency.CR)
public class DailyGacha extends Gacha {
	public DailyGacha(User u) {
		super(DAO.queryAllUnmapped("""
				SELECT x.id
				     , x.weight
				FROM (
				     SELECT c.id
				          , get_weight(c.id, ?1) AS weight
				     FROM card c
				              LEFT JOIN anime a ON a.id = c.anime_id
				              LEFT JOIN senshi s ON c.id = s.card_id
				              LEFT JOIN evogear e ON c.id = e.card_id
				              LEFT JOIN field f ON c.id = f.card_id
				     WHERE coalesce(a.visible, TRUE) = TRUE
				       AND (
				             get_rarity_index(c.rarity) BETWEEN 1 AND 5
				             OR e.tier > 0
				             OR NOT f.effect
				         )
				     ORDER BY hashtextextended(c.id, ?2)
				     LIMIT 50
				     ) x
				ORDER BY x.weight, x.id
				""", u.getId(), LocalDate.now().get(ChronoField.DAY_OF_YEAR)));
	}
}
