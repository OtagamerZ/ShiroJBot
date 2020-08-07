/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SelfRunningList<T> extends LinkedList<T> {
	private final Consumer<T> action;
	private final ExecutorService exec = Executors.newSingleThreadExecutor();

	public SelfRunningList(Consumer<T> action) {
		this.action = action;
	}

	public SelfRunningList(@NotNull Collection<? extends T> c, Consumer<T> action) {
		super(c);
		this.action = action;
	}

	@Override
	public boolean add(T t) {
		exec.execute(() -> action.accept(t));
		return true;
	}

	public Consumer<T> getAction() {
		return action;
	}
}
