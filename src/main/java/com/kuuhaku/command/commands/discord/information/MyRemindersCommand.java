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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ReminderDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Reminder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "lembretes",
		aliases = {"reminders", "rems"},
		usage = "req_index-opt",
		category = Category.INFO
)
public class MyRemindersCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			List<Reminder> rems = ReminderDAO.getReminders(author.getId());
			if (rems.isEmpty()) {
				channel.sendMessage("❌ | Você ainda não criou nenhum lembrete.").queue();
				return;
			}

			ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
			List<Page> pages = new ArrayList<>();
			List<List<Reminder>> chunks = Helper.chunkify(rems, 10);
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor("Seu cronograma")
					.setThumbnail("https://cdn-icons-png.flaticon.com/512/193/193682.png");

			for (List<Reminder> chunk : chunks) {
				eb.clearFields();

				for (int i = 0; i < chunk.size(); i++) {
					Reminder rem = chunk.get(i);
					eb.addField(
							"`" + i + "` | " + rem.getNextReminder().format(Helper.FULL_DATE_FORMAT),
							(Helper.TIMESTAMP + "%s\n\n%s").formatted(
									now.plusSeconds(rem.getPeriod() / 1000).toEpochSecond(),
									rem.isRepeating() ? " - RECORRENTE" : "",
									StringUtils.abbreviate(rem.getDescription(), 100)
							),
							false
					);
				}

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			Reminder rem = ReminderDAO.getReminder(author.getId(), Integer.parseInt(args[0]));
			if (rem == null) {
				channel.sendMessage("❌ | Lembrete inexistente.").queue();
				return;
			}

			ReminderDAO.removeReminder(rem);
			channel.sendMessage("✅ | Lembrete removido com sucesso.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Índice inválido.").queue();
		}
	}
}
