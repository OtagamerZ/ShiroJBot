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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "carteira",
		aliases = {"banco", "bank", "money", "wallet", "atm", "bal", "balance"},
		usage = "req_text",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
@SlashGroup("meus")
@SlashCommand(name = "creditos")
public class WalletCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		int prcnt = Helper.prcntToInt(acc.getSpent(), acc.getBalance() + acc.getSpent());
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Saldo de " + author.getName())
				.addField(
						":moneybag: | Saldo: %s%s".formatted(Helper.separate(acc.getBalance()), acc.getBalance() > 100_000 && prcnt < 10 ? " (" + prcnt + "%)" : ""),
						"""
								:money_with_wings: | Volátil: %s
								:diamonds: | Gemas: %s
								""".formatted(
								Helper.separate(acc.getVBalance()),
								Helper.separate(acc.getGems())
						)
						, true
				)
				.addField(
						"Ultimo voto em:",
						acc.getLastVoted() == null ? "Nunca" : Helper.fullDateFormat.format(acc.getLastVoted()),
						true
				)
				.setThumbnail("https://i.imgur.com/nhWckfq.png");

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
