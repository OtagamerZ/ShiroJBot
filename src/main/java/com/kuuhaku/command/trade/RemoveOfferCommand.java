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
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Trade;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "remove",
		category = Category.MISC
)
@Signature({
		"<value:number:r>",
		"<card:word:r>"
})
public class RemoveOfferCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Trade trade = DAO.query(Trade.class, "SELECT t FROM Trade t WHERE ?1 IN (t.left.uid, t.right.uid) AND t.closed = FALSE", event.user().getId());
		if (trade == null) {
			event.channel().sendMessage(locale.get("error/not_in_trade")).queue();
			return;
		}

		Account other;
		if (trade.getLeft().getUid().equals(event.user().getId())) {
			other = trade.getRight();
		} else {
			other = trade.getLeft();
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

			trade.getSelf(event.user().getId()).addCR(offer, "Trade Nº" + trade.getId() + " offer remove");
			trade.addSelfValue(event.user().getId(), -offer);
			trade.save();

			event.channel().sendMessage(locale.get("success/offer_remove", event.user().getAsMention(), offer + " ₵R")).queue();
		} else {
			//TODO
		}
	}
}
