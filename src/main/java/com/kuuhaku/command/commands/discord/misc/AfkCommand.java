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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "afk",
		aliases = {"javolto", "off"},
		usage = "req_opt-text",
		category = Category.MISC
)
public class AfkCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		String text = args.length > 0 ? String.join(" ", args) : "Não estou disponível no momento, tente mais tarde!";
		if (text.length() > 250) {
			channel.sendMessage(I18n.getString("err_afk-message-too-long")).queue();
			return;
		}

		if (guild.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE) && !member.getUser().getName().startsWith("[AFK]")) {
			try {
				member.modifyNickname("[AFK] " + member.getUser().getName()).queue(null, Helper::doNothing);
			} catch (Exception ignore) {
			}
		}

		Account acc = AccountDAO.getAccount(author.getId());
		acc.setAfkMessage(text);
		AccountDAO.saveAccount(acc);
		if (args.length == 0)
			channel.sendMessage("✅ | Você permanecerá no modo AFK até falar algo no chat.").queue();
		else
			channel.sendMessage("✅ | Mensagem definida com sucesso! Você permanecerá no modo AFK até falar algo no chat.").queue();
	}
}
