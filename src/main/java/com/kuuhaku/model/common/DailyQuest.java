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

package com.kuuhaku.model.common;

import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.utils.Helper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DailyQuest {
	private final HashMap<DailyTask, Integer> tasks = new HashMap<>();

	private DailyQuest(String id) {
		long seed = LocalDate.now().toEpochDay() + Long.parseLong(id);
		List<DailyTask> tasks = Helper.getRandomN(List.of(DailyTask.values()), 3, 1, seed);

		Random r = new Random(seed);
		for (DailyTask task : tasks) {
			this.tasks.put(task,
					switch (task) {
						case CREDIT_TASK -> 5000 + Helper.rng(45000, r, false);
						case CARD_TASK -> 5 + Helper.rng(20, r, false);
						case DROP_TASK -> 10 + Helper.rng(10, r, false);
						case WINS_TASK -> 1 + Helper.rng(4, r, false);
						case XP_TASK -> 1000 + Helper.rng(9000, r, false);
						case CANVAS_TASK -> 10 + Helper.rng(40, r, false);
					}
			);
		}
	}

	public HashMap<DailyTask, Integer> getTasks() {
		return tasks;
	}

	public float getDifficultyMod() {
		float mod = 1;
		for (Map.Entry<DailyTask, Integer> task : tasks.entrySet()) {
			DailyTask dt = task.getKey();
			mod += 1 * switch (dt) {
				case CREDIT_TASK -> Helper.prcnt(task.getValue(), 50000);
				case CARD_TASK -> Helper.prcnt(task.getValue(), 25);
				case DROP_TASK -> Helper.prcnt(task.getValue(), 20);
				case WINS_TASK -> Helper.prcnt(task.getValue(), 5);
				case XP_TASK -> Helper.prcnt(task.getValue(), 10000);
				case CANVAS_TASK -> Helper.prcnt(task.getValue(), 50);
			};
		}
		return mod;
	}

	public boolean checkTasks(Map<DailyTask, Integer> tasks) {
		for (Map.Entry<DailyTask, Integer> task : this.tasks.entrySet()) {
			int progress = tasks.getOrDefault(task.getKey(), 0);
			if (progress < task.getValue())
				return false;
		}
		return true;
	}

	public static DailyQuest getQuest(String id) {
		return new DailyQuest(id);
	}
}
