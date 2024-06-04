/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ThreadBound<T> {
	private final ScheduledExecutorService checker = Executors.newSingleThreadScheduledExecutor();
	private final Map<Thread, T> threadBound = new ConcurrentHashMap<>();
	private final ThreadLocal<T> threadLocal = new ThreadLocal<>();
	private final Consumer<T> closer;
	private final Supplier<T> supplier;

	public ThreadBound(Consumer<T> closer) {
		this(() -> null, closer);
	}

	public ThreadBound(Supplier<T> supplier, Consumer<T> closer) {
		this.supplier = supplier;
		this.closer = closer;

		checker.scheduleAtFixedRate(() -> {
			Iterator<Map.Entry<Thread, T>> it = threadBound.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Thread, T> entry = it.next();
				if (entry.getValue() == null) {
					it.remove();
					continue;
				}

				if (!entry.getKey().isAlive()) {
					this.closer.accept(entry.getValue());
					it.remove();
				}
			}
		}, 30, 30, TimeUnit.SECONDS);
	}

	public T get() {
		T out = threadLocal.get();
		if (out == null) {
			threadBound.compute(Thread.currentThread(), (k, v) -> {
				if (v != null) {
					closer.accept(v);
				}

				T val = supplier.get();
				if (val != null) {
					threadLocal.set(val);
					return val;
				} else {
					return null;
				}
			});
		}

		return out;
	}
}
