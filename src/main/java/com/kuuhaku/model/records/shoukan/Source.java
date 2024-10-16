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
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;

import java.util.Objects;

public record Source(Drawable<?> card, Side side, int index, Trigger trigger) {
	public Source() {
		this(null, null, -1, Trigger.NONE);
	}

	public Source(Drawable<?> card, Trigger trigger) {
		this(card, card.getSide(), card.getIndex(), trigger);
	}

	public boolean execute(EffectParameters ep) {
		if (card != null && card instanceof EffectHolder<?> eh) {
			return eh.execute(ep);
		}

		return false;
	}

	public Target toTarget() {
		return card.asTarget(trigger);
	}

	public Target toTarget(TargetType type) {
		return card.asTarget(trigger, type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Source source = (Source) o;
		return index == source.index && Objects.equals(card, source.card) && side == source.side && trigger == source.trigger;
	}

	@Override
	public int hashCode() {
		return Objects.hash(card, side, index, trigger);
	}
}
