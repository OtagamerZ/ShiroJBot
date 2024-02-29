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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Function;

public class SupplyChain<T> implements Iterable<Function<T, T>> {
	private final Deque<Function<T, T>> chain = new ArrayDeque<>();
	private final T initialValue;

	public SupplyChain(T initialValue) {
		this.initialValue = initialValue;
	}

	public SupplyChain<T> add(Function<T, T> step) {
		chain.add(step);
		return this;
	}

	public T get() {
		T out = initialValue;
		for (Function<T, T> step : chain) {
			out = step.apply(out);
		}

		return out;
	}

	public T process(T value) {
		for (Function<T, T> step : chain) {
			value = step.apply(value);
		}

		return value;
	}

	@NotNull
	@Override
	public Iterator<Function<T, T>> iterator() {
		return chain.iterator();
	}
}
