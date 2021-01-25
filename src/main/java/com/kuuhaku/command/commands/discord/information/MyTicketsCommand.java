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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Lottery;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MyTicketsCommand extends Command {

	public MyTicketsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MyTicketsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MyTicketsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MyTicketsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<Lottery> l = LotteryDAO.getLotteriesByUser(author.getId());

		if (l.size() == 0) {
			channel.sendMessage("❌ | Você não comprou nenhum bilhete ainda.").queue();
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < l.size(); i++) {
			sb.append("**%s -** `%s`".formatted(
					i + 1,
					Arrays.stream(l.get(i).getDozens().split(","))
							.sorted(Comparator.comparingInt(Integer::parseInt))
							.collect(Collectors.joining(","))
			));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(":tickets: | Seus bilhetes da loteria")
				.setDescription(sb.toString())
				.setFooter("Prêmio atual: %s créditos".formatted(Helper.separate(LotteryDAO.getLotteryValue().getValue())));

		channel.sendMessage(eb.build()).queue();
	}
}