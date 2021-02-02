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

package com.kuuhaku.command.commands.discord.clan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "remover",
		aliases = {"remove"},
		usage = "req_mention-id",
		category = Category.CLAN
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class KickClanMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (!c.hasPermission(author.getId(), ClanPermission.ALTER_HIERARCHY)) {
			channel.sendMessage("❌ | Você não tem permissão para expulsar membros.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa mencionar ou informar o ID do membro a ser expulso.").queue();
			return;
		}

		User usr = message.getMentionedUsers().size() == 0 ? Main.getInfo().getUserByID(args[0]) : message.getMentionedUsers().get(0);

		if (usr == null) {
			if (c.getMembers().get(args[0]) == null) {
				channel.sendMessage("❌ | Membro inexistente.").queue();
				return;
			} else if (c.getMembers().get(args[0]).ordinal() <= c.getMembers().get(author.getId()).ordinal()) {
				channel.sendMessage("❌ | Você não pode expulsar membros com hierarquia maior ou igual à sua.").queue();
				return;
			}
		} else {
			if (c.getMembers().get(usr.getId()) == null) {
				channel.sendMessage("❌ | Membro inexistente.").queue();
				return;
			} else if (c.getMembers().get(usr.getId()).ordinal() <= c.getMembers().get(author.getId()).ordinal()) {
				channel.sendMessage("❌ | Você não pode expulsar membros com hierarquia maior ou igual à sua.").queue();
				return;
			}
		}

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Tem certeza que deseja expulsar o membro " + (usr == null ? "ID " + args[0] : usr.getName()) + "?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());

							if (usr != null) c.kick(usr, author);
							else c.kick(args[0], author);

							ClanDAO.saveClan(c);

							s.delete().flatMap(d -> channel.sendMessage("✅ | Membro expulso com sucesso.")).queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}
