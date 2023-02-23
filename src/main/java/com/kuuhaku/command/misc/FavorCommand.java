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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.time.temporal.ChronoField;
import java.util.List;

@Command(
		name = "favored",
		category = Category.MISC
)
@Signature("<card:word:r>")
public class FavorCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		User target = event.message().getMentionedUsers().get(0);

		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getFavCard() != null) {
			event.channel().sendMessage(locale.get("error/favor_remaining", kp.getFavExpiration().get(ChronoField.INSTANT_SECONDS))).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card WHERE rarity NOT IN ('ULTIMATE', 'NONE')");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		int price;
		if (card.getRarity() == Rarity.EVOGEAR) {
			price = DAO.queryNative(Integer.class, "SELECT 4500 * tier FROM evogear WHERE card_id = ?1", card.getId());
		} else if (card.getRarity() == Rarity.FIELD) {
			price = 6000;
		} else {
			price = 3000 * card.getRarity().getIndex();
		}

		try {
			Utils.confirm(locale.get("question/card_favor", card, price), event.channel(), w -> {
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