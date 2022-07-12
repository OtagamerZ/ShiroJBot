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

package com.kuuhaku.command.deck;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import javax.persistence.NoResultException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Command(
		name = "deck",
		subname = "remove",
		category = Category.MISC
)
@Signature("<card:word:r>")
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS
})
public class DeckRemoveCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getCards().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase(Locale.ROOT));
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(Locale.ROOT), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		Set<CardType> types = Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card.getId()));
		if (types.isEmpty()) {
			event.channel().sendMessage(locale.get("error/not_in_shoukan")).queue();
			return;
		}

		List<StashedCard> stash = DAO.queryAll(StashedCard.class,
				"SELECT s FROM StashedCard s WHERE s.kawaipon.uid = ?1 AND s.deck.id = ?2 AND s.card.id IN ?3",
				event.user().getId(), d.getId(), card.getId()
		);
		if (stash.isEmpty()) {
			event.channel().sendMessage(locale.get("error/not_in_deck")).queue();
			return;
		}

		Utils.selectOption(locale, event.channel(), stash, card, event.user())
				.thenAccept(sc -> {
					if (sc == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					}

					Deck dk = d.refresh();
					switch (sc.getType()) {
						case KAWAIPON -> {
							Iterator<Senshi> it = dk.getSenshi().iterator();
							while (it.hasNext()) {
								Senshi s = it.next();
								if (s.getCard().equals(sc.getCard())) {
									it.remove();
									break;
								}
							}

							sc.setDeck(null);
						}
						case EVOGEAR -> {
							//TODO
						}
						case FIELD -> {
							//TODO
						}
					}
					sc.save();
					dk.save();

					event.channel().sendMessage(locale.get("success/deck_remove")).queue();
				})
				.exceptionally(t -> {
					if (!(t instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}
}