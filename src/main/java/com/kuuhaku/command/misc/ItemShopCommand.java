/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.SignatureParser;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.Map;

@Command(
		name = "items",
		subname = "buy",
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

			List<Page> pages = Utils.generatePages(eb, DAO.findAll(UserItem.class).stream().sorted().toList(), 20, 5,
					i -> {
						int has = items.getOrDefault(i, 0);

						String out = i.toString(locale);
						if (i.getPrice() > 0 && i.getCurrency() != null) {
							out += "\n" + locale.get("str/price", locale.get("currency/" + i.getCurrency(), i.getPrice()));
						}

						if (i.getStackSize() > 0) {
							out += "\n" + locale.get("str/item_has", has + "/" + i.getStackSize());
						} else {
							out += "\n" + locale.get("str/item_has", has);
						}

						if (i.isPassive()) {
							out += " | **" + locale.get("str/passive") + "**";
						}

						out += "\n" + i.getDescription(locale);

						if (i.getSignature() != null) {
							String sig = SignatureParser.extract(locale, new String[]{i.getSignature()}, false).get(0);

							Command cmd = getClass().getDeclaredAnnotation(Command.class);
							out += "\n" + locale.get("str/params",
									sig.formatted(data.config().getPrefix(), cmd.name() + "." + cmd.subname() + " " + i.getId())
							);
						}

						return out + "\n";
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

		if (item == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM user_item");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("id").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_item", sug.getFirst())).queue();
			return;
		} else if (item.getStackSize() > 0 && items.getOrDefault(item, 0) + amount > item.getStackSize()) {
			event.channel().sendMessage(locale.get("error/stack_full")).queue();
			return;
		} else if (!acc.hasEnough(item.getPrice(), item.getCurrency())) {
			event.channel().sendMessage(locale.get("error/insufficient_" + item.getCurrency())).queue();
			return;
		} else if (amount <= 0) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 0)).queue();
			return;
		}

		try {
			int value = amount * item.getPrice();
			String price = locale.get("currency/" + item.getCurrency(), value);
			Utils.confirm(locale.get("question/item_buy", amount, item.getName(locale), price), event.channel(), w -> {
						acc.addItem(item, amount);
						if (item.getCurrency() == Currency.CR) {
							acc.consumeCR(value, "Bought " + amount + "x" + item.getName(locale));
						} else {
							acc.consumeGems(value, "Bought " + amount + "x" + item.getName(locale));
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
