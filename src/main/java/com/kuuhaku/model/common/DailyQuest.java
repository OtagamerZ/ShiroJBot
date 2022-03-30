/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MathHelper;

import java.util.*;

public class DailyQuest {
	private final HashMap<DailyTask, Integer> tasks = new HashMap<>();
	private final AddedAnime chosenAnime;
	private final Race chosenRace;
	private final double divergence;

	private DailyQuest(long id) {
		Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT-3:00"));
		long seed = (id / (today.get(Calendar.DAY_OF_YEAR) + today.get(Calendar.YEAR)));
		List<DailyTask> tasks = CollectionHelper.getRandomN(List.of(DailyTask.values()), 3, 1, seed);

		Random r = new Random(seed);
		List<AddedAnime> animes = AddedAnime.queryAll(AddedAnime.class, "SELECT a FROM AddedAnime a WHERE a.hidden = FALSE");
		this.chosenAnime = CollectionHelper.getRandomEntry(r, animes);
		this.chosenRace = CollectionHelper.getRandomEntry(r, Race.validValues());
		this.divergence = MathHelper.round(MathHelper.rng(0.2, 0.5, r), 3);
		for (DailyTask task : tasks) {
			this.tasks.put(task,
					switch (task) {
						case CREDIT_TASK -> MathHelper.rng(5000, 50000, r);
						case CARD_TASK -> MathHelper.rng(5, 20, r);
						case DROP_TASK -> MathHelper.rng(10, 20, r);
						case WINS_TASK, OFFMETA_TASK -> MathHelper.rng(1, 5, r);
						case XP_TASK -> MathHelper.rng(1000, 10000, r);
						case ANIME_TASK -> MathHelper.rng(1, 3, r);
						case RACE_TASK -> MathHelper.rng(2, 10, r);
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
				case CREDIT_TASK -> MathHelper.prcnt(task.getValue(), 25000) * 0.55;
				case CARD_TASK -> MathHelper.prcnt(task.getValue(), 25);
				case DROP_TASK -> MathHelper.prcnt(task.getValue(), 20);
				case WINS_TASK -> MathHelper.prcnt(task.getValue(), 5) * 1.25;
				case OFFMETA_TASK -> MathHelper.prcnt(task.getValue(), 1.75);
				case XP_TASK -> MathHelper.prcnt(task.getValue(), 10000) * 0.45;
				case ANIME_TASK -> MathHelper.prcnt(task.getValue(), 3) * 0.9;
				case RACE_TASK -> MathHelper.prcnt(task.getValue(), 10) * 1.1;
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

	public static DailyQuest getQuest(long id) {
		return new DailyQuest(id);
	}

	public AddedAnime getChosenAnime() {
		return chosenAnime;
	}

	public Race getChosenRace() {
		return chosenRace;
	}

	public double getDivergence() {
		return divergence;
	}
}
