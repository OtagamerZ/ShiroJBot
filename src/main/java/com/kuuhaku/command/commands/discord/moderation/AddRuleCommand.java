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
import com.kuuhaku.model.persistent.guild.GuildConfig;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "novaregra",
		aliases = {"newrule", "nr"},
		usage = "req_rule",
		category = Category.MODERATION
)
public class AddRuleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário digitar uma regra para adicionar.").queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (!argsAsText.contains(";")) {
			channel.sendMessage("❌ | Para adicionar uma regra, informe um título e um texto separados por ponto-e-virgula.").queue();
			return;
		}

		String[] r = argsAsText.split(";");

		if (r[0].length() > 100) {
			channel.sendMessage("❌ | O título da regra só pode conter no máximo 100 caractéres.").queue();
			return;
		} else if (r[1].length() > 1000) {
			channel.sendMessage("❌ | O corpo da regra só pode conter no máximo 1000 caractéres.").queue();
			return;
		}

		gc.addAutoRule(argsAsText);
		channel.sendMessage("✅ | Regra adicionada com sucesso!").queue();

		GuildDAO.updateGuildSettings(gc);
	}
}
