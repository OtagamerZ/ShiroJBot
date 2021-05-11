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
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "leave",
		usage = "req_id",
		category = Category.DEV
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class LeaveCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		List<String[]> servers = new ArrayList<>();
		for (Guild g : Main.getShiroShards().getGuilds()) {
			servers.add(new String[]{g.getName(), g.getId(), String.valueOf(g.getMembers().stream().filter(m -> !m.getUser().isBot()).count())});
		}
		List<List<String[]>> svPages = Helper.chunkify(servers, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < svPages.size(); i++) {
			eb.clear();

			eb.setTitle("Servidores que eu participo:");
			for (String[] p : svPages.get(i)) {
				eb.addField("Nome: " + p[0], "ID: " + p[1] + "\nMembros: " + p[2], false);
			}
			eb.setFooter("Página " + (i + 1) + " de " + svPages.size() + ". Total de " + svPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		try {
			Guild guildToLeave = Main.getInfo().getGuildByID(args[0]);
			guildToLeave.leave().queue();
			channel.sendMessage("Ok, acabei de sair desse servidor!").queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("Escolha o servidor que devo sair!\n").embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(m, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
		} catch (NullPointerException ex) {
			channel.sendMessage(I18n.getString("err_invalid-server")).embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(m, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
		}
	}

}
