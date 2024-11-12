/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.dunhun;

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Consumable;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.Map;

@Command(
		name = "hero",
		path = "buy",
		category = Category.MISC,
		beta = true
)
@Syntax(allowEmpty = true, value = "<id:word:r> <amount:number>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class HeroShopCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero(locale);
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		Map<Consumable, Integer> items = h.getConsumables();

		if (!args.has("id")) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/items_available"));

			List<Consumable> catalogue = DAO.queryAll(Consumable.class, "SELECT c FROM Consumable c WHERE c.price IS NOT NULL ORDER BY c.id");

			List<Page> pages = Utils.generatePages(eb, catalogue, 10, 5,
					c -> {
						int has = items.getOrDefault(c, 0);

						FieldMimic fm = new FieldMimic(c.getName(locale), "");
						if (c.getPrice() > 0) {
							fm.appendLine(locale.get("str/price", locale.get("currency/cr", c.getPrice())));
						}

						fm.appendLine(locale.get("str/item_has", has));
						fm.appendLine(c.getDescription(locale));
						fm.appendLine("`%s%s`".formatted(data.config().getPrefix(), "hero.buy " + c.getId()));

						return fm.toString();
					},
					(p, t) -> eb.setFooter(acc.getBalanceFooter(locale) + "\n" + locale.get("str/page", p + 1, t))
			);

			if (pages.isEmpty()) {
				event.channel().sendMessage(locale.get("error/shop_empty")).queue();
				return;
			}

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		Consumable item = DAO.find(Consumable.class, args.getString("id").toUpperCase());
		int amount = args.getInt("amount", 1);

		if (item == null) {
			String sug = Utils.didYouMean(args.getString("id"), "SELECT id AS value FROM consumable WHERE price IS NOT NULL");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_consumable_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_consumable", sug)).queue();
			}
			return;
		} else if (items.size() + amount > 10) {
			event.channel().sendMessage(locale.get("error/consumables_full")).queue();
			return;
		} else if (!acc.hasEnough(amount * item.getPrice(), Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		} else if (amount <= 0) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 0)).queue();
			return;
		}

		try {
			int value = amount * item.getPrice();
			String price = locale.get("currency/cr", value);

			Utils.confirm(locale.get("question/item_buy", amount, item.getName(locale), price), event.channel(), w -> {
						if (!acc.isTrueState()) {
							event.channel().sendMessage(locale.get("error/account_state_changed", 0)).queue();
							return true;
						}

						Hero n = h.refresh();
						n.addConsumable(item, amount);
						n.save();

						acc.consumeCR(value, "Consumable " + amount + "x " + item.getName(locale));

						event.channel().sendMessage(locale.get("success/item_buy", amount, item.getName(locale))).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
