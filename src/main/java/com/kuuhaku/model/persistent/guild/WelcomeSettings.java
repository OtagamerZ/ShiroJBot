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
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl;
import org.hibernate.annotations.JavaTypeRegistration;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "welcome_settings", schema = "shiro")
@JavaTypeRegistration(javaType = GuildMessageChannel.class, descriptorClass = ChannelJavaType.class)
public class WelcomeSettings extends DAO<WelcomeSettings> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "header", nullable = false)
	@CollectionTable(
			schema = "shiro",
			name = "welcome_settings_header",
			joinColumns = @JoinColumn(name = "gid")
	)
	private Set<String> headers = new LinkedHashSet<>();

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "channel")
	@Convert(converter = ChannelConverter.class)
	private TextChannelImpl channel;

	public WelcomeSettings() {
	}

	public WelcomeSettings(GuildConfig config) {
		this.gid = config.getGid();
		this.message = config.getLocale().get("default/welcome_message");
		this.headers.addAll(Set.of(
				"default/welcome_header_1",
				"default/welcome_header_2",
				"default/welcome_header_3",
				"default/welcome_header_4",
				"default/welcome_header_5"
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

	public TextChannelImpl getChannel() {
		return channel;
	}

	public void setChannel(TextChannelImpl channel) {
		this.channel = channel;
	}
}
