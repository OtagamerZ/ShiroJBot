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
import java.util.List;

public class InfiniteList<T> extends ArrayList<T> implements Iterable<T> {
	private int index = -1;

	public InfiniteList() {
	}

	public InfiniteList(@NotNull Collection<? extends T> c) {
		super(c);
	}

	public int next() {
		return (index + 1) % size();
	}

	public int previous() {
		if (index == -1) index = 0;

		return Math.abs(index - 1) % size();
	}

	public T getNext() {
		if (isEmpty()) throw new IllegalStateException("This collection is empty");

		return get(index = next());
	}

	public T getPrevious() {
		if (isEmpty()) throw new IllegalStateException("This collection is empty");

		return get(index = previous());
	}

	public T peekNext() {
		if (isEmpty()) return null;

		return get(next());
	}

	public T peekPrevious() {
		if (isEmpty()) return null;

		return get(previous());
	}

	public T getCurrent() {
		if (index < 0 || index >= size()) {
			return getNext();
		}

		return get(index);
	}

	public T remove() {
		if (isEmpty()) return null;
		else if (index == -1) getNext();

		return remove(index);
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int idx) {
		index = idx;
	}

	@Override
	public void clear() {
		super.clear();
		index = -1;
	}

	public List<T> values() {
		return super.stream().toList();
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
