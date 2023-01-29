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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "stash",
		subname = "scrap",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[trash]",
		"<card:word:r> <confirm:word>[y]"
})
public class StashScrapCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStash().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		if (args.containsKey("action")) {
			List<StashedCard> trash = kp.getTrash();
			int value = getValue(trash);

			try {
				Utils.confirm(locale.get("question/scrap_trash", trash.size(), value), event.channel(), w -> {
							event.channel().sendMessage(locale.get("success/scrap")).queue();
							kp.getAccount().addCR(value, trash.stream().map(StashedCard::toString).collect(Collectors.joining()) + " scrapped");
							for (StashedCard sc : trash) {
								sc.delete();
							}

							return true;
						}, event.user()
				);
			} catch (PendingConfirmationException e) {
				event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
			}

			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card WHERE rarity NOT IN ('ULTIMATE', 'NONE')");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		Utils.selectOption(args.containsKey("confirm"), locale, event.channel(), kp.getNotInUse(), card, event.user())
				.thenAccept(sc -> {
					if (sc == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					}

					int value = getValue(sc);

					try {
						Utils.confirm(locale.get("question/scrap", value), event.channel(), w -> {
									event.channel().sendMessage(locale.get("success/scrap")).queue();
									kp.getAccount().addCR(value, sc + " scrapped");
									sc.delete();
									return true;
								}, event.user()
						);
					} catch (PendingConfirmationException e) {
						event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
					}
				})
				.exceptionally(t -> {
					if (!(t.getCause() instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}

	private int getValue(StashedCard card) {
		return getValue(List.of(card));
	}

	private int getValue(Collection<StashedCard> cards) {
		int value = 0;
		for (StashedCard sc : cards) {
			double mult = Calc.rng(1, 1.75, sc.getId());
			if (sc.getType() == CardType.KAWAIPON) {
				KawaiponCard kc = sc.getKawaiponCard();
				value += (int) (kc.getSuggestedPrice() / 3 * mult);
			} else {
				if (sc.getType() == CardType.EVOGEAR) {
					Evogear e = DAO.find(Evogear.class, sc.getCard().getId());
					value += (int) (e.getTier() * 225 * mult);
				} else {
					value += (int) (2500 * mult);
				}
			}
		}

		return value;
	}
}
