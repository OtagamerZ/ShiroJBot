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
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "semcomandos",
		aliases = {"nocommand", "blockcommand", "semcmd", "nocmd", "blockcmd"},
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_MANAGE})
public class NoCommandCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (gc.getNoCommandChannels().contains(channel.getId())) {
			gc.removeNoCommandChannel(message.getTextChannel().getId());
			channel.sendMessage("✅ | Comandos agora poderão ser executados neste canal.").queue();
		} else {
			gc.addNoCommandChannel(message.getTextChannel().getId());
			channel.sendMessage("✅ | Não executarei mais comandos neste canal.").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
