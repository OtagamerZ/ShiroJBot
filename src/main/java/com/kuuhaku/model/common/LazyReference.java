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

package com.kuuhaku.model.common;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LazyReference<T> {
	private final Timer timer = new Timer(true);
	private final Supplier<T> loader;
	private final long ttl;
	private TimerTask cleanTask;
	private boolean loaded = false;
	private T ref = null;

	public LazyReference(Supplier<T> loader) {
		this(loader, -1, null);
	}

	public LazyReference(Supplier<T> loader, int time, TimeUnit unit) {
		this.loader = loader;
		if (time == -1 && unit == null) {
			this.ttl = -1;
		} else {
			this.ttl = TimeUnit.MILLISECONDS.convert(time, unit);
		}
	}

	public T load() {
		try {
			if (loaded) return ref;

			ref = loader.get();
			loaded = true;
			return ref;
		} finally {
			if (ttl > -1) {
				if (cleanTask != null) {
					cleanTask.cancel();
				}

				timer.schedule(cleanTask = new TimerTask() {
					@Override
					public void run() {
						ref = null;
						loaded = false;
					}
				}, ttl);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LazyReference<?> that = (LazyReference<?>) o;
		return Objects.equals(ref, that.ref);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ref);
	}

	@Override
	public String toString() {
		return Objects.toString(ref);
	}
}
