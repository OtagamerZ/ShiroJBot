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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "emotes",
		usage = "req_name",
		category = Category.INFO
)
public class LocalEmoteListCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();
		List<MessageEmbed.Field> f = new ArrayList<>();

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		for (Emote emote : guild.getEmotes().stream().filter(e -> StringUtils.containsIgnoreCase(e.getAsMention(), args.length > 0 ? args[0] : "")).collect(Collectors.toList())) {
			f.add(new MessageEmbed.Field("Emote " + emote.getAsMention(), "Menção: " + emote.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false));
		}

		try {
			for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
				eb.clear();
				List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
				for (MessageEmbed.Field field : subF) {
					eb.addField(field);
				}

				eb.setTitle(ShiroInfo.getLocale(I18n.PT).getString("str_available-server-emotes"));
				Helper.finishEmbed(guild, pages, f, eb, i);
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_emote-not-found")).queue();
		}
	}
}
