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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BondedLinkedList<T> extends LinkedList<T> {
	private final Predicate<T> check;
	private final Consumer<T> onAdd;
	private final Consumer<T> onRemove;

	public static <T> BondedLinkedList<T> withBind(Consumer<T> onAdd) {
		return withBind(onAdd, t -> {});
	}

	public static <T> BondedLinkedList<T> withBind(Consumer<T> onAdd, Consumer<T> onRemove) {
		return new BondedLinkedList<T>(t -> true, onAdd, onRemove);
	}

	public static <T> BondedLinkedList<T> withCheck(Predicate<T> check) {
		return new BondedLinkedList<T>(check, t -> {}, t -> {});
	}

	public BondedLinkedList(Predicate<T> check, Consumer<T> onAdd) {
		this(check, onAdd, t -> {});
	}

	public BondedLinkedList(Predicate<T> check, Consumer<T> onAdd, Consumer<T> onRemove) {
		this.check = check;
		this.onAdd = onAdd;
		this.onRemove = onRemove;
	}

	public BondedLinkedList(@Nonnull Collection<? extends T> c, Predicate<T> check, Consumer<T> onAdd) {
		this(c, check, onAdd, t -> {});
	}

	public BondedLinkedList(@Nonnull Collection<? extends T> c, Predicate<T> check, Consumer<T> onAdd, Consumer<T> onRemove) {
		this.check = check;
		this.onAdd = onAdd;
		this.onRemove = onRemove;
		addAll(c);
	}

	@Override
	public void addFirst(T t) {
		try {
			if (!check.test(t)) return;

			super.addFirst(t);
		} finally {
			if (t != null) {
				onAdd.accept(t);
			}
		}
	}

	@Override
	public void addLast(T t) {
		try {
			if (!check.test(t)) return;

			super.addLast(t);
		} finally {
			if (t != null) {
				onAdd.accept(t);
			}
		}
	}

	@Override
	public boolean add(T t) {
		try {
			if (!check.test(t)) return false;

			return super.add(t);
		} finally {
			if (t != null) {
				onAdd.accept(t);
			}
		}
	}

	@Override
	public void add(int index, T t) {
		try {
			if (!check.test(t)) return;

			super.add(index, t);
		} finally {
			if (t != null) {
				onAdd.accept(t);
			}
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		try {
			List<? extends T> filtered = c.stream().filter(check).toList();

			return super.addAll(filtered);
		} finally {
			for (T t : c) {
				if (t != null) {
					onAdd.accept(t);
				}
			}
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		try {
			List<? extends T> filtered = c.stream().filter(check).toList();

			return super.addAll(index, filtered);
		} finally {
			for (T t : c) {
				if (t != null) {
					onAdd.accept(t);
				}
			}
		}
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

	@Override
	public void clear() {
		for (T t : this) {
			onRemove.accept(t);
		}

		super.clear();
	}
}