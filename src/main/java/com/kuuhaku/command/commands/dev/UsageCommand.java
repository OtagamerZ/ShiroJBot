/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.dev;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.LogDAO;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UsageCommand extends Command {

	public UsageCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public UsageCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public UsageCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public UsageCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		List<Object[]> usos = LogDAO.getUsage();
		List<List<Object[]>> uPages = Helper.chunkify(usos, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < uPages.size(); i++) {
			eb.clear();

			eb.setTitle("Quantidade de comandos usados por servidor:");
			uPages.get(i).forEach(p -> eb.addField("Servidor: " + p[0], "Comandos usados: " + p[1], false));
			eb.setFooter("Página " + (i + 1) + " de " + uPages.size() + ". Total de " + uPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
	}
}
