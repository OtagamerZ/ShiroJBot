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

package com.kuuhaku.model.persistent.guild.buttons;

import com.kuuhaku.model.persistent.guild.GuildConfig;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "buttonchannel")
public class ButtonChannel {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn("guildconfig_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private GuildConfig guildConfig;

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<ButtonMessage> messages = new HashSet<>();

	public ButtonChannel() {
	}

	public ButtonChannel(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public GuildConfig getGuildConfig() {
		return guildConfig;
	}

	public Set<ButtonMessage> getMessages() {
		return messages;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ButtonChannel that = (ButtonChannel) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
