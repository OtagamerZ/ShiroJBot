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
import java.util.Iterator;
import java.util.ListIterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BondedList<T> extends ArrayList<T> {
	private final BiFunction<T, ListIterator<T>, Boolean> onAdd;
	private final Consumer<T> onRemove;

	public static <T> BondedList<T> withBind(BiFunction<T, ListIterator<T>, Boolean> onAdd) {
		return withBind(onAdd, t -> {
		});
	}

	public static <T> BondedList<T> withBind(Consumer<T> onRemove) {
		return withBind((t, i) -> true, onRemove);
	}

	public static <T> BondedList<T> withBind(BiFunction<T, ListIterator<T>, Boolean> onAdd, Consumer<T> onRemove) {
		return new BondedList<>(onAdd, onRemove);
	}

	public BondedList() {
		this((a, b) -> true, t -> {
		});
	}

	public BondedList(BiFunction<T, ListIterator<T>, Boolean> onAdd) {
		this(onAdd, t -> {
		});
	}

	public BondedList(Consumer<T> onRemove) {
		this((t, i) -> true, onRemove);
	}

	public BondedList(BiFunction<T, ListIterator<T>, Boolean> onAdd, Consumer<T> onRemove) {
		this.onAdd = onAdd;
		this.onRemove = onRemove;
	}

	public BondedList(@Nonnull Collection<? extends T> c, BiFunction<T, ListIterator<T>, Boolean> onAdd) {
		this(c, onAdd, t -> {
		});
	}

	public BondedList(@Nonnull Collection<? extends T> c, BiFunction<T, ListIterator<T>, Boolean> onAdd, Consumer<T> onRemove) {
		this.onAdd = onAdd;
		this.onRemove = onRemove;
		addAll(c);
	}

	public T getFirst() {
		return get(0);
	}

	public T getLast() {
		return get(size() - 1);
	}

	@Override
	public void add(int index, T t) {
		if (t != null && onAdd.apply(t, listIterator(Math.max(0, size() - 1)))) {
			super.add(index, t);
		}
	}

	public void addFirst(T t) {
		add(0, t);
	}

	public void addlast(T t) {
		add(size(), t);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return addAll(size(), c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		int before = size();
		for (T t : c) {
			add(index++, t);
		}

		return before != size();
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

	public T removeFirst() {
		return remove(0);
	}

	public T removeLast() {
		return remove(size() - 1);
	}

	public T removeFirst(Predicate<T> cond) {
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			T t = it.next();
			if (cond.test(t)) {
				onRemove.accept(t);
				it.remove();

				return t;
			}
		}

		return null;
	}

	@Override
	public void clear() {
		for (T t : this) {
			onRemove.accept(t);
		}

		super.clear();
	}
}