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
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public record EffectOverTime(
		Drawable<?> source,
		boolean debuff,
		Side side,
		BiConsumer<EffectOverTime, EffectParameters> effect,
		AtomicInteger turns,
		AtomicInteger limit,
		AtomicBoolean lock,
		EnumSet<Trigger> triggers
) implements Comparable<EffectOverTime> {
	public EffectOverTime(Senshi source, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, Trigger... triggers) {
		this(source, side != source.getSide(), side, effect, null, null, new AtomicBoolean(), EnumSet.of(Trigger.NONE, triggers));
	}

	public EffectOverTime(Drawable<?> source, boolean debuff, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, int turns, int limit, Trigger... triggers) {
		this(source, debuff, side, effect,
				turns < 0 ? null : new AtomicInteger(turns),
				limit < 0 ? null : new AtomicInteger(limit),
				new AtomicBoolean(),
				EnumSet.of(turns > -1 ? Trigger.ON_TURN_BEGIN : Trigger.NONE, triggers)
		);
	}

	public void decreaseTurn() {
		if (turns != null && turns.get() > 0) turns.getAndDecrement();
	}

	public void decreaseLimit() {
		if (limit != null && limit.get() > 0) limit.getAndDecrement();
	}

	public boolean expired() {
		if (source instanceof Senshi s && !limited()) {
			return s.getSlot().getIndex() == -1;
		}

		boolean expired = false;
		if (turns != null) expired = turns.get() <= 0;
		if (limit != null) expired |= limit.get() <= 0;

		return expired;
	}

	public boolean limited() {
		return turns != null || limit != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EffectOverTime that = (EffectOverTime) o;
		return Objects.equals(source, that.source) && side == that.side;
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, side);
	}

	@Override
	public int compareTo(@NotNull EffectOverTime other) {
		if (turns != null) return turns.get() - other.turns.get();
		if (limit != null) return limit.get() - other.limit.get();

		return Integer.MIN_VALUE;
	}
}
