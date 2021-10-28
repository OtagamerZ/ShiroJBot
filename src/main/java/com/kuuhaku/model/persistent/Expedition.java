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
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "expedition")
public class Expedition {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String name = "{}";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 1")
	private int difficulty = 1;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5")
	private int time = 5;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '{}'")
	private String rewards = "{}";

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public int getTime() {
		return time;
	}

	public JSONObject getRewards() {
		return new JSONObject(rewards);
	}

	public void setRewards(JSONObject rewards) {
		this.rewards = rewards.toString();
	}

	public int getSuccessChance(Hero h) {
		return (int) Helper.clamp(difficulty * 100 / Math.max(1, h.getLevel() / 2f), 0, 100);
	}
}
