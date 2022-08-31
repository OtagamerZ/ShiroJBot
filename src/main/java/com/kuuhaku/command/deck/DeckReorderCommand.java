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

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Command(
		name = "deck",
		subname = "order",
		category = Category.INFO
)
@Signature({
		"<from:number:r> <to:number:r> <kind:word>[s,e,f]",
		"<order:word:r>[name,atk,def,attr,hp,mp,tier] <kind:word>[s,e,f]"
})
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class DeckReorderCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		List<? extends Drawable<?>> cards = switch (args.getString("kind", "s")) {
			case "e" -> d.getEvogear();
			case "f" -> d.getFields();
			default -> d.getSenshi();
		};

		if (args.has("from")) {
			int from = args.getInt("from");
			int to = args.getInt("to");

			try {
				Collections.swap(cards, from, to);
				d.save();

				event.channel().sendMessage(locale.get("success/deck_reorder")).queue();
			} catch (IndexOutOfBoundsException e) {
				event.channel().sendMessage(locale.get("error/invalid_indexes")).queue();
			}
		} else {
			Comparator<? super Drawable<?>> order = switch (args.getString("order")) {
				case "name" -> Comparator.comparing(c -> c.getCard().getId());
				case "atk" -> Comparator.<Drawable<?>>comparingInt(Drawable::getDmg).reversed();
				case "def" -> Comparator.<Drawable<?>>comparingInt(Drawable::getDfs).reversed();
				case "attr" -> Comparator.<Drawable<?>>comparingInt(c -> c.getDmg() + c.getDfs()).reversed();
				case "hp" -> Comparator.<Drawable<?>>comparingInt(Drawable::getHPCost).reversed();
				case "mp" -> Comparator.<Drawable<?>>comparingInt(Drawable::getMPCost).reversed();
				case "tier" -> Comparator.<Drawable<?>>comparingInt(c -> {
					if (c instanceof Evogear e) {
						return e.getTier();
					}

					return 0;
				}).reversed();
				default -> throw new IllegalStateException("Unexpected value: " + args.getString("order"));
			};
			cards.sort(order);
			d.save();

			event.channel().sendMessage(locale.get("success/deck_reorder")).queue();
		}
	}
}