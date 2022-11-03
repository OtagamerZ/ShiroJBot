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
import java.util.function.Function;
import java.util.function.Predicate;

public class BondedList<T> extends ArrayList<T> {
	private final Function<T, Boolean> onAdd;
	private final Consumer<T> onRemove;

	public static <T> BondedList<T> withBind(Function<T, Boolean> onAdd) {
		return withBind(onAdd, t -> {});
	}

	public static <T> BondedList<T> withBind(Consumer<T> onRemove) {
		return withBind(t -> true, onRemove);
	}

	public static <T> BondedList<T> withBind(Function<T, Boolean> onAdd, Consumer<T> onRemove) {
		return new BondedList<T>(onAdd, onRemove);
	}

	public BondedList(Function<T, Boolean> onAdd) {
		this(onAdd, t -> {});
	}

	public BondedList(Consumer<T> onRemove) {
		this(t -> true, onRemove);
	}

	public BondedList(Function<T, Boolean> onAdd, Consumer<T> onRemove) {
		this.onAdd = onAdd;
		this.onRemove = onRemove;
	}

	public BondedList(@Nonnull Collection<? extends T> c, Function<T, Boolean> onAdd) {
		this(c, onAdd, t -> {});
	}

	public BondedList(@Nonnull Collection<? extends T> c, Function<T, Boolean> onAdd, Consumer<T> onRemove) {
		this.onAdd = onAdd;
		this.onRemove = onRemove;
		addAll(c);
	}

	@Override
	public boolean add(T t) {
		if (t != null && onAdd.apply(t)) {
			return super.add(t);
		}

		return false;
	}

	@Override
	public void add(int index, T t) {
		if (t != null && onAdd.apply(t)) {
			super.add(index, t);
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		List<? extends T> filtered = c.stream().filter(onAdd::apply).toList();

		return super.addAll(filtered);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		List<? extends T> filtered = c.stream().filter(onAdd::apply).toList();

		return super.addAll(index, filtered);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		if (super.contains(o)) {
			onRemove.accept((T) o);
		}

		return super.remove(o);
	}

	@Override
	public T remove(int index) {
		onRemove.accept(get(index));
		return super.remove(index);
	}

	@Override
	public void clear() {
		for (T t : this) {
			onRemove.accept(t);
		}

		super.clear();
	}
}