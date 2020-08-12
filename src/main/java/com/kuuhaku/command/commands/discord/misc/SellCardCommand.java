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
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.CardMarketDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SellCardCommand extends Command {

	public SellCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SellCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SellCardCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SellCardCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 3) {
			channel.sendMessage(":x: | Você precisa informar uma carta, o tipo (`N` = normal, `C` = cromada) e o preço dela.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[2])) {
			channel.sendMessage(":x: | O preço precisa ser um valor inteiro.").queue();
			return;
		} else if (!Helper.equalsAny(args[1], "N", "C")) {
			channel.sendMessage(":x: | Você precisa informar o tipo da carta que deseja vender (`N` = normal, `C` = cromada).").queue();
			return;
		}

		Card c = CardDAO.getCard(args[0], false);
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		boolean foil = args[1].equalsIgnoreCase("C");

		if (c == null) {
			channel.sendMessage(":x: | Essa carta não existe.").queue();
			return;
		}

		KawaiponCard card = kp.getCard(c, foil);

		if (card == null) {
			channel.sendMessage(":x: | Você não pode trocar uma carta que não possui!").queue();
			return;
		}

		int price = Integer.parseInt(args[2]);
		int min = c.getRarity().getIndex() * 150 * (foil ? 2 : 1);

		if (price < min) {
			channel.sendMessage(":x: | Você não pode vender essa carta por menos que " + min + " créditos.").queue();
			return;
		}

		channel.sendMessage("Esta carta sairá da sua coleção, você ainda poderá comprá-la novamente pelo mesmo preço. Deseja mesmo anunciá-la?").queue(s -> {
			Pages.buttonize(s, Map.of(Helper.ACCEPT, (member1, message1) -> {
				if (member1.getId().equals(author.getId())) {
					kp.removeCard(card);
					KawaiponDAO.saveKawaipon(kp);

					CardMarket cm = new CardMarket(author.getId(), card, price);
					CardMarketDAO.saveCard(cm);

					s.delete().flatMap(d -> channel.sendMessage(":white_check_mark: | Carta anunciada com sucesso!")).queue();
				}
			}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()));
		});
	}
}