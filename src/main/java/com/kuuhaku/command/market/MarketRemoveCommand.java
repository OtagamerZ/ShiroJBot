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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.List;

@Command(
		name = "market",
		path = "remove",
		category = Category.MISC
)
@Signature("<card:word:r> <confirm:word>[confirm]")
public class MarketRemoveCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStashUsage() == 0) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM v_card_names");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		List<StashedCard> stash = DAO.queryAll(StashedCard.class,
				"SELECT s FROM StashedCard s WHERE s.kawaipon.uid = ?1 AND s.price > 0",
				event.user().getId()
		);
		Utils.selectOption(args.has("confirm"), locale, event.channel(), stash, card, event.user())
				.thenAccept(sc -> {
					if (sc == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					}

					sc.setPrice(0);
					sc.save();

					event.channel().sendMessage(locale.get("success/market_remove")).queue();
				})
				.exceptionally(t -> {
					if (!(t.getCause() instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}
}
