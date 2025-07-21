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
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BiConsumer;

public class EffectOverTime implements Closeable {
	private final @Nullable Drawable<?> source;
	private final Side side;
	private final BiConsumer<EffectOverTime, EffectParameters> effect;
	private final EnumSet<Trigger> triggers;
	private final Object equality;

	private Integer turns;
	private Integer limit;
	private boolean closed;

	public EffectOverTime(Drawable<?> source, BiConsumer<EffectOverTime, EffectParameters> effect, Trigger... triggers) {
		this(source, null, effect, triggers);
	}

	public EffectOverTime(Drawable<?> source, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, Trigger... triggers) {
		this(source, side, effect, -1, -1, triggers);
	}

	public EffectOverTime(Drawable<?> source, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, int turns, int limit, Trigger... triggers) {
		this(
				source, side, effect,
				turns < 0 ? null : turns,
				limit < 0 ? null : limit,
				EnumSet.of(turns > -1 ? Trigger.ON_TURN_BEGIN : Trigger.NONE, triggers)
		);
	}

	public EffectOverTime(@Nullable Drawable<?> source, Side side, BiConsumer<EffectOverTime, EffectParameters> effect, Integer turns, Integer limit, EnumSet<Trigger> triggers) {
		this.source = source;
		this.side = side;
		this.effect = effect;
		this.turns = turns;
		this.limit = limit;
		this.triggers = triggers;

		this.triggers.remove(Trigger.NONE);
		if (source != null) {
			equality = isPermanent() ? source.getId() : source.getSerial();
		} else {
			equality = side;
		}
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
		return side;
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
				return !e.isSpell() && e.getIndex() == -1;
			} else if (source instanceof Field f) {
				return !f.isActive();
			}
		}

		boolean expired = false;
		if (turns != null) expired = turns <= 0;
		if (limit != null) expired |= limit <= 0;

		return expired;
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
		return Objects.equals(equality, that.equality);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(equality);
	}

	@Override
	public void close() {
		closed = true;
	}
}
