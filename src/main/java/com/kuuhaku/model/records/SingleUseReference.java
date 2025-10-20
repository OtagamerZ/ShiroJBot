/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public record SingleUseReference<T>(AtomicReference<T> ref) {
	public SingleUseReference(T ref) {
		this(new AtomicReference<>(ref));
	}

	public synchronized <R> R peekProperty(Function<T, R> retriever) {
		if (ref.get() == null) return null;

		return retriever.apply(ref.get());
	}

	public synchronized boolean isValid() {
		return ref.get() != null;
	}

	public synchronized T get() {
		try {
			return ref.get();
		} finally {
			ref.set(null);
		}
	}
}
