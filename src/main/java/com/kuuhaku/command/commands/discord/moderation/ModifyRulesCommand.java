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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class ModifyRulesCommand extends Command {

	public ModifyRulesCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ModifyRulesCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ModifyRulesCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ModifyRulesCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário digitar uma regra para adicionar ou um índice para remover.").queue();
			return;
		}

		String rule = String.join(" ", args);

		if (!StringUtils.isNumeric(rule) && !rule.contains(";")) {
			channel.sendMessage("❌ | Para adicionar uma regra, informe um título e um texto separados por ponto-e-virgula.").queue();
			return;
		}

		if (StringUtils.isNumeric(rule)) {
			int index = Integer.parseInt(rule) - 1;

			if (!Helper.between(index, 0, gc.getRules().size())) {
				channel.sendMessage("❌ | Não há nenhuma regra com esse índice.").queue();
				return;
			}

			gc.removeRule(index);
			GuildDAO.updateGuildSettings(gc);
			channel.sendMessage("✅ | Regra removida com sucesso!").queue();
		} else {
			if (rule.split(";").length != 2) {
				channel.sendMessage("❌ | A regra deve ter exatamente 1 separador (`;`).").queue();
				return;
			}

			String[] r = rule.split(";");

			if (r[0].length() > 100) {
				channel.sendMessage("❌ | O título da regra só pode conter no máximo 100 caractéres.").queue();
				return;
			}

			if (r[1].length() > 1000) {
				channel.sendMessage("❌ | O corpo da regra só pode conter no máximo 1000 caractéres.").queue();
				return;
			}

			gc.addRule(rule);
			GuildDAO.updateGuildSettings(gc);
			channel.sendMessage("✅ | Regra adicionada com sucesso!").queue();
		}
	}
}
