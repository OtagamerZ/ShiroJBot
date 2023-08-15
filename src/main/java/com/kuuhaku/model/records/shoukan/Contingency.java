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
import com.kuuhaku.model.enums.shoukan.Phase;
import com.kuuhaku.model.persistent.shoukan.Evogear;

public record Contingency(Evogear card, ContingencyTrigger trigger) {
	public boolean check() {
		Hand h = card.getHand();
		Shoukan game = h.getGame();

		return switch (trigger) {
			case HP_LOW -> h.isLowLife();
			case HP_CRITICAL -> h.isCritical();
			case PLAN_PHASE -> game.getCurrentSide() != h.getSide() && game.getPhase() == Phase.PLAN;
			case COMBAT_PHASE -> game.getCurrentSide() != h.getSide() && game.getPhase() == Phase.COMBAT;
			case OUT_OF_MANA -> h.getMP() == 0;
			case DEFEATED -> h.getHP() == 0;
		};
	}
}
