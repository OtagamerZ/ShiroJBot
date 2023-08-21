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

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.shoukan.ContingencyTrigger;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;

public record Contingency(Evogear card, ContingencyTrigger trigger) {
	public boolean check() {
		Hand h = card.getHand();

		return switch (trigger) {
			case HP_LOW -> h.getHPPrcnt() <= 0.5;
			case HP_CRITICAL -> h.getHPPrcnt() <= 0.25;
			case ON_T4 -> {
				Evogear e = h.getOther().getData().get(Evogear.class, "last_spell");
				yield e != null && e.getTier() >= 4;
			}
			case ON_FUSION -> {
				Senshi s = h.getOther().getData().get(Senshi.class, "last_summon");
				yield s != null && s.getBase().getTags().contains("FUSION");
			}
			case ON_HIGH_MANA -> h.getOther().getMP() >= 10;
			case DEFEATED -> h.getHP() == 0;
		};
	}
}
