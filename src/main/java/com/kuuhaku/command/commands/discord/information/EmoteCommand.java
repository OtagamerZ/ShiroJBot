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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.helpers.ImageHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;

@Command(
		name = "emote",
		aliases = {"emoji", "emt", "emj"},
		usage = "req_id-emote",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class EmoteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0 && message.getEmotes().size() < 1) {
			channel.sendMessage("❌ | Você precisa informar um emote.").queue();
			return;
		}

		try {
			String id;
			if (!message.getEmotes().isEmpty())
				id = message.getEmotes().get(0).getId();
			else
				id = ShiroInfo.getEmoteLookup().getOrDefault(args[0], "1");

			Emote emt = Main.getShiro().getEmoteById(id);

			if (emt == null) {
				channel.sendMessage("❌ | Não conheço esse emote.").queue();
				return;
			}

			EmbedBuilder eb = new EmbedBuilder()
					.setTitle(emt.getName(), emt.getImageUrl())
					.setThumbnail(emt.getImageUrl())
					.setColor(ImageHelper.colorThief(emt.getImageUrl()));

			Guild g = emt.getGuild();
			eb.addField(
					"Servidor: " + (g == null ? "desconhecido" : emt.getGuild().getName()),
					"**ID:** " + emt.getId(),
					true
			);

			channel.sendMessageEmbeds(eb.build()).queue();
		} catch (IOException e) {
			channel.sendMessage("❌ | Ocorreu um erro ao tentar recuperar a imagem do emote.").queue();
		}
	}
}
