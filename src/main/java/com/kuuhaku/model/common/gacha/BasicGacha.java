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
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.util.Spawn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BasicGacha extends Gacha<Card> {
	public BasicGacha() {
		this(new ArrayList<>(DAO.queryAll(Card.class, "SELECT c FROM Card c WHERE c.rarity IN :allowed", List.of(Rarity.getActualRarities()))));
	}

	private BasicGacha(List<Card> pool) {
		super(new RandomList<>(2.5 - Spawn.getRarityMult()) {{
			pool.sort(Comparator.comparingInt(c -> -c.getRarity().getIndex()));

			for (Card card : pool) {
				add(card, 6 - card.getRarity().getIndex());
			}
		}}, 3);
	}
}
