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
		name = "convidar",
		usage = "req_mention",
		category = Category.CLAN
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class InviteClanMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (c.isLocked(author.getId(), ClanPermission.INVITE)) {
			channel.sendMessage("❌ | Você não tem permissão para convidar membros.").queue();
			return;
		} else if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage("❌ | Você precisa mencionar o usuário a ser convidado.").queue();
			return;
		} else if (c.getMembers().size() >= c.getTier().getCapacity()) {
			channel.sendMessage("❌ | Seu clã já atingiu o número máximo de membros.").queue();
			return;
		}

		User usr = message.getMentionedUsers().get(0);
		if (ClanDAO.isMember(usr.getId())) {
			channel.sendMessage("❌ | Esse usuário já possui um clã.").queue();
			return;
		}

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage(usr.getAsMention() + ", você foi convidado(a) a juntar-se ao clã " + c.getName() + ", deseja aceitar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());

							c.invite(usr, author);

							ClanDAO.saveClan(c);

							s.delete().flatMap(d -> channel.sendMessage("✅ | " + usr.getAsMention() + " agora é membro do clã " + c.getName() + ".")).queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(usr.getId()),
						ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}
