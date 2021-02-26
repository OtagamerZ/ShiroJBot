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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.text.MessageFormat;

@Command(
		name = "carteira",
		aliases = {"banco", "bank", "money", "wallet", "atm"},
		usage = "req_text",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class WalletCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_balance-title"), author.getName()))
				.addField(
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_balance-field-title"), acc.getBalance()),
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_balance-loan-bugs"), acc.getVBalance(), acc.getLoan(), acc.getBugs())
						, true
				)
				.addField(
						ShiroInfo.getLocale(I18n.PT).getString("str_balance-last-voted"),
						acc.getLastVoted(),
						true
				)
				.setThumbnail("https://i.imgur.com/nhWckfq.png");

		channel.sendMessage(eb.build()).queue();
	}
}
