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

package com.kuuhaku.model.persistent;

import org.json.JSONArray;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
public class MutedMember {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String uid;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String guild = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String reason = "";

	@Temporal(TemporalType.TIMESTAMP)
	private Timestamp mutedUntil;

	@Column(columnDefinition = "TEXT")
	private String roles = "[]";

	public MutedMember(String id, String guild) {
		this.uid = id;
		this.guild = guild;
	}

	public MutedMember() {
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getGuild() {
		return guild;
	}

	public void setGuild(String guild) {
		this.guild = guild;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public void mute(int time) {
		this.mutedUntil = Timestamp.from(Instant.now().plus(time, ChronoUnit.MINUTES));
	}

	public boolean isMuted() {
		return LocalDateTime.now().until(this.mutedUntil.toLocalDateTime(), ChronoUnit.MILLIS) > 0;
	}

	public JSONArray getRoles() {
		return new JSONArray(roles);
	}

	public void setRoles(JSONArray roles) {
		this.roles = new JSONArray(roles).toString();
	}
}
