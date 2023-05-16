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

import com.kuuhaku.model.records.ChannelReference;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.util.XStringBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GameChannel {
	private final Set<ChannelReference> channels = new HashSet<>();
	private final Map<String, XStringBuilder> buffer = new HashMap<>();
	private long lastAction = 0;
	private long cooldown = 0;

	public GameChannel(GuildMessageChannel... channel) {
		for (GuildMessageChannel chn : channel) {
			channels.add(new ChannelReference(chn.getGuild(), chn));
		}
	}

	public List<Guild> getGuilds() {
		return channels.stream()
				.map(ChannelReference::guild)
				.filter(Objects::nonNull)
				.toList();
	}

	public List<GuildMessageChannel> getChannels() {
		return channels.stream()
				.map(ChannelReference::channel)
				.filter(Objects::nonNull)
				.toList();
	}

	public GameChannel setCooldown(long time, TimeUnit unit) {
		cooldown = unit.toMillis(time);
		return this;
	}

	public ClusterAction sendFile(byte[] bytes, String filename) {
		long delay = Math.max(0, (lastAction + cooldown) - System.currentTimeMillis());

		Map<String, MessageCreateAction> acts = new HashMap<>();
		for (GuildMessageChannel chn : getChannels()) {
			acts.put(chn.getId(), chn.sendFiles(FileUpload.fromData(bytes, filename)));
		}

		lastAction = System.currentTimeMillis();
		return new ClusterAction(delay, acts);
	}

	public ClusterAction sendMessage(String message) {
		long delay = Math.max(0, (lastAction + cooldown) - System.currentTimeMillis());

		Map<String, MessageCreateAction> acts = new HashMap<>();
		for (GuildMessageChannel chn : getChannels()) {
			acts.put(chn.getId(), chn.sendMessage(message));
		}

		lastAction = System.currentTimeMillis();
		return new ClusterAction(delay, acts);
	}

	public void buffer(String message) {
		for (GuildMessageChannel chn : getChannels()) {
			buffer.computeIfAbsent(chn.getId(), k -> new XStringBuilder()).appendNewLine(message);
		}
	}

	public ClusterAction flush() {
		long delay = Math.max(0, (lastAction + cooldown) - System.currentTimeMillis());

		Map<String, MessageCreateAction> acts = new HashMap<>();
		for (GuildMessageChannel chn : getChannels()) {
			String msg = buffer.remove(chn.getId()).toString();
			if (!msg.isBlank()) {
				acts.put(chn.getId(), chn.sendMessage(msg));
			}
		}

		lastAction = System.currentTimeMillis();
		return new ClusterAction(delay, acts);
	}
}