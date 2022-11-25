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

package com.kuuhaku.model.records.shoukan.history;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.common.shoukan.Hand;

import java.util.List;

public record Match(Info info, List<Turn> turns) {
	public Match(Shoukan game) {
		this(
				new Info(
						game.getPlayers()[0],
						game.getPlayers()[1],
						game.getHands().values().stream()
								.filter(h -> h.getHP() == 0)
								.map(Hand::getSide)
								.findFirst().orElse(null)
				),
				game.getTurns()
		);
	}
}
