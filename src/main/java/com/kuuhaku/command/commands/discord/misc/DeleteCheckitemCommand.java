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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ChecklistDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Checkitem;
import com.kuuhaku.model.persistent.Checklist;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "removerafazer",
		aliases = {"removernota", "removetodo", "removenote"},
		usage = "req_index-index",
		category = Category.MISC
)
public class DeleteCheckitemCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar o índice do bloco de notas.").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar o índice da anotação.").queue();
			return;
		}

		try {
			Checklist c = ChecklistDAO.getChecklist(author.getId(), Integer.parseInt(args[0]));
			if (c == null) {
				channel.sendMessage("❌ | Bloco de notas inexistente.").queue();
				return;
			}

			Checkitem i = Helper.safeGet(c.getItems(), Integer.parseInt(args[1]));
			if (i == null) {
				channel.sendMessage("❌ | Anotação inexistente.").queue();
				return;
			}

			c.getItems().remove(i);
			ChecklistDAO.saveChecklist(c);
			channel.sendMessage("✅ | Anotação removida com sucesso.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Índice inválido.").queue();
		}
	}
}