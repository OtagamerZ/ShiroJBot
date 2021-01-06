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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Tags;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BroadcastCommand extends Command {

	public BroadcastCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BroadcastCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BroadcastCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BroadcastCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_broadcast-no-type")).queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_broadcast-no-message")).queue();
			return;
		}

		String msg = String.join(" ", args).replace("\\n", "\n");
		Map<String, Boolean> result = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		switch (args[0].toLowerCase()) {
			case "geral" -> Helper.broadcast(msg, (TextChannel) channel, author);
			case "beta" -> {
				List<Tags> ps = TagDAO.getAllBetas();
				List<List<Tags>> psPages = Helper.chunkify(ps, 10);
				for (List<Tags> p : psPages) {
					result.clear();
					eb.clear();
					sb.setLength(0);

					for (Tags t : p) {
						User u = Helper.getOr(Main.getInfo().getUserByID(t.getId()), null);

						if (u == null) {
							result.put("Desconhecido (" + t.getId() + ")", false);
						} else {
							try {
								u.openPrivateChannel().complete().sendMessage(msg).complete();
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
			default -> channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_broadcast-invalid-type")).queue();
		}
	}
}
