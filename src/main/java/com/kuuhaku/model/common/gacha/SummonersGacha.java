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
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;

public class SummonersGacha extends Gacha<String> {
	public SummonersGacha() {
		this(DAO.queryAllUnmapped("""
				SELECT x.id
				     , x.weight
				FROM (
				     SELECT c.id
				          , get_weight(c.id) AS weight
				     FROM card c
				     INNER JOIN senshi s ON c.id = s.card_id
				     WHERE NOT has(s.tags, 'FUSION')
				     ) x
				WHERE x.weight IS NOT NULL
				ORDER BY x.weight, x.id
				"""));
	}

	private SummonersGacha(List<Object[]> pool) {
		super(6200);
		for (Object[] card : pool) {
			this.pool.add((String) card[0], NumberUtils.toDouble(String.valueOf(card[1])));
		}
	}
}
