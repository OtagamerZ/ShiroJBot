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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Command(
		name = "netcat",
		aliases = {"search", "lts", "aap"},
		usage = "req_http",
		category = Category.INFO
)
public class LearnToSearchCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa informar algo para pesquisar.").queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		eb.setTitle("Aqui está seu resultado!");
		eb.setDescription("[Clique aqui para ver.](https://pt-br.lmgtfy.com/?q=" + URLEncoder.encode(String.join(" ", args), StandardCharsets.UTF_8) + "&iie=1)");
		eb.setThumbnail("https://img.icons8.com/cotton/2x/checkmark.png");

		channel.sendMessage(eb.build()).queue();
	}
}
