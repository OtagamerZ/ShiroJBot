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
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.util.Utils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Flags implements Cloneable {
	private final Map<Drawable<?>, EnumSet<Flag>> permanent = new HashMap<>();
	private final Map<Drawable<?>, EnumSet<Flag>> flags = new HashMap<>();

	public void set(Drawable<?> source, Flag flag) {
		set(source, flag, false);
	}

	public void unset(Drawable<?> source, Flag flag) {
		unset(source, flag, false);
	}

	public void set(Drawable<?> source, Flag flag, boolean permanent) {
		Map<Drawable<?>, EnumSet<Flag>> target = permanent ? this.permanent : flags;
		target.computeIfAbsent(source, k -> EnumSet.noneOf(Flag.class)).add(flag);
	}

	public void unset(Drawable<?> source, Flag flag, boolean permanent) {
		Map<Drawable<?>, EnumSet<Flag>> target = permanent ? this.permanent : flags;
		target.computeIfAbsent(source, k -> EnumSet.noneOf(Flag.class)).remove(flag);
		target.entrySet().removeIf(e -> e.getValue().isEmpty());
	}

	public boolean has(Flag flag) {
		return Utils.equalsAny(flag, permanent) || Utils.equalsAny(flag, flags);
	}

	public boolean pop(Flag flag) {
		return Utils.equalsAny(flag, permanent) || flags.values().stream().anyMatch(e -> e.remove(flag));
	}

	public void clearTemp() {
		flags.remove(null);
	}

	public void clear(Drawable<?> source) {
		permanent.remove(source);
		flags.remove(source);
	}

	@Override
	protected Flags clone() {
		Flags clone = new Flags();
		for (Map.Entry<Drawable<?>, EnumSet<Flag>> e : permanent.entrySet()) {
			clone.permanent.put(e.getKey(), EnumSet.copyOf(e.getValue()));
		}

		return clone;
	}
}
