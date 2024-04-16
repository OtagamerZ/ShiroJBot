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
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.util.Calendar;

@Command(
		name = "market",
		path = "buy",
		category = Category.MISC
)
@Signature("<id:number:r>")
public class MarketBuyCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getCapacity() <= 0) {
			event.channel().sendMessage(locale.get("error/insufficient_space")).queue();
			return;
		}

		StashedCard sc = DAO.query(StashedCard.class, "SELECT s FROM StashedCard s WHERE s.price > 0 AND s.id = ?1", args.getInt("id"));
		if (sc == null) {
			event.channel().sendMessage(locale.get("error/not_announced")).queue();
			return;
		} else if (sc.getKawaipon().equals(kp)) {
			event.channel().sendMessage(locale.get("error/cannot_buy_own")).queue();
			return;
		}

		int id = args.getInt("id");
		int price = sc.getPrice();
		Calendar cal = Calendar.getInstance();
		Market m = new Market(event.user().getId());

		if (cal.get(Calendar.MONTH) == Calendar.NOVEMBER && cal.get(Calendar.WEEK_OF_MONTH) == 4) {
			price = (int) (price * 0.66);
		}

		if (sc.equals(m.getDailyOffer())) {
			price = (int) (price * 0.8);
		}

		Account acc = kp.getAccount();
		if (!acc.hasEnough(price, Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/purchase", sc, price), event.channel(), w -> {
						if (m.buy(id)) {
							event.channel().sendMessage(locale.get("success/market_purchase", sc)).queue();
						} else {
							event.channel().sendMessage(locale.get("error/not_announced")).queue();
						}

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
