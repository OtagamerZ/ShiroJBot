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

import java.io.Closeable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Mutex<T> implements Closeable {
	private static final Set<Object> locks = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final T ref;

	public Mutex(T ref) {
		this.ref = ref;
		locks.add(ref);
	}

	public static synchronized boolean isLocked(Object ref) {
		return locks.contains(ref);
	}

	@Override
	public void close() {
		locks.remove(ref);
	}
}
