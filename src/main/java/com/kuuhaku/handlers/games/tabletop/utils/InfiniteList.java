/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.utils;

import java.util.LinkedList;

public class InfiniteList<T> extends LinkedList<T> {
	public T getNext() {
		addLast(pollFirst());
		return getFirst();
	}

	public T getPrevious() {
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

	public T getCurrent() {
		return super.getFirst();
	}
}
