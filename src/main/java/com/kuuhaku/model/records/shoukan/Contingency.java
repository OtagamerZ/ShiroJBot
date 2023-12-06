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

import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.shoukan.ContingencyTrigger;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;

public record Contingency(Evogear card, ContingencyTrigger trigger) {
	public boolean check() {
		Hand h = card.getHand();

		return switch (trigger) {
			case HP_LOW -> h.getHPPrcnt() <= 0.5;
			case HP_CRITICAL -> h.getHPPrcnt() <= 0.25;
			case DEFEATED -> h.getHP() == 0;
			case ON_TIER_1, ON_TIER_2, ON_TIER_3, ON_TIER_4 -> {
				Evogear e = h.getOther().getData().get(Evogear.class, "last_spell");
				yield e != null && e.getTier() >= switch (trigger) {
					case ON_TIER_1 -> 1;
					case ON_TIER_2 -> 2;
					case ON_TIER_3 -> 3;
					case ON_TIER_4 -> 4;
					default -> Integer.MAX_VALUE;
				};
			}
			case ON_EFFECT -> h.getLockTime(Lock.EFFECT) > 0;
			case ON_SPELL -> h.getLockTime(Lock.SPELL) > 0;
			case ON_ABILITY -> h.getLockTime(Lock.ABILITY) > 0;
			case ON_TAUNT -> h.getLockTime(Lock.TAUNT) > 0;
			case ON_DECK -> h.getLockTime(Lock.DECK) > 0;
			case ON_BLIND -> h.getLockTime(Lock.BLIND) > 0;
			case ON_CHARM -> h.getLockTime(Lock.CHARM) > 0;
			case ON_HIGH_MANA -> h.getOther().getMP() >= 10;
		};
	}
}
