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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ReminderDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Reminder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Command(
		name = "lembrete",
		aliases = {"reminder", "rem"},
		usage = "req_period-repeating",
		category = Category.INFO
)
public class ReminderCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Reminder> rems = ReminderDAO.getReminders(author.getId());
		if (rems.size() >= 20) {
			channel.sendMessage("❌ | Você já marcou muitos lembretes.").queue();
			return;
		}

		List<String> params = Arrays.stream(argsAsText.split("([0-9]+[dhms])+|-r"))
				.filter(s -> !s.isBlank())
				.map(String::trim)
				.toList();
		if (params.isEmpty()) {
			channel.sendMessage("❌ | Você precisa informar um tempo.").queue();
			return;
		}

		long time = Helper.stringToDurationMillis(argsAsText);
		if (time < 60000) {
			channel.sendMessage("❌ | O tempo deve ser maior que 1 minuto.").queue();
			return;
		}

		boolean repeating = argsAsText.toLowerCase(Locale.ROOT).contains("-r");
		String desc = String.join(" ", params);
		if (desc.isBlank()) {
			channel.sendMessage("❌ | Você precisa informar uma razão.").queue();
			return;
		}

		Reminder rem = new Reminder(author.getId(), desc, time, repeating);
		ReminderDAO.saveReminder(rem);

		channel.sendMessage("✅ | Lembrete criado com sucesso, use `" + prefix + "lembretes` para ver seu cronograma.").queue();
	}
}
