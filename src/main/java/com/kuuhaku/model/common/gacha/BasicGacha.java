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

public class BasicGacha extends Gacha<String> {
	public BasicGacha() {
		this(DAO.queryAllUnmapped("""
				SELECT c.id
				     , 6. - get_rarity_index(c.rarity)
				FROM card c
				WHERE get_rarity_index(c.rarity) BETWEEN 1 AND 5
				ORDER BY 2
				"""));
	}

	private BasicGacha(List<Object[]> pool) {
		super(2800, Currency.CR, 3,
				new RandomList<>(2.5 - Spawn.getRarityMult()) {{
					for (Object[] card : pool) {
						add((String) card[0], NumberUtils.toDouble(String.valueOf(card[1])));
					}
				}}
		);
	}
}
