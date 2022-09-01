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
import java.util.LinkedList;

public class InfiniteList<T> extends ArrayDeque<T> {
	public InfiniteList() {
	}

	public InfiniteList(@NotNull Collection<? extends T> c) {
		super(c);
	}

	public T getNext() {
		if (isEmpty()) throw new IllegalStateException("This collection is empty");

		addLast(pollFirst());
		return getFirst();
	}

	public T getPrevious() {
		if (isEmpty()) throw new IllegalStateException("This collection is empty");

		addFirst(pollLast());
		return getLast();
	}

	public T peekNext() {
		LinkedList<T> aux = new LinkedList<>(this);

		aux.addLast(aux.pollFirst());
		return aux.getFirst();
	}

	public T peekPrevious() {
		LinkedList<T> aux = new LinkedList<>(this);

		aux.addFirst(aux.pollLast());
		return aux.getLast();
	}

	public T get() {
		return super.getFirst();
	}
}