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
import com.kuuhaku.utils.VipItem;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class VIPShopCommand extends Command {

	public VIPShopCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public VIPShopCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public VIPShopCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public VIPShopCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length == 0) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle(":diamonds: | Loja VIP");
			eb.setDescription("Gemas podem ser obtidos ao resgatar um acúmulo de 7 votos seguidos com o comando `" + prefix + "resgatar`. Para utilizar as gemas basta usar `" + prefix + "vip ID`!\n\n" +
					"Muito obrigada por me apoiar!"
			);
			for (VipItem v : VipItem.values()) eb.addField(v.getField());
			eb.setColor(Color.red);
			eb.setFooter("Suas gemas: " + acc.getGems(), "https://bgasparotto.com/wp-content/uploads/2016/03/ruby-logo.png");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id-value")).queue();
			return;
		}

		VipItem vi = VipItem.getById(Integer.parseInt(args[0]));
		if (vi == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id")).queue();
			return;
		}

		if (vi.name().startsWith("CARD_")) {
			if (args.length < 2) {
				channel.sendMessage(":x: | Você precisa informar uma carta.").queue();
				return;
			}

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Card c = CardDAO.getCard(args[1], false);

			if (c == null) {
				channel.sendMessage(":x: | Essa carta não existe.").queue();
				return;
			}

			if (vi.equals(VipItem.CARD_ROLL)) {
				if (acc.getGems() < vi.getGems()) {
					channel.sendMessage(":x: | Você não possui gemas suficientes.").queue();
					return;
				} else if (args.length < 3) {
					channel.sendMessage(":x: | Você precisa informar uma carta e o tipo (`N` = normal, `C` = cromada).").queue();
					return;
				} else if (!Helper.equalsAny(args[2], "N", "C")) {
					channel.sendMessage(":x: | Você precisa informar o tipo da carta que deseja rodar (`N` = normal, `C` = cromada).").queue();
					return;
				}
				KawaiponCard card = kp.getCard(c, args[2].equalsIgnoreCase("C"));
				KawaiponCard oldCard = new KawaiponCard(c, args[2].equalsIgnoreCase("C"));

				if (card == null) {
					channel.sendMessage(":x: | Você não pode rodar uma carta que não possui!").queue();
					return;
				}

				List<Card> cards = CardDAO.getCards().stream().filter(cd -> kp.getCard(cd, args[1].equalsIgnoreCase("C")) == null).collect(Collectors.toList());
				Card chosen = cards.get(Helper.rng(cards.size(), true));

				kp.removeCard(card);
				card.setCard(chosen);
				kp.addCard(card);

				KawaiponDAO.saveKawaipon(kp);
				acc.removeGem(1);
				AccountDAO.saveAccount(acc);

				channel.sendMessage("Você rodou a carta " + oldCard.getName() + " com sucesso e conseguiu....**" + card.getName() + " (" + card.getCard().getRarity().toString() + ")**!").queue();
				return;
			} else if (vi.equals(VipItem.CARD_FOIL)) {
				if (acc.getGems() < vi.getGems()) {
					channel.sendMessage(":x: | Você não possui gemas suficientes.").queue();
					return;
				}
				KawaiponCard card = kp.getCard(c, false);
				KawaiponCard oldCard = new KawaiponCard(c, false);

				if (card == null) {
					channel.sendMessage(":x: | Você não pode cromar uma carta que não possui!").queue();
					return;
				}

				kp.removeCard(card);
				card.setFoil(true);
				kp.addCard(card);

				KawaiponDAO.saveKawaipon(kp);
				acc.removeGem(5);
				AccountDAO.saveAccount(acc);

				channel.sendMessage("Você cromou a carta " + oldCard.getName() + " com sucesso!").queue();
				return;
			}
		}
	}
}
