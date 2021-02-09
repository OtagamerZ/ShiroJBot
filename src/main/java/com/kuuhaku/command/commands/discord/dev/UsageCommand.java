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
import com.kuuhaku.controller.postgresql.LogDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "usos",
		aliases = {"uses", "usage"},
		category = Category.DEV
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class UsageCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		List<Object[]> usos = LogDAO.getUsage();
		List<List<Object[]>> uPages = Helper.chunkify(usos, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < uPages.size(); i++) {
			eb.clear();

			eb.setTitle("Quantidade de comandos usados por servidor neste mês:");
			for (Object[] p : uPages.get(i)) {
				Guild g = Main.getInfo().getGuildByID(String.valueOf(p[0]));
				if (g == null) continue;
				eb.addField("Servidor: " + g.getName(), """
						Dono: %s (%s)
						Comandos usados: %s
						Último uso: %s
						"""
						.formatted(
								g.getOwner() == null ? "`Desconhecido`" : g.getOwner().getUser().getName(),
								g.getOwner() == null ? "`??`" : g.getOwnerId(),
								p[1],
								p[2]
						), false);
			}
			eb.setFooter("Página " + (i + 1) + " de " + uPages.size() + ". Total de " + uPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
	}
}
