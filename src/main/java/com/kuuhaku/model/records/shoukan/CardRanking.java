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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Utils;
import org.jetbrains.annotations.NotNull;

public record CardRanking(double winrate, int type, Drawable<?> card) implements Comparable<CardRanking> {
	@Override
	public int compareTo(@NotNull CardRanking other) {
		return Double.compare(other.winrate, winrate);
	}

	@Override
	public String toString() {
		String name = "`" + winrate + "%`";

		if (card instanceof Senshi s) {
			return Utils.getEmoteString(s.getRace().name()) + " " + name + " " + card;
		} else if (card instanceof Evogear e) {
			return Utils.getEmoteString("tier_" + e.getTier()) + " " + name + " " + card;
		} else if (card instanceof Field f) {
			return switch (f.getType()) {
				case NONE -> name + " " + card;
				case DAY -> ":sunny: " + name + " " + card;
				case NIGHT -> ":crescent_moon: " + name + " " + card;
				case DUNGEON -> ":japanese_castle: " + name + " " + card;
			};
		}

		return name;
	}
}
