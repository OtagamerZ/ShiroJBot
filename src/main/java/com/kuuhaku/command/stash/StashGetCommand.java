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

package com.kuuhaku.command.stash;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.Stash;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.List;
import java.util.Locale;

@Command(
		name = "stash",
		subname = "get",
		category = Category.MISC
)
@Signature({"<card:word:r>"})
public class StashGetCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Stash stash = DAO.find(Stash.class, event.user().getId());
		if (stash.getCards().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase(Locale.ROOT));
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(Locale.ROOT), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		Utils.selectOption(locale, event.channel(), stash, card)
				.thenAccept(sc -> {
					if (sc == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					} else if (sc.getTrade() != null) {
						event.channel().sendMessage(locale.get("error/card_in_trade")).queue();
						return;
					}

					Kawaipon kp = stash.getAccount().getKawaipon();
					switch (sc.getType()) {
						case KAWAIPON -> {
							KawaiponCard kc = kp.getCards().stream()
									.filter(c -> c.getCard().equals(card))
									.filter(c -> c.isFoil() == args.getString("kind").equalsIgnoreCase("f"))
									.findFirst().orElse(null);

							if (kc != null) {
								event.channel().sendMessage(locale.get("error/in_collection")).queue();
								return;
							}

							new KawaiponCard(sc.getUUID(), kp, card, sc.isFoil()).save();
						}
						case EVOGEAR -> {
							//TODO
						}
						case FIELD -> {
							//TODO
						}
					}

					Stash sth = sc.getStash();
					sth.getCards().remove(sc);
					sth.save();
					event.channel().sendMessage(locale.get("success/card_retrieved")).queue();
				})
				.exceptionally(t -> {
					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}
}
