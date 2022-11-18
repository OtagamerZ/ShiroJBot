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
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BondedLinkedList<T> extends LinkedList<T> {
	private final BiFunction<T, ListIterator<T>, Boolean> onAdd;
	private final Consumer<T> onRemove;

	public static <T> BondedLinkedList<T> withBind(BiFunction<T, ListIterator<T>, Boolean> onAdd) {
		return withBind(onAdd, t -> {
		});
	}

	public static <T> BondedLinkedList<T> withBind(Consumer<T> onRemove) {
		return withBind((t, i) -> true, onRemove);
	}

	public static <T> BondedLinkedList<T> withBind(BiFunction<T, ListIterator<T>, Boolean> onAdd, Consumer<T> onRemove) {
		return new BondedLinkedList<>(onAdd, onRemove);
	}

	public BondedLinkedList(BiFunction<T, ListIterator<T>, Boolean> onAdd) {
		this(onAdd, t -> {
		});
	}

	public BondedLinkedList(Consumer<T> onRemove) {
		this((t, i) -> true, onRemove);
	}

	public BondedLinkedList(BiFunction<T, ListIterator<T>, Boolean> onAdd, Consumer<T> onRemove) {
		this.onAdd = onAdd;
		this.onRemove = onRemove;
	}

	public BondedLinkedList(@Nonnull Collection<? extends T> c, BiFunction<T, ListIterator<T>, Boolean> onAdd) {
		this(c, onAdd, t -> {
		});
	}

	public BondedLinkedList(@Nonnull Collection<? extends T> c, BiFunction<T, ListIterator<T>, Boolean> onAdd, Consumer<T> onRemove) {
		this.onAdd = onAdd;
		this.onRemove = onRemove;
		addAll(c);
	}

	@Override
	public void addFirst(T t) {
		if (t != null && onAdd.apply(t, listIterator(Math.max(0, size() - 1)))) {
			super.addFirst(t);
		}
	}

	@Override
	public void addLast(T t) {
		if (t != null && onAdd.apply(t, listIterator(Math.max(0, size() - 1)))) {
			super.addLast(t);
		}
	}

	@Override
	public boolean add(T t) {
		if (t != null && onAdd.apply(t, listIterator(Math.max(0, size() - 1)))) {
			return super.add(t);
		}

		return false;
	}

	@Override
	public void add(int index, T t) {
		if (t != null && onAdd.apply(t, listIterator(Math.max(0, size() - 1)))) {
			super.add(index, t);
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		List<? extends T> filtered = c.stream()
				.filter(Objects::nonNull)
				.filter(t -> onAdd.apply(t, listIterator(Math.max(0, size() - 1))))
				.toList();

		return super.addAll(filtered);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		List<? extends T> filtered = c.stream()
				.filter(Objects::nonNull)
				.filter(t -> onAdd.apply(t, listIterator(Math.max(0, size() - 1))))
				.toList();

		return super.addAll(index, filtered);
	}

	@Override
	public T removeFirst() {
		onRemove.accept(getFirst());
		return super.removeFirst();
	}

	@Override
	public T removeLast() {
		onRemove.accept(getLast());
		return super.removeLast();
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

	public T removeOn(Predicate<T> cond) {
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