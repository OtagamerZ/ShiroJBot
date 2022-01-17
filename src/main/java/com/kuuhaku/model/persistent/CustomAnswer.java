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

import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customanswer")
public class CustomAnswer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String guildId = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String trigger = "";

	@Column(columnDefinition = "TEXT")
	private String answer = "";

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean anywhere = false;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 100")
	private int chance = 100;

	@ElementCollection(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, name = "customanswer_id")
	private final Set<String> users = new HashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, name = "customanswer_id")
	private final Set<String> channels = new HashSet<>();

	public CustomAnswer(Integer id, String guildId, String trigger, String answer, Boolean anywhere, Integer chance, String users, String channels) {
		this.id = id;
		this.guildId = guildId;
		this.trigger = trigger;
		this.answer = answer;
		this.anywhere = anywhere;
		this.chance = chance;

		for (String s : Helper.getOr(users, "").split(",")) {
			if (!s.isBlank()) this.users.add(s);
		}

		for (String s : Helper.getOr(channels, "").split(",")) {
			if (!s.isBlank()) this.channels.add(s);
		}
	}

	public CustomAnswer(String guildId, String trigger, String answer) {
		this.guildId = guildId;
		this.trigger = trigger;
		this.answer = answer;
	}

	public CustomAnswer() {
	}

	public int getId() {
		return id;
	}

	public String getGuildId() {
		return guildId;
	}

	public void setGuildId(String guildID) {
		this.guildId = guildID;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String gatilho) {
		this.trigger = gatilho;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public boolean isAnywhere() {
		return anywhere;
	}

	public void setAnywhere(boolean anywhere) {
		this.anywhere = anywhere;
	}

	public int getChance() {
		return chance;
	}

	public void setChance(int chance) {
		this.chance = chance;
	}

	public Set<String> getUsers() {
		return users;
	}

	public Set<String> getChannels() {
		return channels;
	}
}
