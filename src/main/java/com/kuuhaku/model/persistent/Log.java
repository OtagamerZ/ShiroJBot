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

import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "logs")
public class Log {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String guildId = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String usr = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String guild = "";

	@Column(columnDefinition = "TEXT")
	private String command = "";

	@Column(columnDefinition = "VARCHAR(191)")
	private String timestamp = OffsetDateTime.now().atZoneSameInstant(ZoneId.of('GMT-3')).format(Helper.dateformat);

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGuildId() {
		return guildId;
	}

	public Log setGuildId(String guildId) {
		this.guildId = guildId;
		return this;
	}

	public String getUser() {
		return usr;
	}

	public Log setUser(User user) {
		this.usr = user.getAsTag();
		this.uid = user.getId();
		return this;
	}

	public String getGuild() {
		return guild;
	}

	public Log setGuild(String guild) {
		this.guild = guild;
		return this;
	}

	public String getCommand() {
		return command;
	}

	public Log setCommand(String command) {
		this.command = command;
		return this;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
}
