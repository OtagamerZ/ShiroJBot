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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BondedList<T> extends ArrayList<T> {
	private final Consumer<T> bonding;
	private final Predicate<T> check;

	public static <T> BondedList<T> withBind(Consumer<T> bonding) {
		return new BondedList<T>(t -> true, bonding);
	}

	public static <T> BondedList<T> withCheck(Predicate<T> check) {
		return new BondedList<T>(check, t -> {});
	}

	public BondedList(Predicate<T> check, Consumer<T> bonding) {
		this.bonding = bonding;
		this.check = check;
	}

	public BondedList(@Nonnull Collection<? extends T> c, Predicate<T> check, Consumer<T> bonding) {
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
		List<? extends T> filtered = c.stream().filter(check).toList();

		try {
			return super.addAll(filtered);
		} finally {
			for (T t : filtered) {
				bonding.accept(t);
			}
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		List<? extends T> filtered = c.stream().filter(check).toList();

		try {
			return super.addAll(index, filtered);
		} finally {
			for (T t : filtered) {
				bonding.accept(t);
			}
		}
	}
}