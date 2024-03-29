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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "recepcionista",
		aliases = {"welcomer"},
		usage = "req_role-reset",
		category = Category.MODERATION
)
public class WelcomerRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (message.getMentionedRolesBag().isEmpty() && args.length == 0) {
			Role r = gc.getWelcomerRole();
			if (r == null)
				channel.sendMessage("Ainda não foi definido um cargo de recepcionista.").queue();
			else
				channel.sendMessage("O cargo de recepcionista atual do servidor é " + r.getAsMention() + ".").queue();
			return;
		}

		try {
			if (Helper.equalsAny(args[0], "limpar", "reset")) {
				gc.setWelcomerRole(null);
				channel.sendMessage("✅ | Cargo de recepcionista limpo com sucesso.").queue();
			} else {
				gc.setWelcomerRole(message.getMentionedRoles().get(0).getId());
				channel.sendMessage("✅ | Cargo de recepcionista definido com sucesso.").queue();
			}
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Você precisa mencionar um cargo ou digitar `limpar`.").queue();
			return;
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
