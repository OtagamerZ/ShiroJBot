/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import org.apache.commons.lang3.tuple.Pair;

public class Duelists {
	private final Pair<Champion, Champion> duelists;

	private Duelists(Champion attacker, Champion defender) {
		this.duelists = Pair.of(attacker, defender);
	}

	public static Duelists of(Champion attacker, Champion defender) {
		return new Duelists(attacker, defender);
	}

	public Champion getAttacker() {
		return duelists.getLeft();
	}

	public Champion getDefender() {
		return duelists.getRight();
	}
}
