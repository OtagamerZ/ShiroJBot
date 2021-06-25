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

package com.kuuhaku.model.persistent.guild;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "voicerole")
public class VoiceRole {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn("guildconfig_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private GuildConfig guildConfig;

	@Column(columnDefinition = "BIGINT NOT NULL")
	private long time;

	public VoiceRole() {
	}

	public VoiceRole(String id, long time) {
		this.id = id;
		this.time = time;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public GuildConfig getGuildConfig() {
		return guildConfig;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VoiceRole voiceRole = (VoiceRole) o;
		return Objects.equals(id, voiceRole.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
