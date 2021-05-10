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

package com.kuuhaku.model.persistent;

import javax.persistence.*;

@Entity
@Table(name = "leaderboards")
public class Leaderboards {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL")
	private String uid;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL")
	private String usr;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL")
	private String minigame;

	@Column(columnDefinition = "INT NOT NULL")
	private int score;

	public Leaderboards(String uid, String usr, String minigame, int score) {
		this.uid = uid;
		this.usr = usr;
		this.minigame = minigame;
		this.score = score;
	}

	public Leaderboards() {
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUsr() {
		return usr;
	}

	public void setUsr(String usr) {
		this.usr = usr;
	}

	public String getMinigame() {
		return minigame;
	}

	public void setMinigame(String minigame) {
		this.minigame = minigame;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}
}
