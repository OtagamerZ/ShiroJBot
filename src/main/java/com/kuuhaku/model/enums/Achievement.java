/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.enums;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum Achievement {
	SPOOKY_NIGHTS("Noites de Arrepio", "Vença uma partida ranqueada em Outubro onde seu deck possua apenas criaturas malígnas (espírito, morto-vivo e demônio).", false),
	UNTOUCHABLE("O Intocável", "Vença uma partida ranqueada onde seu HP não fique abaixo do valor base.", false);

	private final String title;
	private final String description;
	private final boolean hidden;

	Achievement(String title, String description, boolean hidden) {
		this.title = title;
		this.description = description;
		this.hidden = hidden;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isValid(Shoukan game, Side side) {
		Hand h = game.getHands().get(side);

		return switch (this) {
			case SPOOKY_NIGHTS -> {
				Calendar c = Calendar.getInstance();
				if (c.get(Calendar.MONTH) != Calendar.OCTOBER) yield false;

				Set<Race> valid = EnumSet.of(Race.SPIRIT, Race.UNDEAD, Race.DEMON);
				Map<Race, Long> races = h.getRaceCount();
				for (Map.Entry<Race, Long> e : races.entrySet()) {
					if (valid.contains(e.getKey())) continue;

					if (e.getValue() > 0) yield false;
				}

				yield true;
			}
			case UNTOUCHABLE -> h.getHp() >= h.getBaseHp();
		};
	}
}
