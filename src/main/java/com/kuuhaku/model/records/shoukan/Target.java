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

import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;

public record Target(Senshi card, Side side, int index, Trigger trigger, TargetType type) {
	public Target() {
		this(null, null, -1, null, TargetType.NONE);
	}

	public Target(Senshi card, Side side, int index, Trigger trigger, TargetType type) {
		Evogear shield = null;
		if (card != null) {
			card.getHand().getGame().trigger(Trigger.ON_EFFECT_TARGET, new Source(card, Trigger.ON_EFFECT_TARGET));
			if (!card.getHand().equals(card.getHand().getGame().getCurrent())) {
				for (Evogear e : card.getEquipments()) {
					if (e.hasCharm(Charm.SHIELD)) {
						shield = e;
					}
				}
			}
		}

		if (shield != null || (card != null && (card.getStats().popFlag(Flag.IGNORE_EFFECT) || card.isStasis()))) {
			if (shield != null) {
				int charges = shield.getStats().getData().getInt("shield", 0) + 1;
				if (charges >= Charm.SHIELD.getValue(shield.getTier())) {
					card.getHand().getGraveyard().add(shield);
				} else {
					shield.getStats().getData().put("shield", charges);
				}
			}

			this.card = null;
			this.side = null;
			this.index = -1;
			this.trigger = null;
			this.type = TargetType.NONE;
		} else {
			this.card = card;
			this.side = side;
			this.index = index;
			this.trigger = trigger;
			this.type = type;
		}
	}

	public boolean execute(EffectParameters ep) {
		if (card != null) {
			return card.execute(ep);
		}

		return false;
	}
}
