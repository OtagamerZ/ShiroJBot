/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.info;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Emote;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "help",
		category = Category.INFO
)
@Signature({
		"<category:word>",
		"<command:word>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class HelpCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<Category> categories = new ArrayList<>();
		for (Category cat : Category.values()) {
			if (cat.check(event.member())) {
				categories.add(cat);
			}
		}

		Emote home = bot.getEmoteById("674261700366827539");

		EmbedBuilder index = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/all_commands"))
				.appendDescription(locale.get("str/category_counter", categories.size()) + "\n")
				.appendDescription(locale.get("str/command_counter", categories.stream().map(Category::getCommands).mapToInt(Set::size).sum()));

		Map<Emoji, Page> pages = new LinkedHashMap<>();
		for (Category cat : categories) {
			index.addField(cat.getName(locale), cat.getDescription(locale), true);
		}

		if (home != null) {
			index.setThumbnail(home.getImageUrl());
			pages.put(Utils.parseEmoji(home), new InteractPage(index.build()));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		for (Category cat : categories) {
			int total = cat.getCommands().size();
			Emote emt = cat.getEmote();

			eb.setTitle(cat.getName(locale))
					.setThumbnail(emt.getImageUrl())
					.appendDescription(cat.getDescription(locale) + "\n\n")
					.appendDescription(locale.get("str/command_counter", total));

			AtomicInteger i = new AtomicInteger();
			pages.put(Utils.parseEmoji(emt), Utils.generatePage(eb, cat.getCommands(), 10, cmd -> {
				String[] parts = cmd.name().split("\\.");

				try {
					if (parts.length > 1) {
						if (i.get() + 1 == total) {
							return "└ `" + parts[1] + "`";
						} else {
							return "├ `" + parts[1] + "`";
						}
					}

					return "`" + cmd.name() + "`";
				} finally {
					i.getAndIncrement();
				}
			}));
		}

		event.channel().sendMessageEmbeds(index.build()).queue(s ->
				Pages.categorize(s, pages, true, 1, TimeUnit.MINUTES, u -> u.equals(event.user()))
		);
	}
}
