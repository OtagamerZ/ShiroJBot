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

package com.kuuhaku.events;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.FollowEvent;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.kuuhaku.Main;
import com.kuuhaku.command.TwitchCommand;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.sqlite.BlacklistDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class TwitchEvents {
	private final SimpleEventHandler handler;

	public TwitchEvents(TwitchClient client) {
		this.handler = client.getEventManager().getEventHandler(SimpleEventHandler.class);
		handler.onEvent(ChannelMessageEvent.class, this::onChannelMessageEvent);
		handler.onEvent(FollowEvent.class, this::onFollowEvent);
		handler.onEvent(ChannelGoLiveEvent.class, this::onChannelGoLiveEvent);
	}

	private void onChannelMessageEvent(ChannelMessageEvent message) {
		EventUser author = message.getUser();
		EventChannel channel = message.getChannel();
		TwitchClient client = Main.getTwitch();
		String rawMessage = StringUtils.normalizeSpace(message.getMessage());
		String rawMsgNoPrefix = rawMessage;
		String commandName = "";

		Account acc = AccountDAO.getAccountByTwitchId(author.getId());

		if (rawMessage.toLowerCase().startsWith(Main.getInfo().getDefaultPrefix())) {
			rawMsgNoPrefix = rawMessage.substring(Main.getInfo().getDefaultPrefix().length()).trim();
			commandName = rawMsgNoPrefix.split(" ")[0].trim();
		}

		boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
		String[] args = new String[]{};
		if (hasArgs) {
			args = Arrays.copyOfRange(rawMsgNoPrefix.split(" "), 1, rawMsgNoPrefix.split(" ").length);
			args = ArrayUtils.removeAllOccurences(args, "");
		}

		TwitchCommand command = Main.getTwitchCommandManager().getCommand(commandName);
		client.getChat().sendMessage(channel.getName(), "abc");
		if (command != null) {
			if (command.requiresBinding() && acc == null) {
				client.getChat().sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_user-not-bound"));
				return;
			} else if (BlacklistDAO.isBlacklisted(author)) {
				client.getChat().sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_user-blacklisted"));
				return;
			} else if (ShiroInfo.getRatelimit().getIfPresent(author.getId()) != null) {
				client.getChat().sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_user-ratelimited"));
				return;
			}

			command.execute(author, acc, rawMsgNoPrefix, args, message, channel, client.getChat(), message.getPermissions());
			ShiroInfo.getRatelimit().put(author.getId(), true);

			String ad = Helper.getAd();
			if (ad != null) {
				client.getChat().sendMessage(channel.getName(), ad);
			}
		}
	}

	private void onFollowEvent(FollowEvent evt) {

	}

	private void onChannelGoLiveEvent(ChannelGoLiveEvent evt) {

	}

	public SimpleEventHandler getHandler() {
		return handler;
	}
}
