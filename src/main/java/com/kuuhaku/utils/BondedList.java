/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;

public class BondedList<T> extends LinkedList<T> {
	private final Consumer<T> bonding;

	public BondedList(Consumer<T> bonding) {
		this.bonding = bonding;
	}

	public BondedList(@NotNull Collection<? extends T> c, Consumer<T> bonding) {
		super(c);
		this.bonding = bonding;
	}

	@Override
	public void addFirst(T t) {
		bonding.accept(t);
		super.addFirst(t);
	}

	@Override
	public void addLast(T t) {
		bonding.accept(t);
		super.addLast(t);
	}

	@Override
	public boolean add(T t) {
		bonding.accept(t);
		return super.add(t);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) {
			bonding.accept(t);
		}

		return super.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		for (T t : c) {
			bonding.accept(t);
		}

		return super.addAll(index, c);
	}

	@Override
	public void add(int index, T element) {
		bonding.accept(element);
		super.add(index, element);
	}
}
