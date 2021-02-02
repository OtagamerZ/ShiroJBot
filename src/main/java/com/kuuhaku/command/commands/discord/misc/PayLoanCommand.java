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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "pagar",
		aliases = {"pay", "payloan"},
		category = Category.MISC
)
public class PayLoanCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (acc.getBalance() <= 0 && acc.getLoan() > 0) {
			channel.sendMessage("❌ | Você não têm créditos suficientes para reduzir sua dívida (não é possível pagar com créditos voláteis).").queue();
			return;
		} else if (acc.getLoan() == 0) {
			channel.sendMessage("❌ | Você não está devendo nada (ao menos não para mim :wink:).").queue();
			return;
		}

		long toPay = acc.debitLoan();
		AccountDAO.saveAccount(acc);

		channel.sendMessage("Você debitou " + Helper.separate(toPay) + " créditos da sua dívida.").queue();
	}
}
