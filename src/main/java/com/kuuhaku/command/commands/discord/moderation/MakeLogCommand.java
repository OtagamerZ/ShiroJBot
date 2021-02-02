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
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

@Command(
		name = "criarlog",
		aliases = {"makelog", "logchannel", "canallog"},
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_CHANNEL})
public class MakeLogCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		try {

			GuildConfig gc = GuildDAO.getGuildById(guild.getId());
			try {
				TextChannel tc = guild.getTextChannelById(gc.getCanalLog());
				if (tc != null)
					tc.delete().complete();
			} catch (Exception ignore) {
			}

			guild.createTextChannel("shiro-log")
					.setParent(channel.getParent())
					.queue(c -> {
						gc.setCanalLog(c.getId());
						channel.sendMessage("✅ | Canal de log criado com sucesso em " + c.getAsMention()).queue(null, Helper::doNothing);
						GuildDAO.updateGuildSettings(gc);
					});
		} catch (InsufficientPermissionException e) {
			channel.sendMessage("❌ | Não tenho permissões sufficientes para criar um canal.").queue();
		}
	}
}
