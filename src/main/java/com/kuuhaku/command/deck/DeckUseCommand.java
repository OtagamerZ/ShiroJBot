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

import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.AccountSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "deck",
		path = "use",
		category = Category.INFO
)
@Signature(allowEmpty = true, value = {
		"<id:number:r>",
		"<name:word:r>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class DeckUseCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		AccountSettings settings = acc.getSettings();

		if (args.isEmpty()) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/decks"));

			AtomicInteger idx = new AtomicInteger();
			List<Page> pages = Utils.generatePages(eb, acc.getDecks(), 10, 2,
					deck -> new FieldMimic(
							(deck.isCurrent() ? "✅ " : "") + "`" + idx.getAndIncrement() + " | " + deck.getName() + "`",
							deck.toString(locale)
					).toString()
			);

			Utils.paginate(pages, event.channel(), event.user());
			return;
		}

		List<Deck> decks = acc.getDecks();
		int id = args.getInt("id", -1);
		if (Utils.between(id, 0, decks.size() - 1)) {
			Deck d = decks.get(id);
			settings.setCurrentDeck(d.getId());
			settings.save();

			event.channel().sendMessage(locale.get("success/deck_switch", d.getName())).queue();
			return;
		}

		String name = args.getString("name");
		for (Deck deck : decks) {
			if (deck.getName().equalsIgnoreCase(name)) {
				settings.setCurrentDeck(deck.getId());
				settings.save();

				event.channel().sendMessage(locale.get("success/deck_switch", deck.getName())).queue();
				return;
			}
		}

		event.channel().sendMessage(locale.get("error/deck_not_found")).queue();
	}
}