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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.persistent.shoukan.Senshi;

public record Targeting(Senshi ally, Senshi enemy) {
	public Targeting(Hand hand, int ally, int enemy) {
		this(
				ally == -1 ? null : hand.getGame()
						.getSlots(hand.getSide())
						.get(ally)
						.getTop(),
				enemy == -1 ? null : hand.getGame()
						.getSlots(hand.getSide().getOther())
						.get(enemy)
						.getTop()
		);
	}

	public boolean validate(TargetType type) {
		return switch (type) {
			case NONE -> true;
			case ALLY -> ally != null;
			case ENEMY -> enemy != null;
			case BOTH -> ally != null && enemy != null;
		};
	}
}
