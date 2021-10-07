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

import com.kuuhaku.model.persistent.id.CompositeRoleId;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "temprole")
@IdClass(CompositeRoleId.class)
public class TempRole {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String sid;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String rid;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime until = null;

	public TempRole() {
	}

	public TempRole(Member mb, Role role, ZonedDateTime until) {
		this.uid = mb.getId();
		this.sid = mb.getGuild().getId();
		this.rid = role.getId();
		this.until = until;
	}

	public String getUid() {
		return uid;
	}

	public String getSid() {
		return sid;
	}

	public String getRid() {
		return rid;
	}

	public ZonedDateTime getUntil() {
		return until;
	}
}
