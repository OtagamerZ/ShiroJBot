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
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "investir",
		aliases = {"invest"},
		usage = "req_card-value",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class InvestCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar uma carta e um valor para investir.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
			return;
		}

		Card c = CardDAO.getRawCard(args[0]);
		if (c == null) {
			channel.sendMessage("❌ | Essa carta não existe.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		StockMarket sm = StockMarketDAO.getCardInvestment(author.getId(), c);

		int amount = Integer.parseInt(args[1]);
		if (acc.getBalance() < amount) {
			channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
			return;
		} else if (sm.getInvestment() + amount >= 1000) {
			channel.sendMessage("❌ | O limite máximo por carta é 100.000 ações.").queue();
			return;
		}

		StockValue sv = StockMarketDAO.getValues().get(c.getId());
		double readjust = Helper.round(amount / (double) sv.getValue(), 3);

		sm.setInvestment(sm.getInvestment() + readjust);

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Você está prestes comprar " + readjust + " ações (" + Helper.separate(amount) + " créditos) da carta " + c.getName() + ", deseja confirmar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());
							acc.removeCredit(amount, this.getClass());
							acc.removeProfit(amount);
							AccountDAO.saveAccount(acc);

							StockMarketDAO.saveInvestment(sm);

							s.delete().flatMap(d -> channel.sendMessage("✅ | Ações compradas com sucesso!")).queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}
}
