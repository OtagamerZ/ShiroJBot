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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.shoukan.CardRanking;
import com.kuuhaku.util.Bit;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Command(
		name = "deck",
		path = "meta",
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

		List<CardRanking> cards = DAO.queryAllUnmapped("SELECT card_id, get_type(card_id), card_winrate(card_id) FROM v_shoukan_meta").stream()
				.map(o -> {
							int type = NumberUtils.toInt(String.valueOf(o[1]));
							List<CardType> types = List.copyOf(Bit.toEnumSet(CardType.class, type));
							if (types.isEmpty()) return null;

							Drawable<?> card = switch (types.get(types.size() - 1)) {
								case KAWAIPON, SENSHI -> DAO.find(Senshi.class, String.valueOf(o[0]));
								case EVOGEAR -> DAO.find(Evogear.class, String.valueOf(o[0]));
								case FIELD -> DAO.find(Field.class, String.valueOf(o[0]));
							};

							return new CardRanking(NumberUtils.toDouble(String.valueOf(o[2])), type, card);
						}
				)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparingInt(CardRanking::type).thenComparing(CardRanking::compareTo))
				.toList();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/meta_deck_title"));

		Class<?> current = Senshi.class;
		XStringBuilder sb = new XStringBuilder();
		for (CardRanking cr : cards) {
			if (current != cr.card().getClass()) {
				eb.addField(locale.get("type/" + current.getSimpleName()), sb.toString(), true);
				System.out.println(sb);

				sb.clear();
				current = cr.card().getClass();
			}

			sb.appendNewLine(cr);
		}

		if (!sb.isEmpty()) {
			eb.addField(locale.get("type/" + current.getSimpleName()), sb.toString(), true);
			System.out.println(sb);
		}

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}
}
