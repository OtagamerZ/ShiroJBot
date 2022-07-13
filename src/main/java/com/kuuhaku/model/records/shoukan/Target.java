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

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;

public record Target(Drawable<?> card, int index, Trigger trigger) {
	public Target() {
		this(null, -1, null);
	}

	public Target(Drawable<?> card, Trigger trigger) {
		this(card, card.getSlot().getIndex(), trigger);
	}

	public Target(Drawable<?> card, int index, Trigger trigger) {
		if (card instanceof Senshi s && s.getStats().popFlag(Flag.IGNORE_EFFECT)) {
			this.card = null;
			this.index = -1;
			this.trigger = null;
		} else {
			this.card = card;
			this.index = index;
			this.trigger = trigger;
		}
	}

	public void execute(EffectParameters ep) {
		if (card != null && card instanceof EffectHolder eh) {
			eh.execute(ep);
		}
	}
}
