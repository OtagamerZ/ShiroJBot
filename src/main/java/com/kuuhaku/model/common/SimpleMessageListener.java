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

package com.kuuhaku.model.common;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SimpleMessageListener {
	private final GameChannel channel;
	public Object mutex = new Object();

	public SimpleMessageListener(GuildMessageChannel... channels) {
		this.channel = new GameChannel(channels);
	}

	public SimpleMessageListener(GameChannel channel) {
		this.channel = channel;
	}

	public void execute(MessageReceivedEvent event) {
		CompletableFuture.runAsync(() -> onMessageReceived(event));
	}

	protected abstract void onMessageReceived(@NotNull MessageReceivedEvent event);

	public SimpleMessageListener getSelf() {
		return this;
	}

	public GameChannel getChannel() {
		return channel;
	}

	public boolean checkChannel(GuildMessageChannel channel) {
		return checkChannel(channel.getId());
	}

	public boolean checkChannel(String channel) {
		return this.channel.getChannels().parallelStream().anyMatch(tc -> tc.getId().equals(channel));
	}

	public boolean isClosed() {
		return mutex == null;
	}

	public void close() {
		mutex = null;
	}
}