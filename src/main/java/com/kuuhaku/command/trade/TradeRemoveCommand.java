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

package com.kuuhaku.command.trade;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.*;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.List;
import java.util.Locale;

@Command(
		name = "trade",
		subname = "remove",
		category = Category.MISC
)
@Signature({
		"<value:number:r>",
		"<card:word:r>"
})
public class TradeRemoveCommand implements Executable {
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

		if (args.containsKey("value")) {
			int offer = args.getInt("value");
			if (offer < 0) {
				event.channel().sendMessage(locale.get("error/invalid_value_low", 0)).queue();
				return;
			} else if (offer > trade.getSelfValue(event.user().getId())) {
				event.channel().sendMessage(locale.get("error/offer_cr")).queue();
				return;
			}

			trade.addSelfValue(event.user().getId(), -offer);
			event.channel().sendMessage(locale.get("success/offer_remove", event.user().getAsMention(), offer + " ₵R")).queue();
		} else {
			List<Integer> selfOffer = trade.getSelfOffers(event.user().getId());
			List<StashedCard> stash = DAO.queryAll(StashedCard.class,
					"SELECT s FROM StashedCard s WHERE s.kawaipon.uid = ?1 AND s.deck IS NULL AND s.id IN ?2",
					event.user().getId(), selfOffer
			);
			if (stash.isEmpty()) {
				event.channel().sendMessage(locale.get("error/empty_offer")).queue();
				return;
			}

			Card card = DAO.find(Card.class, args.getString("card").toUpperCase(Locale.ROOT));
			if (card == null) {
				List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card");

				Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(Locale.ROOT), names);
				event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
				return;
			}

			Utils.selectOption(locale, event.channel(), stash, card, event.user())
					.thenAccept(sc -> {
						if (sc == null) {
							event.channel().sendMessage(locale.get("error/invalid_value")).queue();
							return;
						}

						selfOffer.remove(sc.getId());
						event.channel().sendMessage(locale.get("success/offer_remove", event.user().getAsMention(), sc)).queue();
					})
					.exceptionally(t -> {
						event.channel().sendMessage(locale.get("error/not_owned")).queue();
						return null;
					});
		}
	}
}
