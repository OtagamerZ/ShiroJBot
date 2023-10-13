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

package com.kuuhaku.command.market;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.MarketOrder;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.List;

/*
@Command(
		name = "market",
		path = "order",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<card:word:r> <price:number:r>")
 */
public class MarketOrderCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getCapacity() <= 0) {
			event.channel().sendMessage(locale.get("error/insufficient_space")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM v_card_names");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		int price = args.getInt("price");
		if (!Utils.between(price, 1, 10_000_000)) {
			event.channel().sendMessage(locale.get("error/invalid_value_range", 1, 10_000_000)).queue();
			return;
		}

		Account acc = kp.getAccount();
		if (!acc.hasEnough(price, Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/order", card, price), event.channel(), w -> {
						new MarketOrder(kp, card, price).save();
						event.channel().sendMessage(locale.get("success/order_placed")).queue();

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
