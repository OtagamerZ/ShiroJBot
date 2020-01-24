/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.model;

import com.kuuhaku.utils.Helper;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
public class Log {
	@Id
	@GeneratedValue
	private int id;
	private String user;
	private String guild;
	private String command;
	private final String timestamp = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.dateformat);

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public Log setUser(String user) {
		this.user = user;
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
}
