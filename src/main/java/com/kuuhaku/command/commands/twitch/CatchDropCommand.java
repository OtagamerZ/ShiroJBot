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

package com.kuuhaku.command.commands.twitch;

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.Main;
import com.kuuhaku.command.TwitchCommand;
import com.kuuhaku.model.common.drop.Prize;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;

import java.util.Set;

public class CatchDropCommand extends TwitchCommand {

	public CatchDropCommand(String name, String description, boolean requiresBinding) {
		super(name, description, requiresBinding);
	}

	public CatchDropCommand(String name, String[] aliases, String description, boolean requiresBinding) {
		super(name, aliases, description, requiresBinding);
	}

	public CatchDropCommand(String name, String usage, String description, boolean requiresBinding) {
		super(name, usage, description, requiresBinding);
	}

	public CatchDropCommand(String name, String[] aliases, String usage, String description, boolean requiresBinding) {
		super(name, aliases, usage, description, requiresBinding);
	}

	@Override
	public void execute(EventUser author, Account account, String rawCmd, String[] args, ChannelMessageEvent message, EventChannel channel, TwitchChat chat, Set<CommandPermission> permissions) {
		Prize p = Main.getInfo().getCurrentDrop().getIfPresent("twitch");

		if (p == null) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_no-drop"));
			return;
		}

		if (!p.getRequirementForTwitch().getValue().apply(author)) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_requirements-not-fulfilled"));
			return;
		} else if (args.length < 1) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_no-captcha"));
			return;
		} else if (!p.getCaptcha().equals(args[0])) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_invalid-captcha"));
			return;
		}

		Main.getInfo().getCurrentDrop().invalidate("twitch");
		p.award(author);

		chat.sendMessage(channel.getName(), author.getName() + " coletou o drop com sucesso!");
	}
}
