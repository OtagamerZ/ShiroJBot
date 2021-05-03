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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "cargomute",
		aliases = {"muterole"},
		usage = "req_role-reset",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class MuteRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		int highest = member.getRoles().stream()
				.map(Role::getPosition)
				.max(Integer::compareTo)
				.orElse(-1);

		if (args.length > 0 && Helper.equalsAny(args[0], "limpar", "reset")) {
			gc.setMuteRole(null);
			channel.sendMessage("✅ | Cargo mute limpo com sucesso.").queue();
		} else {
			if (message.getMentionedRoles().isEmpty()) {
				channel.sendMessage("❌ | É necessário mencionar um cargo.").queue();
				return;
			}

			Role r = message.getMentionedRoles().get(0);

			if (r.getPosition() > guild.getSelfMember().getRoles().get(0).getPosition()) {
				channel.sendMessage("❌ | Esse cargo está acima de mim.").queue();
				return;
			} else if (r.getPosition() > highest) {
				channel.sendMessage("❌ | Você não pode atribuir cargos maiores que os seus.").queue();
				return;
			}

			gc.setMuteRole(r.getId());
			channel.sendMessage("✅ | Cargo mute definido com sucesso.").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
