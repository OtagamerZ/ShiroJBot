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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.time.temporal.ChronoField;
import java.util.List;

@Command(
		name = "favor",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<card:word:r>")
public class FavorCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (args.isEmpty()) {
			if (kp.getFavCard() == null) {
				event.channel().sendMessage(locale.get("error/no_favor")).queue();
				return;
			}

			event.channel().sendMessage(locale.get("str/current_favor", kp.getFavCard(), Constants.TIMESTAMP_R.formatted(kp.getFavExpiration().getLong(ChronoField.INSTANT_SECONDS)))).queue();
			return;
		}

		if (kp.getFavCard() != null) {
			event.channel().sendMessage(locale.get("error/favor_remaining")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			String sug = Utils.didYouMean(args.getString("card").toUpperCase(), "SELECT id AS value FROM v_card_names");
			event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			return;
		} else {
			switch (card.getRarity()) {
				case FIELD -> {
					Field f = card.asField();

					if (f.isEffect()) {
						event.channel().sendMessage(locale.get("error/cannot_favor")).queue();
						return;
					}
				}
				case EVOGEAR -> {
					Evogear e = card.asEvogear();

					if (e.getTier() < 1) {
						event.channel().sendMessage(locale.get("error/cannot_favor")).queue();
						return;
					}
				}
				case NONE, ULTIMATE -> {
					event.channel().sendMessage(locale.get("error/cannot_favor")).queue();
					return;
				}
			}
		}

		int price;
		if (card.getRarity() == Rarity.EVOGEAR) {
			price = DAO.queryNative(Integer.class, "SELECT 4500 * tier FROM evogear WHERE card_id = ?1", card.getId());
		} else if (card.getRarity() == Rarity.FIELD) {
			price = 6000;
		} else {
			price = 3000 * card.getRarity().getIndex();
		}

		Account acc = kp.getAccount();
		if (!acc.hasEnough(price, Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/card_favor", card, price), event.channel(), w -> {
						acc.consumeCR(price, "Favored " + card);

						kp.setFavCard(card);
						kp.save();

						event.channel().sendMessage(locale.get("success/card_favor")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
