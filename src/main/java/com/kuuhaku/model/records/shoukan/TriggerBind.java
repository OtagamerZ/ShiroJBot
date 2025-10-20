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

import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.enums.shoukan.Trigger;
import org.apache.commons.collections4.SetUtils;

import java.util.*;

public record TriggerBind(EffectHolder<?> holder, EnumMap<Target, EnumSet<Trigger>> binds, boolean permanent) {
	public enum Target {
		SAME, OTHER, BOTH
	}

	public TriggerBind(EffectHolder<?> holder, EnumMap<Target, EnumSet<Trigger>> binds) {
		this(holder, binds, false);
	}

	public TriggerBind(EffectHolder<?> holder, EnumSet<Trigger> binds) {
		this(holder, binds, false);
	}

	public TriggerBind(EffectHolder<?> holder, EnumSet<Trigger> binds, boolean permanent) {
		this(holder, new EnumMap<>(Map.of(Target.BOTH, binds)), permanent);
	}

	public TriggerBind(EffectHolder<?> holder, EnumMap<Target, EnumSet<Trigger>> binds, boolean permanent) {
		this.holder = holder;
		this.binds = binds;
		this.permanent = permanent;

		for (Target t : Target.values()) {
			this.binds.computeIfAbsent(t, k -> EnumSet.noneOf(Trigger.class));
		}
	}


	public boolean isBound(EffectParameters ep) {
		Target tgt = ep.side() == holder.getSide() ? Target.SAME : Target.OTHER;
		Set<Trigger> trigs = SetUtils.union(binds.get(tgt), binds.get(Target.BOTH));

		return trigs.contains(ep.trigger());
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TriggerBind that = (TriggerBind) o;
		return Objects.equals(holder, that.holder);
	}

	@Override
	public int hashCode() {
		return holder.hashCode();
	}
}
