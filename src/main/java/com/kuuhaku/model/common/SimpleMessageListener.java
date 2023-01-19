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

package com.kuuhaku.model.common;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public abstract class SimpleMessageListener extends ListenerAdapter {
	private final GameChannel channel;
	public Object mutex = new Object();

	public SimpleMessageListener(TextChannel... channels) {
		this.channel = new GameChannel(channels);
	}

	public SimpleMessageListener(GameChannel channel) {
		this.channel = channel;
	}

	@Override
	public abstract void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event);

	public SimpleMessageListener getSelf() {
		return this;
	}

	public GameChannel getChannel() {
		return channel;
	}

	public boolean checkChannel(TextChannel channel) {
		return this.channel.getChannels().stream().anyMatch(tc -> tc.equals(channel));
	}

	public boolean checkChannel(String channel) {
		return this.channel.getChannels().stream().anyMatch(tc -> tc.getId().equals(channel));
	}

	public boolean isClosed() {
		return mutex == null;
	}

	public void close() {
		mutex = null;
	}
}