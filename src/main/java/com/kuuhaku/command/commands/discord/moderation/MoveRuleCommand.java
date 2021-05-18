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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

@Command(
		name = "moverregra",
		aliases = {"moverule", "mr"},
		usage = "req_index",
		category = Category.MODERATION
)
public class MoveRuleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | É necessário digitar o índice da regra a ser movida e a nova posição dela.").queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		List<String> rules = gc.getRules();

		try {
			int from = Integer.parseInt(args[0]);
			int to = Integer.parseInt(args[1]);
			if (!Helper.between(from, 0, rules.size()) || !Helper.between(to, 0, rules.size())) {
				channel.sendMessage("❌ | Regra inexistente ou nova posição inválida.").queue();
				return;
			}

			//gc.moveRule(from, to);
			channel.sendMessage("✅ | Regra movida com sucesso!").queue();

			GuildDAO.updateGuildSettings(gc);
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-index")).queue();
		}
	}
}
