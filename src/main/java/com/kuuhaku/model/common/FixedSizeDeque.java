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

package com.kuuhaku.model.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collection;

public class FixedSizeDeque<T> extends ArrayDeque<T> {
	private final int size;

	public FixedSizeDeque(int size) {
		this.size = size;
	}

	public FixedSizeDeque(@NotNull Collection<? extends T> c, int size) {
		this.size = size;
		addAll(c);
	}

	@Override
	public void addFirst(@NotNull T t) {
		if (size() >= size) removeLast();

		super.addFirst(t);
	}

	@Override
	public void addLast(@NotNull T t) {
		if (size() >= size) removeFirst();

		super.addLast(t);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) {
			addLast(t);
		}

		return c.size() > 0;
	}

	@Override
	public boolean add(@NotNull T t) {
		addLast(t);
		return true;
	}
}
