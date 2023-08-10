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
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Flags implements Cloneable, Iterable<Flag> {
	private final EnumSet<Flag> permanent = EnumSet.noneOf(Flag.class);
	private final Map<Drawable<?>, EnumSet<Flag>> flags = new HashMap<>();

	public void fixed(Flag flag) {
		permanent.add(flag);
	}

	public void set(Drawable<?> source, Flag flag) {
		flags.computeIfAbsent(source, k -> EnumSet.noneOf(Flag.class)).add(flag);
	}

	public void unset(Drawable<?> source, Flag flag) {
		flags.computeIfAbsent(source, k -> EnumSet.noneOf(Flag.class)).remove(flag);
		flags.entrySet().removeIf(e -> e.getValue().isEmpty());
	}

	public void clearTemp() {
		flags.remove(null);
	}

	public boolean has(Flag flag) {
		return permanent.contains(flag) || flags.values().stream().anyMatch(e -> e.contains(flag));
	}

	public boolean pop(Flag flag) {
		return permanent.contains(flag) || flags.values().stream().anyMatch(e -> e.remove(flag));
	}

	@NotNull
	@Override
	public Iterator<Flag> iterator() {
		EnumSet<Flag> flags = EnumSet.copyOf(permanent);
		flags.addAll(this.flags.values().stream().flatMap(Collection::stream).toList());

		return flags.iterator();
	}

	@Override
	protected Flags clone() {
		Flags clone = new Flags();
		clone.permanent.addAll(permanent);

		return clone;
	}
}
