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

package com.kuuhaku.listeners;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.InvalidSignatureException;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.PreparedCommand;
import com.kuuhaku.utils.Calc;
import com.kuuhaku.utils.SignatureUtils;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.XStringBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.jodah.expiringmap.ExpiringMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class GuildListener extends ListenerAdapter {
	private static final ExpiringMap<String, Boolean> ratelimit = ExpiringMap.builder().variableExpiration().build();
	private static final ConcurrentMap<String, ExpiringMap<String, Message>> messages = new ConcurrentHashMap<>();

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		onGuildMessageReceived(new GuildMessageReceivedEvent(
				event.getJDA(),
				event.getResponseNumber(),
				event.getMessage()
		));
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		if (event.getJDA().getStatus() != JDA.Status.CONNECTED) return;
		if (!event.getChannel().getId().equals("718666970119143436")) return;
		else if (event.getAuthor().isBot()) return;

		String content = event.getMessage().getContentRaw();

		MessageData.Guild data = new MessageData.Guild(event);
		GuildConfig config = DAO.find(GuildConfig.class, data.guild().getId());
		if (!Objects.equals(config.getName(), data.guild().getName())) {
			config.setName(data.guild().getName());
			config.save();
		}

		Profile profile = DAO.find(Profile.class, new ProfileId(data.user().getId(), data.guild().getId()));
		Account account = profile.getAccount();
		if (!Objects.equals(account.getName(), data.user().getName())) {
			account.setName(data.user().getName());
			account.save();
		}

		EventData ed = new EventData(config, profile);
		if (content.toLowerCase(Locale.ROOT).startsWith(config.getPrefix())) {
			processCommand(data, ed, content);
		}

		messages.computeIfAbsent(data.guild().getId(), k ->
				ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
		).put(data.message().getId(), data.message());
	}

	private void processCommand(MessageData.Guild data, EventData event, String content) {
		I18N locale = event.config().getLocale();
		String[] args = content.toLowerCase(Locale.ROOT).split("\s+");
		String name = args[0].replaceFirst(event.config().getPrefix(), "");

		PreparedCommand pc = Main.getCommandManager().getCommand(name);
		if (pc != null) {
			Permission[] missing = pc.getMissingPerms(data.channel());

			if (event.config().getSettings().getDisabledCategories().contains(pc.category())) {
				data.channel().sendMessage(locale.get("error/disabled_category")).queue();
				return;
			} else if (event.config().getSettings().getDisabledCommands().contains(pc.command().getClass().getCanonicalName())) {
				data.channel().sendMessage(locale.get("error/disabled_command")).queue();
				return;
			} else if (missing.length > 0) {
				XStringBuilder sb = new XStringBuilder(locale.get("error/missing_perms"));
				for (Permission perm : missing) {
					sb.appendNewLine("- " + locale.get("perm/" + perm.name()));
				}

				data.channel().sendMessage(sb.toString()).queue();
				return;
			}

			if (event.profile().getAccount().isBlacklisted()) {
				data.channel().sendMessage(locale.get("error/blacklisted")).queue();
				return;
			} else if (!pc.category().check(data.member())) {
				data.channel().sendMessage(locale.get("error/not_allowed")).queue();
				return;
			} else if (ratelimit.containsKey(data.user().getId())) {
				data.channel().sendMessage(locale.get("error/ratelimited")).queue();
				return;
			}

			try {
				Map<String, String> params = SignatureUtils.parse(locale, pc.command(), content.substring(args[0].length()).trim());

				pc.command().execute(data.guild().getJDA(), event.config().getLocale(), event, data, params);

				if (!Constants.SUP_PRIVILEGE.apply(data.member())) {
					ratelimit.put(data.user().getId(), true, Calc.rng(2000, 3500), TimeUnit.MILLISECONDS);
				}
			} catch (InvalidSignatureException e) {
				data.channel().sendMessage(locale.get("error/invalid_signature") + "```css\n%s%s %s```".formatted(
						event.config().getPrefix(),
						name,
						e.getMessage().replace("`", "'")
				)).queue();
			}
		}
	}

	public static List<Message> getMessages(Guild guild) {
		return List.copyOf(Utils.getOr(messages.get(guild.getId()), Map.<String, Message>of()).values());
	}
}
