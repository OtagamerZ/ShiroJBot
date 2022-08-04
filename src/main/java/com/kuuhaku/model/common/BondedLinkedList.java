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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BondedLinkedList<T> extends LinkedList<T> {
	private final Consumer<T> bonding;
	private final Predicate<T> check;

	public static <T> BondedLinkedList<T> withBind(Consumer<T> bonding) {
		return new BondedLinkedList<T>(t -> true, bonding);
	}

	public static <T> BondedLinkedList<T> withCheck(Predicate<T> check) {
		return new BondedLinkedList<T>(check, t -> {});
	}

	public BondedLinkedList(Predicate<T> check, Consumer<T> bonding) {
		this.bonding = bonding;
		this.check = check;
	}

	public BondedLinkedList(@Nonnull Collection<? extends T> c, Predicate<T> check, Consumer<T> bonding) {
		this.bonding = bonding;
		this.check = check;
		addAll(c);
	}

	@Override
	public void addFirst(T t) {
		if (!check.test(t)) return;

		try {
			super.addFirst(t);
		} finally {
			bonding.accept(t);
		}
	}

	@Override
	public void addLast(T t) {
		if (!check.test(t)) return;

		try {
			super.addLast(t);
		} finally {
			bonding.accept(t);
		}
	}

	@Override
	public boolean add(T t) {
		if (!check.test(t)) return false;

		try {
			return super.add(t);
		} finally {
			bonding.accept(t);
		}
	}

	@Override
	public void add(int index, T element) {
		if (!check.test(element)) return;

		try {
			super.add(index, element);
		} finally {
			bonding.accept(element);
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		try {
			return super.addAll(c.stream().filter(check).toList());
		} finally {
			for (T t : c) {
				bonding.accept(t);
			}
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		try {
			return super.addAll(index, c.stream().filter(check).toList());
		} finally {
			for (T t : c) {
				bonding.accept(t);
			}
		}
	}
}