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

import com.kuuhaku.utils.JSONArray;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "mutedmember")
public class MutedMember {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String uid;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String guild = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String reason = "";

	@Column(columnDefinition = "TIMESTAMP")
	private LocalDateTime mutedUntil;

	@Column(columnDefinition = "TEXT")
	private String roles = "[]";

	public MutedMember(String id, String guild, JSONArray roles) {
		this.uid = id;
		this.guild = guild;
		this.roles = roles.toString();
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
		this.mutedUntil = LocalDateTime.from(LocalDateTime.now().plusMinutes(time));
	}

	public boolean isMuted() {
		return LocalDateTime.now().until(this.mutedUntil, ChronoUnit.MILLIS) > 0;
	}

	public JSONArray getRoles() {
		return new JSONArray(roles);
	}

	public void setRoles(JSONArray roles) {
		this.roles = roles.toString();
	}
}
