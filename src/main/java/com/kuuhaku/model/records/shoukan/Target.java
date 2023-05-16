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

import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public record Target(Senshi card, Side side, int index, Trigger trigger, TargetType type, AtomicBoolean skip) {
	public Target() {
		this(null, null, -1, Trigger.NONE, TargetType.NONE);
	}

	public Target(Senshi card, Side side, int index, Trigger trigger, TargetType type) {
		this(card, side, index, trigger, type, new AtomicBoolean());
	}

	public boolean execute(EffectParameters ep) {
		if (card != null) {
			return card.execute(ep);
		}

		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Target target = (Target) o;
		return index == target.index && Objects.equals(card, target.card) && side == target.side && trigger == target.trigger && type == target.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(card, side, index, trigger, type);
	}
}
