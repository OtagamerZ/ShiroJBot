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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ChecklistDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Checkitem;
import com.kuuhaku.model.persistent.Checklist;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "notas",
		aliases = {"mynotes"},
		usage = "req_id-opt",
		category = Category.MISC
)
public class MyChecklistsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			List<Checklist> lists = ChecklistDAO.getChecklists(author.getId());
			if (lists.isEmpty()) {
				channel.sendMessage("❌ | Você ainda não criou nenhum bloco de notas.").queue();
				return;
			}

			List<Page> pages = new ArrayList<>();
			List<List<Checklist>> chunks = Helper.chunkify(lists, 10);
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor("Meus blocos de notas")
					.setThumbnail("https://cdn2.iconfinder.com/data/icons/origami/PNG/256%20x%20256/archive.png");

			for (List<Checklist> chunk : chunks) {
				eb.clearFields();

				for (int i = 0; i < chunk.size(); i++) {
					Checklist list = chunk.get(i);
					eb.addField("`" + i + "` | " + list.getName(), Helper.VOID, false);
				}

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			Checklist c = ChecklistDAO.getChecklist(author.getId(), Integer.parseInt(args[0]));
			if (c == null) {
				channel.sendMessage("❌ | Bloco de notas inexistente.").queue();
				return;
			}

			List<Page> pages = new ArrayList<>();
			List<List<Checkitem>> chunks = Helper.chunkify(c.getItems(), 10);
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(c.getName())
					.setThumbnail("https://cdn-icons-png.flaticon.com/512/2438/2438239.png");

			for (List<Checkitem> chunk : chunks) {
				eb.clearFields();

				for (int i = 0; i < chunk.size(); i++) {
					Checkitem item = chunk.get(i);
					eb.addField("`" + i + "`", item.getDescription(), false);
				}

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Índice inválido.").queue();
		}
	}
}
