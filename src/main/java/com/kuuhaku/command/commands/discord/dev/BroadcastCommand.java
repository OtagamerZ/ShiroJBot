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

package com.kuuhaku.command.commands.discord.dev;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Tags;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "transmitir",
		aliases = {"broadcast", "bc"},
		usage = "req_type-message",
		category = Category.DEV
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class BroadcastCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_broadcast-no-type")).queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(I18n.getString("err_broadcast-no-message")).queue();
			return;
		}

		Map<String, Boolean> result = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "geral" -> Helper.broadcast(argsAsText.replaceFirst("geral", "").trim(), channel, author);
			case "beta" -> {
				List<Tags> ps = TagDAO.getAllBetas();
				List<List<Tags>> psPages = Helper.chunkify(ps, 10);
				for (List<Tags> p : psPages) {
					result.clear();
					eb.clear();
					sb.setLength(0);

					for (Tags t : p) {
						User u = Helper.getOr(Main.getInfo().getUserByID(t.getUid()), null);

						if (u == null) {
							result.put("Desconhecido (" + t.getUid() + ")", false);
						} else {
							try {
								u.openPrivateChannel().complete().sendMessage(argsAsText.replaceFirst("beta", "").trim()).complete();
								result.put(u.getAsTag(), true);
							} catch (ErrorResponseException e) {
								result.put(u.getAsTag(), false);
							}
						}
					}

					sb.append("```diff\n");
					for (Map.Entry<String, Boolean> entry : result.entrySet()) {
						String key = entry.getKey();
						Boolean value = entry.getValue();
						sb.append(value ? "+ " : "- ").append(key).append("\n");
					}
					sb.append("```");

					eb.setTitle("__**STATUS**__ ");
					eb.setDescription(sb.toString());
					pages.add(new Page(PageType.EMBED, eb.build()));
				}
				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
			}
			default -> channel.sendMessage(I18n.getString("err_broadcast-invalid-type")).queue();
		}
	}
}
