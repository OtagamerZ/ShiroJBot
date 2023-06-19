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

package com.kuuhaku.model.records.shoukan.snapshot;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.enums.shoukan.Side;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public record StateSnap(Global global, Map<Side, Player> players, Map<Side, List<Slot>> slots) {
	public StateSnap(Shoukan game) throws IOException {
		this(
				new Global(game),
				Map.of(
						Side.TOP, new Player(game.getHands().get(Side.TOP)),
						Side.BOTTOM, new Player(game.getHands().get(Side.BOTTOM))
				),
				Map.of(
						Side.TOP, game.getSlots(Side.TOP).stream().map(s -> {
							try {
								return new Slot(s);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}).toList(),
						Side.BOTTOM, game.getSlots(Side.BOTTOM).stream().map(s -> {
							try {
								return new Slot(s);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}).toList()
				)
		);
	}
}
