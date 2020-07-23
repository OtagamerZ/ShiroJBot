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
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.Map;
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
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage(":x: | Você não pode trocar cartas com você mesmo.").queue();
			return;
		} else if (args.length < 4) {
			channel.sendMessage(":x: | Você precisa mencionar uma quantia de créditos ou uma carta, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada) para realizar a troca.").queue();
			return;
		}

		User other = message.getMentionedUsers().get(0);

		if (StringUtils.isNumeric(args[1])) {
			int price = Integer.parseInt(args[1]);
			Card tc = CardDAO.getCard(args[2], false);
			boolean foil = args[3].equalsIgnoreCase("C");

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
			}

			KawaiponCard card = new KawaiponCard(tc, foil);

			if (kp.getCards().contains(card)) {
				channel.sendMessage(":x: | Parece que você já possui essa carta!").queue();
				return;
			} else if (!target.getCards().contains(card)) {
				channel.sendMessage(":x: | Ele/ela não possui essa carta!").queue();
				return;
			}

			int min = tc.getRarity().getIndex() * 150 * (foil ? 2 : 1);

			if (price < min) {
				channel.sendMessage(":x: | Você não pode oferecer menos que " + min + " créditos por essa carta.").queue();
				return;
			}

			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja comprar sua carta `" + card.getName() + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (member1, message1) -> {
						if (!member1.getId().equals(other.getId())) return;
						acc.removeCredit(price, this.getClass());
						target.removeCard(card);
						kp.addCard(card);
						tacc.addCredit(price, this.getClass());

						KawaiponDAO.saveKawaipon(kp);
						KawaiponDAO.saveKawaipon(target);
						AccountDAO.saveAccount(acc);
						AccountDAO.saveAccount(tacc);

						s.delete().flatMap(n -> channel.sendMessage("Troca concluída com sucesso!")).queue(null, Helper::doNothing);
					}), true, 1, TimeUnit.MINUTES));
		} else {
			if (args.length < 5) {
				channel.sendMessage(":x: | Você precisa mencionar uma carta, o tipo, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada) para realizar a troca.").queue();
				return;
			} else if (!Helper.equalsAny(args[2], "N", "C")) {
				channel.sendMessage(":x: | Você precisa informar o tipo da carta que deseja oferecer (`N` = normal, `C` = cromada).").queue();
				return;
			} else if (!Helper.equalsAny(args[4], "N", "C")) {
				channel.sendMessage(":x: | Você precisa informar o tipo da carta que deseja obter (`N` = normal, `C` = cromada).").queue();
				return;
			}

			Card c = CardDAO.getCard(args[1], false);
			Card tc = CardDAO.getCard(args[3], false);
			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());
			boolean yourFoil = args[2].equalsIgnoreCase("C");
			boolean hisFoil = args[4].equalsIgnoreCase("C");


			if (c == null || tc == null) {
				channel.sendMessage(":x: | Essa carta não existe.").queue();
				return;
			}

			KawaiponCard yourCard = new KawaiponCard(c, yourFoil);
			KawaiponCard hisCard = new KawaiponCard(tc, hisFoil);

			if (!kp.getCards().contains(yourCard)) {
				channel.sendMessage(":x: | Você não pode trocar uma carta que não possui!").queue();
				return;
			} else if (target.getCards().contains(yourCard)) {
				channel.sendMessage(":x: | Eu acho que ele já possui essa carta!").queue();
				return;
			} else if (kp.getCards().contains(hisCard)) {
				channel.sendMessage(":x: | Parece que você já possui essa carta!").queue();
				return;
			} else if (!target.getCards().contains(hisCard)) {
				channel.sendMessage(":x: | Ele/ela não possui essa carta!").queue();
				return;
			}

			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja trocar a carta `" + yourCard.getName() + "` pela sua carta `" + hisCard.getName() + "`, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (member1, message1) -> {
						if (!member1.getId().equals(other.getId())) return;
						kp.removeCard(yourCard);
						target.removeCard(hisCard);
						kp.addCard(hisCard);
						target.addCard(yourCard);

						KawaiponDAO.saveKawaipon(kp);
						KawaiponDAO.saveKawaipon(target);

						s.delete().flatMap(n -> channel.sendMessage("Troca concluída com sucesso!")).queue(null, Helper::doNothing);
					}), true, 1, TimeUnit.MINUTES));
		}
	}
}
