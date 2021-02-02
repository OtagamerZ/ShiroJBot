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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "bio",
		aliases = {"story", "desc"},
		usage = "req_text",
		category = Category.MISC
)
public class BiographyCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (String.join(" ", args).length() > 140) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_biography-too-long")).queue();
			return;
		}

		for (String s : args)
			if (s.length() > 20) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_biography-big-words")).queue();
				return;
			}

		String text = String.join(" ", args);

		Account acc = AccountDAO.getAccount(author.getId());
		acc.setBio(text);
		AccountDAO.saveAccount(acc);
		if (text.length() > 0) channel.sendMessage("✅ | Biografia definida com sucesso!").queue();
		else channel.sendMessage("✅ | Biografia limpa com sucesso!").queue();
	}
}
