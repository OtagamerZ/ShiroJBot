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

import com.kuuhaku.controller.DAO;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "raidinfo")
public class RaidInfo extends DAO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String sid;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long duration = 0;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "raidinfo_id")
	private List<RaidMember> members = new ArrayList<>();

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime occurrence = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public RaidInfo(String sid, long duration) {
		this.sid = sid;
		this.duration = duration;
	}

	public RaidInfo() {
	}

	public int getId() {
		return id;
	}

	public String getSid() {
		return sid;
	}

	public long getDuration() {
		return duration;
	}

	public List<RaidMember> getMembers() {
		return members;
	}

	public ZonedDateTime getOccurrence() {
		return occurrence;
	}
}
