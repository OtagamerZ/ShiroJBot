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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.converter.ChannelConverter;
import com.kuuhaku.model.persistent.javatype.ChannelJavaType;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JavaTypeRegistration;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "goodbye_settings")
@JavaTypeRegistration(javaType = GuildMessageChannel.class, descriptorClass = ChannelJavaType.class)
public class GoodbyeSettings extends DAO<GoodbyeSettings> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "header")
	@CollectionTable(name = "goodbye_settings_headers", joinColumns = @JoinColumn(name = "gid"))
	private Set<String> headers = new LinkedHashSet<>();

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "channel")
	@Convert(converter = ChannelConverter.class)
	private GuildMessageChannel channel;

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

	public GuildMessageChannel getChannel() {
		return channel;
	}

	public void setChannel(GuildMessageChannel channel) {
		this.channel = channel;
	}
}
