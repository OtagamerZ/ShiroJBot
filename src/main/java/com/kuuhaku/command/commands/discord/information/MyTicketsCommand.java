/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.command.Slashed;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Lottery;
import com.kuuhaku.model.persistent.LotteryValue;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "bilhetes",
		aliases = {"tickets"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
@SlashGroup("meus")
@SlashCommand(name = "bilhetes")
public class MyTicketsCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Lottery> l = Lottery.queryAll(Lottery.class, "SELECT l FROM Lottery l WHERE l.valid = TRUE AND l.uid = :uid", author.getId());

		if (l.isEmpty()) {
			channel.sendMessage("❌ | Você não comprou nenhum bilhete ainda.").queue();
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < l.size(); i++) {
			sb.append("**%s -** `%s`\n".formatted(
					i + 1,
					Arrays.stream(l.get(i).getDozens().split(","))
							.sorted(Comparator.comparingInt(Integer::parseInt))
							.collect(Collectors.joining("-"))
			));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(":tickets: | Seus bilhetes da loteria")
				.setDescription(sb.toString())
				.setFooter("Prêmio atual: %s CR".formatted(StringHelper.separate(LotteryValue.find(LotteryValue.class, 0).getValue())));

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
