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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class InfiniteList<T> extends ArrayList<T> implements Iterable<T> {
	private int iter = -1;

	public InfiniteList() {
	}

	public InfiniteList(@NotNull Collection<? extends T> c) {
		super(c);
	}

	private int next() {
		return (iter + 1) % size();
	}

	private int previous() {
		if (iter == -1) iter = 0;

		return Math.abs(iter - 1) % size();
	}

	public T getNext() {
		if (isEmpty()) throw new IllegalStateException("This collection is empty");

		return get(iter = next());
	}

	public T getPrevious() {
		if (isEmpty()) throw new IllegalStateException("This collection is empty");

		return get(iter = previous());
	}

	public T peekNext() {
		if (isEmpty()) return null;

		return get(next());
	}

	public T peekPrevious() {
		if (isEmpty()) return null;

		return get(previous());
	}

	public T get() {
		if (isEmpty()) return null;
		else if (iter == -1 || iter >= size()) iter = 0;

		return get(iter);
	}

	public T remove() {
		if (isEmpty()) return null;

		return remove(iter);
	}

	@Override
	public void clear() {
		super.clear();
		iter = -1;
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return !isEmpty();
			}

			@Override
			public T next() {
				return getNext();
			}
		};
	}
}
