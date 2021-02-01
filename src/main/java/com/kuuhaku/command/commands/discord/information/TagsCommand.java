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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Tag;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "tags",
		aliases = {"insignias"},
		category = Category.INFO
)
public class TagsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		eb.setTitle(":label: Emblemas disponíveis");
		for (Tag t : Tag.values()) {
			if (t.equals(Tag.LEVEL)) {
				eb.addField(":tada: " + t.toString(), t.getDescription(), false);
			} else if (t.toString().contains("Coletado")) {
				eb.addField(t.getEmote().getTag(0) + t.toString() + "%", t.getDescription(), false);
			} else {
				eb.addField(t.getEmote().getTag(0) + t.toString(), t.getDescription(), false);
			}
		}

		channel.sendMessage(eb.build()).queue();
	}
}
