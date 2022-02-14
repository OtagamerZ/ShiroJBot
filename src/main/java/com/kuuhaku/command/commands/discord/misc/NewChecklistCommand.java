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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Checklist;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "novobloco",
		aliases = {"newlist"},
		usage = "req_name",
		category = Category.MISC
)
public class NewChecklistCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		String name = args.length > 0 ? String.join(" ", args) : "Minhas notas " + (ChecklistDAO.getChecklists(author.getId()).size() + 1);
		if (name.length() > 250) {
			channel.sendMessage(I18n.getString("err_checklist-name-too-long")).queue();
			return;
		}

		Checklist c = new Checklist(author.getId(), name);
		ChecklistDAO.saveChecklist(c);
		channel.sendMessage("✅ | Bloco de notas criado com sucesso, use `" + prefix + "notas` para ver suas anotações.").queue();
	}
}
