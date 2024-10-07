/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Basetype;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "hero",
		path = {"debug", "gen_item"},
		category = Category.STAFF
)
@Syntax(allowEmpty = true, value = "<id:word:r>")
public class TestItemCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero();
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		Gear g;
		if (args.has("id")) {
			Basetype base = DAO.find(Basetype.class, args.getString("id").toUpperCase());
			if (base == null) {
				event.channel().sendMessage("ERR_BASE_NOT_FOUND").queue();
				return;
			}

			g = Gear.getRandom(h, base);
		} else {
			g = Gear.getRandom(h);
		}

		g.save();
		g = g.refresh();

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		if (g.getAffixes().size() > 2) {
			eb.setTitle(g.getName(locale) + ", " + g.getBasetype().getInfo(locale).getName());
		} else {
			eb.setTitle(g.getName(locale));
		}

		GearAffix imp = g.getImplicit();
		if (imp != null) {
			eb.appendDescription(imp.getDescription(locale) + "\n");
			if (!g.getAffixes().isEmpty()) {
				eb.appendDescription("──────────────────\n");
			}
		}

		for (String l : g.getAffixLines(locale)) {
			eb.appendDescription(l + "\n");
		}

		event.channel().sendMessage("GEN_ITEM")
				.addEmbeds(eb.build())
				.queue();
	}
}
