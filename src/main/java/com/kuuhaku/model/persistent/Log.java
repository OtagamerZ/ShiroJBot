/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import net.dv8tion.jda.api.entities.Guild;
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

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String guildId = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String usr = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String guild = "";

	@Column(columnDefinition = "TEXT")
	private String command = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String timestamp = "";

	public Log(Guild guild, User user, String command) {
		this.guildId = guild.getId();
		this.guild = guild.getName();
		this.usr = user.getAsTag();
		this.uid = user.getId();
		this.command = command;
		this.timestamp = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.fullDateFormat);
	}

	public Log() {
	}

	public int getId() {
		return id;
	}

	public String getGuildId() {
		return guildId;
	}

	public String getUsr() {
		return usr;
	}

	public String getUid() {
		return uid;
	}

	public String getGuild() {
		return guild;
	}

	public String getCommand() {
		return command;
	}

	public String getTimestamp() {
		return timestamp;
	}
}
