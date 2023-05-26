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

package com.kuuhaku.command.trade;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.Trade;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Command(
		name = "trade",
		path = {"add", "item"},
		category = Category.MISC
)
@Signature("<item:word:r> <amount:number>")
public class TradeAddItemCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Trade trade = Trade.getPending().get(event.user().getId());
		if (trade == null) {
			event.channel().sendMessage(locale.get("error/not_in_trade")).queue();
			return;
		} else if (trade.isFinalizing()) {
			event.channel().sendMessage(locale.get("error/trade_finalizing")).queue();
			return;
		}

		Account acc = trade.getSelf(event.user().getId());
		if (acc.getItems().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_inventory")).queue();
			return;
		}

		Map<UserItem, Integer> items = acc.getItems();
		UserItem item = items.keySet().stream()
				.filter(i -> i.getId().equals(args.getString("id").toUpperCase()))
				.findFirst().orElse(null);

		if (item == null) {
			List<String> names = items.keySet().stream().map(UserItem::getId).toList();

			Pair<String, Double> sug = Utils.didYouMean(args.getString("id").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/item_not_found", sug.getFirst())).queue();
			return;
		} else if (!items.containsKey(item)) {
			event.channel().sendMessage(locale.get("error/item_not_have")).queue();
			return;
		} else if (item.isAccountBound()) {
			event.channel().sendMessage(locale.get("error/item_account_bound")).queue();
			return;
		}

		int amount = args.getInt("amount", 1);
		trade.getSelfItems(event.user().getId()).addAll(Collections.nCopies(amount, item.getId()));
		event.channel().sendMessage(locale.get("success/offer_add", event.user().getAsMention(), amount + "x " + item.toString(locale))).queue();
	}
}
