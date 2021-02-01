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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.model.persistent.Clan;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "emblema",
		aliases = {"icon", "icone"},
		usage = "req_clanicon",
		category = Category.CLAN
)
public class ClanMotdCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (c.getMembers().get(author.getId()).ordinal() > 1) {
			channel.sendMessage("❌ | Apenas o líder, sub-líder e capitões podem alterar o MOTD do clã.").queue();
			return;
		} else if (c.getTier().ordinal() < ClanTier.FACTION.ordinal()) {
			channel.sendMessage("❌ | Seu clã ainda não desbloqueou o MOTD.").queue();
			return;
		}

		String motd = String.join(" ", args);
		if (motd.length() > 256) {
			channel.sendMessage("❌ | O MOTD deve ter no máximo 256 caractéres.").queue();
			return;
		}

		c.setMotd(motd);
		ClanDAO.saveClan(c);

		channel.sendMessage("✅ | MOTD alterado com sucesso.").queue();
	}
}
