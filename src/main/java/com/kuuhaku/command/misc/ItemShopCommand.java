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

package com.kuuhaku.command.misc;

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.Map;

@Command(
		name = "items",
		path = "buy",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<id:word:r> <amount:number>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class ItemShopCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Map<UserItem, Integer> items = acc.getItems();

		if (!args.has("id")) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/items_available"));

			List<UserItem> catalogue = DAO.findAll(UserItem.class).stream()
					.filter(i -> i.getCurrency() != null)
					.sorted()
					.toList();

			List<Page> pages = Utils.generatePages(eb, catalogue, 10, 5,
					i -> {
						int has = items.getOrDefault(i, 0);

						FieldMimic fm = new FieldMimic(i.getIcon() + " " + i.getName(locale), "");
						if (i.getPrice() > 0 && i.getCurrency() != null) {
							if (i.getCurrency() == Currency.ITEM) {
								fm.appendLine(locale.get("str/price", i.getPrice() + " " + i.getItemCost().getName(locale)));
							} else {
								fm.appendLine(locale.get("str/price", locale.get("currency/" + i.getCurrency(), i.getPrice())));
							}
						}

						if (i.getStackSize() > 0) {
							fm.appendLine(locale.get("str/item_has", has + "/" + i.getStackSize()));
						} else {
							fm.appendLine(locale.get("str/item_has", has));
						}

						if (i.isPassive()) {
							fm.append(" | **" + locale.get("str/passive") + "**");
						}

						if (i.isAccountBound()) {
							fm.append(" :lock:");
						}

						fm.appendLine(i.getDescription(locale));
						fm.appendLine("`%s%s`".formatted(data.config().getPrefix(), "items.buy " + i.getId()));

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

		UserItem item = DAO.find(UserItem.class, args.getString("id").toUpperCase());
		int amount = args.getInt("amount", 1);

		if (item == null || item.getCurrency() == null) {
			String sug = Utils.didYouMean(args.getString("id").toUpperCase(), "SELECT id FROM user_item WHERE currency IS NOT NULL");
			event.channel().sendMessage(locale.get("error/unknown_item", sug)).queue();
			return;
		} else if (item.getStackSize() > 0 && items.getOrDefault(item, 0) + amount > item.getStackSize()) {
			event.channel().sendMessage(locale.get("error/stack_full")).queue();
			return;
		} else if (!acc.hasEnough(amount * item.getPrice(), item.getCurrency(), item.getItemCostId())) {
			event.channel().sendMessage(locale.get("error/insufficient_" + item.getCurrency())).queue();
			return;
		} else if (amount <= 0) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 0)).queue();
			return;
		}

		try {
			int value = amount * item.getPrice();
			String price;
			if (item.getCurrency() == Currency.ITEM) {
				price = item.getPrice() + " " + item.getItemCost().getName(locale);
			} else {
				price = locale.get("currency/" + item.getCurrency(), value);
			}

			Utils.confirm(locale.get("question/item_buy", amount, item.getName(locale), price), event.channel(), w -> {
						acc.addItem(item, amount);
						switch (item.getCurrency()) {
							case CR -> acc.consumeCR(value, "Bought " + amount + "x " + item.getName(locale));
							case GEM -> acc.consumeGems(value, "Bought " + amount + "x " + item.getName(locale));
							case ITEM -> acc.consumeItem(item.getItemCostId(), value);
						}

						event.channel().sendMessage(locale.get("success/item_buy", amount, item.getName(locale))).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
