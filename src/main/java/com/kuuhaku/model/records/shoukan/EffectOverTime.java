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
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public record EffectOverTime(
		@Nullable Drawable<?> source,
		boolean debuff,
		Supplier<Side> sideSupplier,
		BiConsumer<EffectOverTime, EffectParameters> effect,
		AtomicInteger turns,
		AtomicInteger limit,
		AtomicBoolean lock,
		EnumSet<Trigger> triggers,
		AtomicBoolean closed
) implements Comparable<EffectOverTime>, Closeable {
	public EffectOverTime(Drawable<?> source, BiConsumer<EffectOverTime, EffectParameters> effect, Trigger... triggers) {
		this(source, false, source::getSide, effect,
				new AtomicInteger(),
				new AtomicInteger(),
				new AtomicBoolean(),
				EnumSet.of(Trigger.NONE, triggers),
				new AtomicBoolean()
		);
	}

	public EffectOverTime(Drawable<?> source, boolean debuff, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, int turns, int limit, Trigger... triggers) {
		this(source, debuff, () -> side, effect,
				turns < 0 ? null : new AtomicInteger(turns),
				limit < 0 ? null : new AtomicInteger(limit),
				new AtomicBoolean(),
				EnumSet.of(turns > -1 ? Trigger.ON_TURN_BEGIN : Trigger.NONE, triggers),
				new AtomicBoolean()
		);
	}

	public EffectOverTime(@Nullable Drawable<?> source, boolean debuff, Supplier<Side> sideSupplier, BiConsumer<EffectOverTime, EffectParameters> effect, AtomicInteger turns, AtomicInteger limit, AtomicBoolean lock, EnumSet<Trigger> triggers, AtomicBoolean closed) {
		this.source = source;
		this.debuff = debuff;
		this.sideSupplier = sideSupplier;
		this.effect = effect;
		this.turns = turns;
		this.limit = limit;
		this.lock = lock;
		this.triggers = triggers;
		this.closed = closed;

		this.triggers.remove(Trigger.NONE);
	}

	public void decreaseTurn() {
		if (turns != null && turns.get() > 0) turns.getAndDecrement();
	}

	public void decreaseLimit() {
		if (limit != null && limit.get() > 0) limit.getAndDecrement();
	}

	public Side side() {
		return sideSupplier.get();
	}

	public boolean expired() {
		if (permanent()) {
			if (source instanceof Senshi s) {
				return s.getIndex() == -1;
			} else if (source instanceof Field f) {
				return !f.isActive();
			}
		}

		boolean expired = false;
		if (turns != null) expired = turns.get() <= 0;
		if (limit != null) expired |= limit.get() <= 0;

		return expired;
	}

	public boolean permanent() {
		return turns == null && limit == null;
	}

	public boolean removed() {
		return closed.get();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EffectOverTime that = (EffectOverTime) o;

		Side side = side();
		Side oSide = that.side();
		if (source != null && that.source != null) {
			if (permanent()) {
				return source.getId().equals(that.source.getId()) && side == oSide;
			} else {
				return source.getSerial() == that.source.getSerial() && side == oSide;
			}
		} else {
			return side == oSide;
		}
	}

	@Override
	public int hashCode() {
		Side side = side();
		if (source != null) {
			return Objects.hash(permanent() ? source.getId() : source.getSerial(), side);
		} else {
			return Objects.hash(side);
		}
	}

	@Override
	public int compareTo(@NotNull EffectOverTime other) {
		if (turns != null && other.turns != null) return turns.get() - other.turns.get();
		if (limit != null && other.limit != null) return limit.get() - other.limit.get();

		return (turns != null || limit != null) ? Integer.MAX_VALUE : -255;
	}

	@Override
	public void close() {
		closed.set(true);
	}
}
