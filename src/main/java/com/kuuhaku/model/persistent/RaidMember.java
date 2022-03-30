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

@Entity
@Table(name = "raidmember")
public class RaidMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String sid;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Column(columnDefinition = "VARCHAR(255)")
	private String name;

	public RaidMember(String sid, String uid, String name) {
		this.sid = sid;
		this.uid = uid;
		this.name = name;
	}

	public RaidMember() {
	}

	public int getId() {
		return id;
	}

	public String getSid() {
		return sid;
	}

	public String getUid() {
		return uid;
	}

	public String getName() {
		return Helper.getOr(name, "Desconhecido");
	}
}
