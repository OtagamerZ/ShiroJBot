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
import com.kuuhaku.model.persistent.ClanMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "promover",
		aliases = {"promote", "prom"},
		usage = "req_id",
		category = Category.CLAN
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class PromoteClanMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (c.isLocked(author.getId(), ClanPermission.ALTER_HIERARCHY)) {
			channel.sendMessage("❌ | Você não tem permissão para promover membros.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa mencionar ou informar o ID do membro a ser promovido.").queue();
			return;
		}

		try {
			ClanMember cm = c.getMembers().get(Integer.parseInt(args[0]));

			if (cm.getRole().ordinal() <= c.getMember(author.getId()).getRole().ordinal()) {
				channel.sendMessage("❌ | Você não pode promover membros com hierarquia maior ou igual à sua.").queue();
				return;
			}

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Tem certeza que deseja promover o membro " + Helper.getUsername(cm.getUid()) + "?")
					.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());

								c.promote(cm.getUid(), author);
								ClanDAO.saveClan(c);

								s.delete().mapToResult().flatMap(d -> channel.sendMessage("✅ | Membro promovido com sucesso.")).queue();
							}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
					));
		} catch (IndexOutOfBoundsException | NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}
