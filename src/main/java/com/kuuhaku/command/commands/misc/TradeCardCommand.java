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
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
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
		} else if (args.length < 4) {
			channel.sendMessage(":x: | Você precisa mencionar uma quantia de créditos ou uma carta, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada) para realizar a troca.").queue();
			return;
		}

		User other = message.getMentionedUsers().get(0);

		if (StringUtils.isNumeric(args[1])) {
			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());

			int price = Integer.parseInt(args[1]);
			KawaiponCard tc = CardDAO.getCard(target, args[2], args[3].equalsIgnoreCase("C"));

			Account acc = AccountDAO.getAccount(author.getId());
			Account tacc = AccountDAO.getAccount(other.getId());

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

			int min = (5 - tc.getCard().getRarity().getIndex()) * 125 * (tc.isFoil() ? 2 : 1);

			if (price < min) {
				channel.sendMessage(":x: | Você não pode oferecer menos que " + min + " créditos por essa carta.").queue();
				return;
			}

			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja comprar sua carta `" + (tc.isFoil() ? "✦ " : "") + tc.getCard().getName() + (tc.isFoil() ? " ✦" : "") + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (member1, message1) -> {
						if (!member1.getId().equals(other.getId())) return;
						acc.removeCredit(price);
						target.removeCard(tc);
						kp.addCard(tc.getCard(), tc.isFoil());
						tacc.addCredit(price);

						KawaiponDAO.saveKawaipon(kp);
						KawaiponDAO.saveKawaipon(target);
						AccountDAO.saveAccount(acc);
						AccountDAO.saveAccount(tacc);

						s.delete().flatMap(n -> channel.sendMessage("Troca concluída com sucesso!")).queue();
					}), false));
		} else {
			if (args.length < 5) {
				channel.sendMessage(":x: | Você precisa mencionar uma carta que quer oferecer, o tipo, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada) para realizar a troca.").queue();
				return;
			}

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());

			KawaiponCard c = CardDAO.getCard(kp, args[1], args[2].equalsIgnoreCase("C"));
			KawaiponCard tc = CardDAO.getCard(target, args[3], args[4].equalsIgnoreCase("C"));

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

			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja trocar a carta `" + (c.isFoil() ? "✦ " : "") + c.getCard().getName() + (c.isFoil() ? " ✦" : "") + "` pela sua carta `" + (tc.isFoil() ? "✦ " : "") + tc.getCard().getName() + (tc.isFoil() ? " ✦" : "") + "`, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (member1, message1) -> {
						if (!member1.getId().equals(other.getId())) return;
						kp.removeCard(c);
						target.removeCard(tc);
						kp.addCard(tc.getCard(), tc.isFoil());
						target.addCard(c.getCard(), c.isFoil());

						KawaiponDAO.saveKawaipon(kp);
						KawaiponDAO.saveKawaipon(target);

						s.delete().flatMap(n -> channel.sendMessage("Troca concluída com sucesso!")).queue();
					}), false, 1, TimeUnit.MINUTES));
		}
	}
}
