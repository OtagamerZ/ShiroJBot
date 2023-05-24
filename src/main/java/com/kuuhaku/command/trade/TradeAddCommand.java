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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.Trade;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.List;

@Command(
		name = "trade",
		path = {"add", "test"},
		category = Category.MISC
)
@Signature({
		"<value:number:r>",
		"card <card:word:r>",
		"item <item:word:r>"
})
public class TradeAddCommand implements Executable {
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

		if (args.has("value")) {
			addCurrency(locale, event, args, trade);
		} else {
			addCard(locale, event, args, trade);
		}
	}

	private static void addCard(I18N locale, MessageData.Guild event, JSONObject args, Trade trade) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStash().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card WHERE rarity NOT IN ('ULTIMATE', 'NONE')");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		Utils.selectOption(locale, event.channel(), kp.getStash(), card, event.user())
				.thenAccept(sc -> {
					if (sc == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					}

					trade.getSelfOffers(event.user().getId()).add(sc.getId());
					event.channel().sendMessage(locale.get("success/offer_add", event.user().getAsMention(), sc)).queue();
				})
				.exceptionally(t -> {
					if (!(t.getCause() instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}

	private static void addCurrency(I18N locale, MessageData.Guild event, JSONObject args, Trade trade) {
		int offer = args.getInt("value");
		if (offer < 1) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 1)).queue();
			return;
		} else if (!Utils.between(offer + trade.getSelfValue(event.user().getId()), 0, 10_000_000)) {
			event.channel().sendMessage(locale.get("error/invalid_value_range", 0, 10_000_000)).queue();
			return;
		}

		Account acc = trade.getSelf(event.user().getId());
		if (!acc.hasEnough(offer, Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		}

		trade.addSelfValue(event.user().getId(), offer);
		event.channel().sendMessage(locale.get("success/offer_add", event.user().getAsMention(), Utils.separate(offer) + " ₵R")).queue();
	}
}
