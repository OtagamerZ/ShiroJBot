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

package com.kuuhaku.game.engine;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DelayedAction {
	private final ScheduledExecutorService exec;
	private int time;
	private TimeUnit unit;
	private ScheduledFuture<?> action;
	private Runnable cachedTask;

	private DelayedAction(ScheduledExecutorService exec) {
		this.exec = exec;
	}

	public static DelayedAction of(ScheduledExecutorService exec) {
		return new DelayedAction(exec);
	}

	public DelayedAction setTimeUnit(int time, TimeUnit unit) {
		this.time = time;
		this.unit = unit;

		return this;
	}

	public DelayedAction setTask(Runnable task) {
		this.cachedTask = task;

		return this;
	}

	public void run(Runnable task) {
		stop();
		action = exec.schedule(cachedTask = task, time, unit);
	}

	public void start() {
		if (cachedTask != null) {
			if (action != null) return;

			action = exec.schedule(cachedTask, time, unit);
		}
	}

	public void stop() {
		if (action != null) {
			action.cancel(true);
			action = null;
		}
	}

	public void restart() {
		if (cachedTask != null) {
			stop();
			action = exec.schedule(cachedTask, time, unit);
		}
	}
}
