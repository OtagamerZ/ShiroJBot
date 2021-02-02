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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.BlacklistDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Blacklist;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "listanegra",
		aliases = {"blacklist", "bl"},
		usage = "req_id",
		category = Category.DEV
)
public class BlacklistCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_blacklist-no-id")).queue();
			return;
		}

		User u = Main.getInfo().getUserByID(args[0]);

		if (u == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id")).queue();
			return;
		}

		Blacklist bl = new Blacklist(u.getId(), author.getName());
		BlacklistDAO.blacklist(bl);

		channel.sendMessage("✅ | Usuário adicionado à lista negra com sucesso!").queue();
	}
}
