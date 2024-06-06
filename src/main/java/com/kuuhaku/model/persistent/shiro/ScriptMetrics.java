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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "script_metrics")
public class ScriptMetrics extends DAO<ScriptMetrics> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "issuer", nullable = false)
	private String issuer;

	@Lob
	@Column(name = "script", nullable = false, columnDefinition = "TEXT")
	private String script;

	@Column(name = "run_time", nullable = false)
	private int runTime;

	@Column(name = "execution", nullable = false)
	private ZonedDateTime execution = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public ScriptMetrics() {
	}

	public ScriptMetrics(String issuer, String script, int runTime) {
		this.issuer = issuer;
		this.script = script;
		this.runTime = runTime;
	}

	public int getId() {
		return id;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getScript() {
		return script;
	}

	public int getRunTime() {
		return runTime;
	}

	public ZonedDateTime getExecution() {
		return execution;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ScriptMetrics that = (ScriptMetrics) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
