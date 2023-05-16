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

package com.kuuhaku.model.persistent.user;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "voice_data")
public class VoiceData {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "time", nullable = false)
	private long time;

	@Column(name = "date", nullable = false)
	private ZonedDateTime date;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "profile_uid", referencedColumnName = "uid"),
			@PrimaryKeyJoinColumn(name = "profile_gid", referencedColumnName = "gid")
	})
	@Fetch(FetchMode.JOIN)
	private Profile profile;

	public VoiceData() {
	}

	public VoiceData(Profile profile) {
		this.date = ZonedDateTime.now(ZoneId.of("GMT-3"));
		this.profile = profile;
	}

	public int getId() {
		return id;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public ZonedDateTime getDate() {
		return date;
	}

	public Profile getProfile() {
		return profile;
	}
}
