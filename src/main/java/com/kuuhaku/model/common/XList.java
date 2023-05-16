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

package com.kuuhaku.model.common;

import com.kuuhaku.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

public class XList<T> extends ArrayList<T> {
	private final RandomGenerator rng;

	public XList(int initialCapacity, RandomGenerator rng) {
		super(initialCapacity);
		this.rng = rng;
	}

	public XList(RandomGenerator rng) {
		this.rng = rng;
	}

	public XList(@NotNull Collection<? extends T> c, RandomGenerator rng) {
		super(c);
		this.rng = rng;
	}

	public T getRandom() {
		return Utils.getRandomEntry(this);
	}

	public List<T> getRandom(int n) {
		return Utils.getRandomN(this, n);
	}
}
