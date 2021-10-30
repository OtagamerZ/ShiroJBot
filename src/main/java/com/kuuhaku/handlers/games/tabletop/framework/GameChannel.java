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

package com.kuuhaku.handlers.games.tabletop.framework;

import com.kuuhaku.handlers.games.tabletop.ClusterAction;
import com.kuuhaku.model.records.ChannelReference;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GameChannel {
	private final Set<ChannelReference> channels = new HashSet<>();

	public GameChannel(TextChannel... channel) {
		for (TextChannel chn : channel)
			channels.add(new ChannelReference(chn.getGuild(), chn));
	}

	public List<Guild> getGuilds() {
		return channels.stream()
				.map(ChannelReference::guild)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public List<TextChannel> getChannels() {
		return channels.stream()
				.map(ChannelReference::channel)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public ClusterAction sendFile(File f) {
		Map<String, MessageAction> acts = new HashMap<>();
		for (TextChannel chn : getChannels()) {
			acts.put(chn.getId(), chn.sendFile(f));
		}

		return new ClusterAction(acts);
	}

	public ClusterAction sendFile(byte[] bytes, String filename) {
		Map<String, MessageAction> acts = new HashMap<>();
		for (TextChannel chn : getChannels()) {
			acts.put(chn.getId(), chn.sendFile(bytes, filename));
		}

		return new ClusterAction(acts);
	}

	public ClusterAction sendMessage(String message) {
		Map<String, MessageAction> acts = new HashMap<>();
		for (TextChannel chn : getChannels()) {
			acts.put(chn.getId(), chn.sendMessage(message));
		}

		return new ClusterAction(acts);
	}
}
