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

package com.kuuhaku.manager;

import com.kuuhaku.interfaces.PreInitialize;
import com.kuuhaku.interfaces.annotations.Schedule;
import it.sauronsoftware.cron4j.Scheduler;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class ScheduleManager extends Scheduler {
	private final Reflections refl = new Reflections("com.kuuhaku.schedule");
	private final Set<Class<?>> scheds = refl.getTypesAnnotatedWith(Schedule.class);

	public ScheduleManager() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		for (Class<?> sched : scheds) {
			if (Runnable.class.isAssignableFrom(sched)) {
				Schedule info = sched.getDeclaredAnnotation(Schedule.class);

				Runnable task = (Runnable) sched.getConstructor().newInstance();
				schedule(info.value(), task);

				if (task instanceof PreInitialize) {
					task.run();
				}
			}
		}

		start();
	}

	public Set<Class<?>> getSchedules() {
		return scheds;
	}
}
