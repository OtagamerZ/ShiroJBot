/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.tabletop.ClusterAction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameChannel {
	private final Set<String> channels = new HashSet<>();

	public GameChannel(TextChannel... channel) {
		for (TextChannel chn : channel)
			channels.add(chn.getGuild().getId() + "." + chn.getId());
	}

	public List<Guild> getGuilds() {
		return channels.stream()
				.map(ids -> ids.split(Pattern.quote("."))[0])
				.map(id -> Main.getInfo().getGuildByID(id))
				.collect(Collectors.toList());
	}

	public List<TextChannel> getChannels() {
		return channels.stream()
				.map(ids -> ids.split(Pattern.quote(".")))
				.map(ids -> Main.getInfo().getGuildByID(ids[0]).getTextChannelById(ids[1]))
				.collect(Collectors.toList());
	}

	public ClusterAction sendFile(byte[] bytes, String filename) {
		List<MessageAction> acts = new ArrayList<>();
		for (String chn : channels) {
			String[] ids = chn.split(Pattern.quote("."));
			try {
				acts.add(Objects.requireNonNull(Main.getInfo()
						.getGuildByID(ids[0])
						.getTextChannelById(ids[1]))
						.sendFile(bytes, filename)
				);
			} catch (NullPointerException ignore) {
			}
		}
		return new ClusterAction(acts);
	}

	public ClusterAction sendMessage(String message) {
		List<MessageAction> acts = new ArrayList<>();
		for (String chn : channels) {
			String[] ids = chn.split(Pattern.quote("."));
			try {
				acts.add(Objects.requireNonNull(Main.getInfo()
						.getGuildByID(ids[0])
						.getTextChannelById(ids[1]))
						.sendMessage(message)
				);
			} catch (NullPointerException ignore) {
			}
		}
		return new ClusterAction(acts);
	}
}
