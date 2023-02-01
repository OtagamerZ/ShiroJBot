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
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.Objects;

@Command(
		name = "deck",
		subname = "meta",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class DeckMetaCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		List<? extends Drawable<?>> cards = DAO.queryAllNative(String.class, "SELECT card_id FROM v_shoukan_meta ORDER BY type, card_id").stream()
				.map(id -> {
							List<CardType> types = List.copyOf(Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", id)));
							if (types.isEmpty()) return null;

							return switch (types.get(0)) {
								case NONE -> null;
								case KAWAIPON -> DAO.find(Senshi.class, id);
								case EVOGEAR -> DAO.find(Evogear.class, id);
								case FIELD -> DAO.find(Field.class, id);
							};
						}
				)
				.filter(Objects::nonNull)
				.toList();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/meta_deck_title"));

		Class<?> current = Senshi.class;
		XStringBuilder sb = new XStringBuilder();
		for (Drawable<?> card : cards) {
			if (card instanceof Senshi s) {
				sb.appendNewLine(Utils.getEmoteString(Constants.EMOTE_REPO_4, s.getRace().name()) + " " + s);
			} else if (card instanceof Evogear e) {
				sb.appendNewLine(Utils.getEmoteString(Constants.EMOTE_REPO_4, "tier_" + e.getTier()) + " " + e);
			} else if (card instanceof Field f) {
				sb.appendNewLine(switch (f.getType()) {
					case NONE -> f.toString();
					case DAY -> ":sunny: " + f;
					case NIGHT -> ":crescent_moon: " + f;
					case DUNGEON -> ":japanese_castle: " + f;
				});
			}

			if (current != card.getClass()) {
				eb.addField(locale.get("type/" + current.getSimpleName()), sb.toString(), true);

				sb.clear();
				current = card.getClass();
			}
		}

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}
}