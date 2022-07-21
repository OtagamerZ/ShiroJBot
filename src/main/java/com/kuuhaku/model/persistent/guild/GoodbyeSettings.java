/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.DAO;
import net.dv8tion.jda.api.entities.TextChannel;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "goodbye_settings")
public class GoodbyeSettings extends DAO<GoodbyeSettings> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@ElementCollection
	@Column(name = "header")
	@CollectionTable(name = "goodbye_settings_headers", joinColumns = @JoinColumn(name = "gid"))
	private Set<String> headers = new LinkedHashSet<>();

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "channel")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.ChannelStringType")
	private TextChannel channel;

	public GoodbyeSettings() {
	}

	public GoodbyeSettings(GuildConfig config) {
		this.gid = config.getGid();
		this.message = config.getLocale().get("default/goodbye_message");
		this.headers.addAll(Set.of(
				"default/goodbye_header_1",
				"default/goodbye_header_2",
				"default/goodbye_header_3",
				"default/goodbye_header_4",
				"default/goodbye_header_5"
		));
	}

	public String getGid() {
		return gid;
	}

	public Set<String> getHeaders() {
		return headers;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public TextChannel getChannel() {
		return channel;
	}

	public void setChannel(TextChannel channel) {
		this.channel = channel;
	}
}
