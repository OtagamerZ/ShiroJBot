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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Command(
		name = "desbanir",
		aliases = {"unban"},
		usage = "req_ids",
		category = Category.MODERATION
)
@Requires({Permission.BAN_MEMBERS})
public class UnbanMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa digitar o ID de ao menos um usuário.").queue();
			return;
		} else if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			channel.sendMessage(I18n.getString("err_unban-not-allowed")).queue();
			return;
		}

		Set<User> users = new HashSet<>();
		for (String id : args) {
			try {
				User u = Main.getInfo().getUserByID(id);
				if (u == null) {
					channel.sendMessage(I18n.getString("err_invalid-user-with-id", id)).queue();
					return;
				}
				users.add(u);
			} catch (NumberFormatException e) {
				channel.sendMessage(I18n.getString("err_invalid-id-value")).queue();
			}
		}

		if (users.size() > 1) {
			List<AuditableRestAction<Void>> acts = new ArrayList<>();

			for (User u : users) {
				acts.add(guild.unban(u));
			}

			RestAction.allOf(acts)
					.mapToResult()
					.flatMap(s -> channel.sendMessage("✅ | Membros desbanidos com sucesso!"))
					.queue(null, Helper::doNothing);
		} else {
			User u = users.stream().findFirst().orElse(null);

			assert u != null;
			guild.unban(u)
					.flatMap(s -> channel.sendMessage("✅ | " + u.getName() + " desbanido com sucesso!"))
					.queue(null, Helper::doNothing);
		}
	}
}
