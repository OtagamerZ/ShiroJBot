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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;
import java.util.Objects;

@Command(
		name = "emote",
		aliases = {"emoji", "emt", "emj"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class EmoteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getEmotes().size() < 1) {
			channel.sendMessage("❌ | Você precisa informar um emote.").queue();
			return;
		}

		try {
			Emote emt = message.getEmotes().get(0);
			Emote cached = Main.getShiroShards().getEmoteById(emt.getId());
			EmbedBuilder eb = new EmbedBuilder()
					.addField(emt.getName(), """
							Guild: %s
							ID: %s
							""".formatted(
							cached == null ? "desconhecido" : Objects.requireNonNull(cached.getGuild()).getName(),
							emt.getId()
					), true)
					.setThumbnail(emt.getImageUrl())
					.setColor(Helper.colorThief(emt.getImageUrl()));

			channel.sendMessage(eb.build()).queue();
		} catch (IOException e) {
			channel.sendMessage("❌ | Ocorreu um erro ao tentar recuperar a imagem do emote.").queue();
		}
	}
}
