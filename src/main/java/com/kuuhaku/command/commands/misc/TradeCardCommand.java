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

package com.kuuhaku.command.commands.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class TradeCardCommand extends Command {

	public TradeCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TradeCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TradeCardCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TradeCardCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (args.length < 3) {
			channel.sendMessage(":x: | Você precisa mencionar uma quantia de créditos ou uma carta para realizar a troca e qual carta você deseja.").queue();
			return;
		}

		User other = message.getMentionedUsers().get(0);

		if (StringUtils.isNumeric(args[1])) {
			int price = Integer.parseInt(args[1]);
			Card tc = CardDAO.getCard(args[2]);
			Account acc = AccountDAO.getAccount(author.getId());
			Account tacc = AccountDAO.getAccount(other.getId());

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());

			if (tc == null) {
				channel.sendMessage(":x: | Essa carta não existe.").queue();
				return;
			} else if (acc.getBalance() < price) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
				return;
			} else if (kp.getCards().contains(tc)) {
				channel.sendMessage(":x: | Parece que você já possui essa carta!").queue();
				return;
			} else if (!target.getCards().contains(tc)) {
				channel.sendMessage(":x: | Ele/ela não possui essa carta!").queue();
				return;
			}

			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja comprar sua carta `" + tc.getName() + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (member1, message1) -> {
						acc.removeCredit(price);
						target.removeCard(tc);
						kp.addCard(tc);
						tacc.addCredit(price);

						KawaiponDAO.saveKawaipon(kp);
						KawaiponDAO.saveKawaipon(target);
						AccountDAO.saveAccount(acc);
						AccountDAO.saveAccount(tacc);

						s.delete().flatMap(n -> channel.sendMessage("Troca concluída com sucesso!")).queue();
					}), false));
		} else {
			Card c = CardDAO.getCard(args[1]);
			Card tc = CardDAO.getCard(args[2]);
			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());

			if (c == null || tc == null) {
				channel.sendMessage(":x: | Essa carta não existe.").queue();
				return;
			} else if (!kp.getCards().contains(c)) {
				channel.sendMessage(":x: | Você não pode trocar uma carta que não possui!").queue();
				return;
			} else if (target.getCards().contains(c)) {
				channel.sendMessage(":x: | Eu acho que ele já possui essa carta!").queue();
				return;
			} else if (kp.getCards().contains(tc)) {
				channel.sendMessage(":x: | Parece que você já possui essa carta!").queue();
				return;
			} else if (!target.getCards().contains(tc)) {
				channel.sendMessage(":x: | Ele/ela não possui essa carta!").queue();
				return;
			}

			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja trocar a carta `" + c.getName() + "` pela sua carta `" + tc.getName() + "`, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (member1, message1) -> {
						kp.removeCard(c);
						target.removeCard(tc);
						kp.addCard(tc);
						target.addCard(c);

						KawaiponDAO.saveKawaipon(kp);
						KawaiponDAO.saveKawaipon(target);

						s.delete().flatMap(n -> channel.sendMessage("Troca concluída com sucesso!")).queue();
					}), false, 1, TimeUnit.MINUTES));
		}
	}
}