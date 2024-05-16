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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class EffectOverTime implements Comparable<EffectOverTime>, Closeable {
	private final @Nullable Drawable<?> source;
	private final Supplier<Side> sideSupplier;
	private final BiConsumer<EffectOverTime, EffectParameters> effect;
	private final EnumSet<Trigger> triggers;
	private Integer turns;
	private Integer limit;
	private boolean lock;
	private boolean closed;

	public EffectOverTime(Drawable<?> source, BiConsumer<EffectOverTime, EffectParameters> effect, Trigger... triggers) {
		this(source, null, effect, triggers);
	}

	public EffectOverTime(Drawable<?> source, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, Trigger... triggers) {
		this(source, side, effect, -1, -1, triggers);
	}

	public EffectOverTime(Drawable<?> source, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, int turns, int limit, Trigger... triggers) {
		this(
				source, () -> side, effect,
				turns < 0 ? null : turns,
				limit < 0 ? null : limit,
				EnumSet.of(turns > -1 ? Trigger.ON_TURN_BEGIN : Trigger.NONE, triggers)
		);
	}

	public EffectOverTime(@Nullable Drawable<?> source, Supplier<Side> sideSupplier, BiConsumer<EffectOverTime, EffectParameters> effect, Integer turns, Integer limit, EnumSet<Trigger> triggers) {
		this.source = source;
		this.sideSupplier = sideSupplier;
		this.effect = effect;
		this.turns = turns;
		this.limit = limit;
		this.triggers = triggers;

		this.triggers.remove(Trigger.NONE);
	}

	public void decreaseTurn() {
		if (turns != null && turns > 0) turns--;
	}

	public void decreaseLimit() {
		if (limit != null && limit > 0) limit--;
	}

	public @Nullable Drawable<?> getSource() {
		return source;
	}

	public Side getSide() {
		return sideSupplier.get();
	}

	public BiConsumer<EffectOverTime, EffectParameters> getEffect() {
		return effect;
	}

	public EnumSet<Trigger> getTriggers() {
		return triggers;
	}

	public boolean hasTrigger(Trigger trigger) {
		return triggers.contains(trigger);
	}

	public Integer getTurns() {
		return turns;
	}

	public Integer getLimit() {
		return limit;
	}

	public boolean isExpired() {
		if (isPermanent()) {
			if (source instanceof Senshi s) {
				return s.getIndex() == -1;
			} else if (source instanceof Evogear e) {
				if (e.isSpell()) return false;

				return e.getEquipper() == null || e.getEquipper().getIndex() == -1;
			} else if (source instanceof Field f) {
				return !f.isActive();
			}
		}

		boolean expired = false;
		if (turns != null) expired = turns <= 0;
		if (limit != null) expired |= limit <= 0;

		return expired;
	}

	public boolean isLocked() {
		return lock;
	}

	public void lock() {
		lock = true;
	}

	public void unlock() {
		lock = false;
	}

	public boolean isPermanent() {
		return turns == null && limit == null;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EffectOverTime that = (EffectOverTime) o;

		Side side = getSide();
		Side oSide = that.getSide();
		if (source != null && that.source != null) {
			if (isPermanent()) {
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
		Side side = getSide();
		if (source != null) {
			return Objects.hash(isPermanent() ? source.getId() : source.getSerial(), side);
		} else {
			return Objects.hash(side);
		}
	}

	@Override
	public int compareTo(@NotNull EffectOverTime other) {
		if (turns != null && other.turns != null) return turns - other.turns;
		if (limit != null && other.limit != null) return limit - other.limit;

		return (turns != null || limit != null) ? Integer.MAX_VALUE : -255;
	}

	@Override
	public void close() {
		closed = true;
	}
}
