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

package com.kuuhaku.model.persistent;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.enums.BountyDifficulty;
import com.kuuhaku.model.enums.Danger;
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.records.BountyInfo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONArray;
import com.kuuhaku.utils.JSONObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "bountyquest")
public class BountyQuest {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String name;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String description;

	@Enumerated(EnumType.STRING)
	private BountyDifficulty difficulty = BountyDifficulty.NONE;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5")
	private int baseTime = 5;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '[]'")
	private String baseStats = "[]";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '{}'")
	private String rewards = "{}";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '[]'")
	private String dangers = "[]";

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BountyDifficulty getDifficulty() {
		return difficulty;
	}

	public int getBaseTime() {
		return baseTime;
	}

	public Integer[] getBaseStats() {
		return new JSONArray(baseStats).stream()
				.map(v -> (int) (double) v)
				.toArray(Integer[]::new);
	}

	public Map<Reward, Integer> getRewards() {
		return new JSONObject(rewards).entrySet().stream()
				.map(e -> Pair.of(Reward.valueOf(e.getKey()), (int) (double) e.getValue()))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

	public Set<Danger> getDangers() {
		return new JSONArray(dangers).stream()
				.map(String::valueOf)
				.map(Danger::valueOf)
				.collect(Collectors.toSet());
	}

	public BountyInfo getInfo(Hero h, long seed) {
		if (difficulty == BountyDifficulty.NONE) {
			return new BountyInfo(id, baseTime, 0, new Attributes(getBaseStats()), getRewards());
		}

		double min = Math.max(1 + difficulty.ordinal() / 2d, difficulty.getValue() / 3d);
		double diff = Helper.round(Helper.rng(min, difficulty.getValue(), seed) / difficulty.getValue(), 2);
		Integer[] baseStats = Arrays.stream(getBaseStats())
				.map(i -> (int) Math.round(i * diff * difficulty.getValue()))
				.toArray(Integer[]::new);
		Integer[] heroStats = h.getStats().getStats();
		Map<Reward, Integer> rewards = getRewards();

		double modDiff = difficulty.getValue();
		double finDiff = modDiff;
		int statSum = Arrays.stream(baseStats).mapToInt(i -> i).sum();
		if (statSum == 0) finDiff = 0;
		else {
			for (int i = 0; i < baseStats.length; i++) {
				int base = baseStats[i];
				if (base <= 0) continue;

				double share = modDiff * ((double) base / statSum);
				int hero = heroStats[i];

				finDiff -= Helper.clamp(hero * share / base, 0, share);
			}
		}

		return new BountyInfo(
				id,
				(int) Math.round(baseTime * diff),
				Helper.round(finDiff, 1),
				new Attributes(baseStats),
				rewards.entrySet().stream()
						.map(e -> Pair.of(e.getKey(), (int) Math.round(e.getValue() * diff)))
						.collect(Collectors.toMap(Pair::getLeft, Pair::getRight))
		);
	}

	@Override
	public String toString() {
		return name;
	}
}
