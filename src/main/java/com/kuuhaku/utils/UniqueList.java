/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public class UniqueList<T> extends ArrayList<T> {
	private final Function<T, ?> checker;

	public UniqueList(Function<T, ?> checker) {
		this.checker = checker;
	}

	public UniqueList(@Nonnull Collection<? extends T> c, Function<T, ?> checker) {
		addAll(c);
		this.checker = checker;
	}

	public Function<T, ?> getChecker() {
		return checker;
	}

	private boolean check(T a, T b) {
		return Objects.equals(checker.apply(a), checker.apply(b));
	}

	@Override
	public boolean add(T t) {
		if (stream().anyMatch(v -> check(v, t))) return false;

		return super.add(t);
	}

	@Override
	public void add(int index, T element) {
		if (stream().anyMatch(v -> check(v, element))) return;

		super.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) {
			add(t);
		}

		return super.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		for (T t : c) {
			add(index, t);
		}

		return super.addAll(index, c);
	}
}
