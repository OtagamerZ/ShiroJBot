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

package com.kuuhaku.model.records.shoukan.history;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kuuhaku.model.enums.shoukan.Side.BOTTOM;
import static com.kuuhaku.model.enums.shoukan.Side.TOP;
import static com.kuuhaku.util.Utils.map;
import static com.kuuhaku.util.Utils.safeGet;

public record Turn(int turn, Side top, Side bottom, List<String> banned, String field) {
	public static Turn from(Shoukan game) {
		Hand top = game.getHands().get(TOP);
		Hand bot = top.getOther();

		return new Turn(
				game.getTurn(),
				makeSide(game, top, TOP),
				makeSide(game, bot, BOTTOM),
				Drawable.ids(game.getArena().getBanned()),
				game.getArena().getField().getId()
		);
	}

	private static Side makeSide(Shoukan game, Hand bot, com.kuuhaku.model.enums.shoukan.Side side) {
		return new Side(
				bot.getHP(),
				bot.getMP(),
				bot.getRegDeg().peek(),
				new Locks(Arrays.stream(Lock.values()).collect(Collectors.toMap(
						Function.identity(), l -> bot.getLockTime(Lock.EFFECT)
				))),
				Drawable.ids(bot.getCards()),
				Drawable.ids(bot.getDeck()),
				Drawable.ids(bot.getGraveyard()),
				map(game.getSlots(side), sc ->
						new Slot(
								safeGet(sc.getTop(), Senshi::getId),
								safeGet(sc.getTop(), s -> Drawable.ids(s.getEquipments())),
								safeGet(sc.getBottom(), Senshi::getId),
								sc.getLock()
						)
				)
		);
	}
}
