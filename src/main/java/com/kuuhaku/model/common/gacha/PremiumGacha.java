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
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.Spawn;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;

@GachaType(value = "premium", price = 1, prizes = 5, currency = Currency.GEM)
public class PremiumGacha extends Gacha {
	public PremiumGacha(User u) {
		this(DAO.queryAllUnmapped("""
				SELECT c.id
				     , get_weight(c.id, ?1) AS weight
				FROM card c
				         LEFT JOIN anime a ON a.id = c.anime_id
				         LEFT JOIN senshi s ON c.id = s.card_id
				         LEFT JOIN evogear e ON c.id = e.card_id
				         LEFT JOIN field f ON c.id = f.card_id
				WHERE is_valid_rarity(c.rarity)
				  AND (
				        (coalesce(a.visible, TRUE) = TRUE
				            AND get_rarity_index(c.rarity) BETWEEN 3 AND 5
				            AND NOT has(s.tags, 'FUSION'))
				        OR e.tier > 2
				        OR NOT f.effect
				    )
				ORDER BY weight, c.id
				""", u.getId()));
	}

	private PremiumGacha(List<Object[]> pool) {
		super(new RandomList<>(Spawn.getRarityMult() * 1.5), pool);
	}

	@Override
	public List<String> draw(Account acc) {
		GachaType type = getClass().getAnnotation(GachaType.class);
		if (type == null) return List.of();

		List<String> out = new ArrayList<>();
		String fav = acc.getKawaipon().getFavCardId();
		int extra = acc.consumeItem("extra_draw") ? 1 : 0;
		for (int i = 0; i < type.prizes() + extra; i++) {
			out.add(Utils.luckyRoll(pool::get, (a, b) -> rollOutput(a, b, fav)));
		}

		return out;
	}
}
