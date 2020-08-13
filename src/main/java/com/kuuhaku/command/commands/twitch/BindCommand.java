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
import com.kuuhaku.command.TwitchCommand;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.PendingBindingDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.PendingBinding;
import com.kuuhaku.utils.Helper;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class BindCommand extends TwitchCommand {

	public BindCommand(String name, String description, boolean requiresBinding) {
		super(name, description, requiresBinding);
	}

	public BindCommand(String name, String[] aliases, String description, boolean requiresBinding) {
		super(name, aliases, description, requiresBinding);
	}

	public BindCommand(String name, String usage, String description, boolean requiresBinding) {
		super(name, usage, description, requiresBinding);
	}

	public BindCommand(String name, String[] aliases, String usage, String description, boolean requiresBinding) {
		super(name, aliases, usage, description, requiresBinding);
	}

	@Override
	public void execute(EventUser author, Account account, String rawCmd, String[] args, ChannelMessageEvent message, EventChannel channel, TwitchChat chat, Set<CommandPermission> permissions) {
		Account acc = AccountDAO.getAccountByTwitchId(author.getId());

		if (acc != null) {
			chat.sendMessage(channel.getName(), "❌ | Você já vinculou esta conta a um perfil do Discord.");
			return;
		}

		try {
			String code = Hex.encodeHexString(MessageDigest.getInstance("SHA-1").digest(author.getName().getBytes(StandardCharsets.UTF_8)));

			if (PendingBindingDAO.getPendingBinding(code) != null) {
				chat.sendMessage(channel.getName(), "❌ | Você já requisitou uma vinculação a esta conta, verifique suas mensagens privadas.");
				return;
			}

			PendingBinding pb = new PendingBinding(code, author.getId());
			PendingBindingDAO.savePendingBinding(pb);

			chat.sendPrivateMessage(author.getName().toLowerCase(), "Use este código no comando \"vincular\" em um servidor que use a Shiro para vincular esta conta ao seu perfil do Discord.");
		} catch (NoSuchAlgorithmException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
