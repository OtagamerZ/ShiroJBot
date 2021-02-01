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
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.StockMarketDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.StockMarket;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ActionsCommand implements Executable {

	public ActionsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ActionsCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ActionsCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ActionsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();
		List<StockMarket> act = StockMarketDAO.getInvestments(author.getId());

		if (act.size() == 0) {
			channel.sendMessage("❌ | Você não possui nenhuma ação.").queue();
			return;
		}

		List<List<StockMarket>> actions = Helper.chunkify(act, 10);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(":chart_with_upwards_trend: | Ações de " + author.getName());

		StringBuilder sb = new StringBuilder();
		for (List<StockMarket> chunk : actions) {
			sb.setLength(0);

			for (StockMarket sm : chunk)
				sb.append("%s - %s ações\n".formatted(sm.getCard().getName(), sm.getInvestment()));

			eb.setDescription(sb.toString());
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
