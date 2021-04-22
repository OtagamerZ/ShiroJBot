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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "topicogeral",
		aliases = {"tgeral", "generaltopic"},
		usage = "req_text",
		category = Category.MODERATION
)
public class GeneralTopicCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (args.length == 0) {
			channel.sendMessage("O tópico do canal geral atual do servidor é ```" + gc.getGeneralTopic() + "```.").queue();
			return;
		}

		if (!Helper.between(argsAsText.length(), 0, 512)) {
			channel.sendMessage("❌ | O tópico deve possuir entre 0 e 512 caracteres.").queue();
			return;
		}

		if (argsAsText.length() == 0) {
			gc.setGeneralTopic(null);
			channel.sendMessage("✅ | Tópico geral limpo com sucesso.").queue();
		} else {
			gc.setGeneralTopic(argsAsText);
			channel.sendMessage("✅ | Tópico geral definido com sucesso.").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
