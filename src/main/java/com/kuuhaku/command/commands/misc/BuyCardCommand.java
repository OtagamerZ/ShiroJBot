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
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardMarketDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BuyCardCommand extends Command {

	public BuyCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BuyCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BuyCardCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BuyCardCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account buyer = AccountDAO.getAccount(author.getId());
		if (args.length < 1) {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setTitle(":scales: | Mercado de cartas");
			eb.setDescription("User `" + prefix + "comprar ID` para comprar uma carta.");
			eb.setFooter("Seus créditos: " + buyer.getBalance(), "https://i.imgur.com/U0nPjLx.gif");

			List<Page> pages = new ArrayList<>();
			List<CardMarket> cards = CardMarketDAO.getCards();
			cards.sort(Comparator
					.<CardMarket, KawaiponRarity>comparing(k -> k.getCard().getCard().getRarity(), Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
					.thenComparing(k -> k.getCard().getCard().getAnime(), Comparator.comparing(AnimeName::toString, String.CASE_INSENSITIVE_ORDER))
					.thenComparing(k -> k.getCard().isFoil())
					.thenComparing(k -> k.getCard().getCard().getName(), String.CASE_INSENSITIVE_ORDER));
			int totalPages = (int) Math.ceil(cards.size() / 10f);
			for (int i = 0; i < totalPages; i++) {
				eb.clearFields();
				eb.setColor(Helper.getRandomColor());
				for (int p = i * 10; p < cards.size() && p < 10; p++) {
					CardMarket cm = cards.get(i);
					User seller = Main.getInfo().getUserByID(cm.getSeller());
					eb.addField(
							"ID: " + cm.getId() + " | " + cm.getCard().getName() + " (" + cm.getCard().getCard().getRarity().toString() + ")",
							"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: **" + cm.getPrice() + "** créditos",
							false
					);
				}
				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			if (pages.size() == 0) {
				channel.sendMessage("Ainda não há nenhuma carta anunciada.").queue();
			} else
				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5));
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(":x: | O ID precisa ser um valor inteiro.").queue();
			return;
		}

		CardMarket cm = CardMarketDAO.getCard(Integer.parseInt(args[0]));

		if (cm == null) {
			channel.sendMessage(":x: | ID inválido ou a carta já foi comprada por alguém.").queue();
			return;
		}

		Account seller = AccountDAO.getAccount(cm.getSeller());

		if (buyer.getBalance() < cm.getPrice()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (kp.getCards().contains(cm.getCard())) {
			channel.sendMessage(":x: | Parece que você já possui essa carta!").queue();
			return;
		}

		kp.addCard(cm.getCard());
		KawaiponDAO.saveKawaipon(kp);

		seller.addCredit(cm.getPrice());
		buyer.removeCredit(cm.getPrice());

		AccountDAO.saveAccount(seller);
		AccountDAO.saveAccount(buyer);

		cm.setBuyer(author.getId());
		CardMarketDAO.saveCard(cm);

		channel.sendMessage(":white_check_mark: | Carta comprada com sucesso!").queue();
	}
}