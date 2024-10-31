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

package com.kuuhaku.model.persistent.shiro;

import com.kuuhaku.controller.DAO;
import jakarta.persistence.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "command_metrics")
public class CommandMetrics extends DAO<CommandMetrics> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "issuer", nullable = false)
	private String issuer;

	@Column(name = "guild", nullable = false)
	private String guild;

	@Column(name = "command", nullable = false)
	private String command;

	@Column(name = "run_time", nullable = false)
	private int runTime;

	@Column(name = "error", columnDefinition = "TEXT")
	private String error;

	@Column(name = "execution", nullable = false)
	private ZonedDateTime execution = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public CommandMetrics() {
	}

	public CommandMetrics(String issuer, String guild, String command, int runTime, Exception error) {
		this.issuer = issuer;
		this.guild = guild;
		this.command = command;
		this.runTime = runTime;

		if (error != null) {
			this.error = error + "\n" + ExceptionUtils.getStackTrace(error);
		}
	}

	public int getId() {
		return id;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getGuild() {
		return guild;
	}

	public String getCommand() {
		return command;
	}

	public int getRunTime() {
		return runTime;
	}

	public String getError() {
		return error;
	}

	public ZonedDateTime getExecution() {
		return execution;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CommandMetrics that = (CommandMetrics) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
