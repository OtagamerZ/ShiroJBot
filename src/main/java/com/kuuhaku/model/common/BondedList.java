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
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BondedList<T> extends ArrayList<T> {
	private final Consumer<T> bonding;
	private final Predicate<T> check;

	public BondedList(Consumer<T> bonding) {
		this(bonding, t -> true);
	}

	public BondedList(@Nonnull Collection<? extends T> c, Consumer<T> bonding) {
		this(c, bonding, t -> true);
	}

	public BondedList(Consumer<T> bonding, Predicate<T> check) {
		this.bonding = bonding;
		this.check = check;
	}

	public BondedList(@Nonnull Collection<? extends T> c, Consumer<T> bonding, Predicate<T> check) {
		this.bonding = bonding;
		this.check = check;
		addAll(c);
	}

	public Consumer<T> getBonding() {
		return bonding;
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
		for (T t : c) {
			if (!check.test(t)) return false;
		}

		try {
			return super.addAll(c);
		} finally {
			for (T t : c) {
				bonding.accept(t);
			}
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		for (T t : c) {
			if (!check.test(t)) return false;
		}

		try {
			return super.addAll(index, c);
		} finally {
			for (T t : c) {
				bonding.accept(t);
			}
		}
	}
}