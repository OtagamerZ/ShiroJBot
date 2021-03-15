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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "prefixo",
		aliases = {"prefix", "p"},
		usage = "req_prefix",
		category = Category.MODERATION
)
public class PrefixCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("O prefixo atual do servidor é `" + prefix + "`.").queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		String p = args[0].trim();

		if (Helper.between(p.length(), 1, 6)) {
			channel.sendMessage("❌ | O prefixo deve possuir entre 1 e 5 caracteres.").queue();
			return;
		}

		channel.sendMessage("✅ | Prefixo definido com sucesso.").queue();
		gc.setPrefix(p);
		GuildDAO.updateGuildSettings(gc);
	}
}
