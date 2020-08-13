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
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.BlacklistDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
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
		if (command != null) {
			if (command.requiresBinding() && acc == null) {
				client.getChat().sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_user-not-bound"));
				return;
			} else if (BlacklistDAO.isBlacklisted(author)) {
				client.getChat().sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_user-blacklisted"));
				return;
			} else if (Main.getInfo().getRatelimit().getIfPresent(author.getId()) != null) {
				client.getChat().sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_user-ratelimited"));
				return;
			}

			command.execute(author, acc, rawMsgNoPrefix, args, message, channel, client.getChat(), message.getPermissions());
			Main.getInfo().getRatelimit().put(author.getId(), true);

			String ad = Helper.getAd();
			if (ad != null) {
				client.getChat().sendMessage(channel.getName(), ad);
			}
		} else if (acc != null && Main.getInfo().isLive()) {
			acc.addCredit(5, this.getClass());
			AccountDAO.saveAccount(acc);
		}
	}

	private void onFollowEvent(FollowEvent evt) {
		Account acc = AccountDAO.getAccountByTwitchId(evt.getUser().getId());

		if (acc == null || acc.isFollower()) return;

		Main.getInfo().getUserByID(acc.getUserId()).openPrivateChannel().queue(c -> {
			try {
				EmbedBuilder eb = new EmbedBuilder();

				eb.setThumbnail("https://i.imgur.com/A0jXqpe.png");
				eb.setTitle("Opa, obrigada por seguir o canal do meu nii-chan!");
				eb.setDescription("Como agradecimento, aqui estão 5000 créditos para serem utilizados nos módulos que utilizam o sistema de dinheiro.");
				eb.setFooter("Seus créditos: " + (acc.getBalance() + 5000), "https://i.imgur.com/U0nPjLx.gif");
				eb.setColor(Color.cyan);

				if (ExceedDAO.hasExceed(c.getUser().getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(c.getUser().getId())));
					ps.modifyInfluence(50);
					PStateDAO.savePoliticalState(ps);

					eb.addField("Bonus ao seu Exceed", "Adicionalmente, seu Exceed recebeu 50 pontos de influência adicionais!", false);
				}

				acc.addCredit(5000, this.getClass());
				acc.setFollower(true);
				AccountDAO.saveAccount(acc);
				c.sendMessage(eb.build()).queue(null, Helper::doNothing);
			} catch (RuntimeException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}, Helper::doNothing);
	}

	private void onChannelGoLiveEvent(ChannelGoLiveEvent evt) {

	}

	public SimpleEventHandler getHandler() {
		return handler;
	}
}
