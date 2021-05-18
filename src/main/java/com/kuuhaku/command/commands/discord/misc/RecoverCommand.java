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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.StockMarketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.StockValue;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.StockMarket;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "retirar",
		aliases = {"recover"},
		usage = "req_card-value",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class RecoverCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar uma carta e um valor para recuperar.").queue();
			return;
		}

		Card c = CardDAO.getRawCard(args[0]);
		if (c == null) {
			channel.sendMessage("❌ | Essa carta não existe.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		StockMarket sm = StockMarketDAO.getCardInvestment(author.getId(), c);

		try {
			double amount = Helper.round(Double.parseDouble(args[1]), 3);
			if (sm.getInvestment() < amount) {
				channel.sendMessage("❌ | Você não tem ações suficientes.").queue();
				return;
			}

			StockValue sv = StockMarketDAO.getValues().get(c.getId());
			int readjust = (int) Math.round(sv.getValue() * amount);

			sm.setInvestment(sm.getInvestment() - amount);

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prestes vender " + Helper.separate(amount) + " ações (" + Helper.separate(readjust) + " créditos) da carta " + c.getName() + ", deseja confirmar?")
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								acc.addCredit(readjust, this.getClass());
								acc.addProfit(readjust);
								AccountDAO.saveAccount(acc);

								StockMarketDAO.saveInvestment(sm);

								s.delete().flatMap(d -> channel.sendMessage("✅ | Ações vendidas com sucesso!")).queue();
							}), true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
					));
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
		}
	}
}
