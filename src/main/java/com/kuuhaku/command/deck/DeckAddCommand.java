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

package com.kuuhaku.command.deck;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.NoResultException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;

@Command(
		name = "deck",
		path = "add",
		category = Category.MISC
)
@Signature({
		"<card:word:r> <amount:number>",
		"<card:word:r>"
})
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS
})
public class DeckAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStashUsage() == 0) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			String sug = Utils.didYouMean(args.getString("card").toUpperCase(), "SELECT id AS value FROM v_card_names");
			if (sug.equalsIgnoreCase("NULL")) {
				event.channel().sendMessage(locale.get("error/unknown_card_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			}
			return;
		}

		List<StashedCard> stash = kp.getNotInUse();
		if (args.has("amount")) {
			int qtd = args.getInt("amount");
			if (qtd < 1) {
				event.channel().sendMessage(locale.get("error/invalid_value_low", 1)).queue();
				return;
			}

			Deck dk = d.refresh();
			for (int i = 0, j = 0; i < stash.size() && j < qtd; i++) {
				StashedCard sc = stash.get(i);
				if (sc.getCard().equals(card)) {
					if (!addToDeck(event, locale, dk, sc)) {
						if (j == 0) {
							event.channel().sendMessage(locale.get("error/not_owned")).queue();
							return;
						}

						break;
					}

					j++;
				}
			}

			event.channel().sendMessage(locale.get("success/deck_add")).queue();
			return;
		}

		Utils.selectOption(locale, event.channel(), stash, card, event.user())
				.thenAccept(sc -> {
					Deck dk = d.refresh();
					if (!addToDeck(event, locale, dk, sc)) return;
					dk.save();

					event.channel().sendMessage(locale.get("success/deck_add")).queue();
				})
				.exceptionally(t -> {
					if (!(t.getCause() instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}

	private boolean addToDeck(MessageData.Guild event, I18N locale, Deck d, StashedCard sc) {
		if (sc == null) {
			event.channel().sendMessage(locale.get("error/invalid_value")).queue();
			return false;
		}

		switch (sc.getType()) {
			case KAWAIPON, SENSHI -> {
				Senshi s = sc.getCard().asSenshi();

				if (s.isFusion()) {
					event.channel().sendMessage(locale.get("error/cannot_add_card")).queue();
					return false;
				} else if (sc.getKawaiponCard().isChrome()) {
					event.channel().sendMessage(locale.get("error/cannot_add_chrome")).queue();
					return false;
				} else if (d.getSenshiRaw().size() >= 36) {
					event.channel().sendMessage(locale.get("error/deck_full")).queue();
					return false;
				}
			}
			case EVOGEAR -> {
				Evogear e = sc.getCard().asEvogear();

				if (e.getTier() < 1) {
					event.channel().sendMessage(locale.get("error/cannot_add_card")).queue();
					return false;
				} else if (d.getEvogearRaw().size() >= 24) {
					event.channel().sendMessage(locale.get("error/deck_full")).queue();
					return false;
				}
			}
			case FIELD -> {
				Field f = sc.getCard().asField();

				if (f.isEffect()) {
					event.channel().sendMessage(locale.get("error/cannot_add_card")).queue();
					return false;
				} else if (d.getFieldsRaw().size() >= 3) {
					event.channel().sendMessage(locale.get("error/deck_full")).queue();
					return false;
				}
			}
		}

		sc.setDeck(d);
		sc.save();
		return true;
	}
}
