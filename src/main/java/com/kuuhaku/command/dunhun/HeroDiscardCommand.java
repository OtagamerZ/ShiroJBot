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
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
		name = "hero",
		path = "discard",
		category = Category.STAFF
)
@Syntax("<ids:text:r>")
public class HeroDiscardCommand implements Executable {
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

		List<Gear> gears = new ArrayList<>();
		Set<Integer> ids = Arrays.stream(args.getString("ids").toUpperCase().split(" +"))
				.map(NumberUtils::toInt)
				.collect(Collectors.toSet());

		for (Integer id : ids) {
			Gear g = h.getInvGear(id);
			if (g == null) {
				event.channel().sendMessage(locale.get("error/gear_not_found")).queue();
				return;
			}

			gears.add(g);
		}

		List<String> names = gears.stream()
				.map(g -> g.getName(locale))
				.limit(5)
				.collect(Collectors.toCollection(ArrayList::new));

		if (names.size() < gears.size()) {
			names.add(locale.get("str/and_more", gears.size() - names.size()));
		}

		try {
			Utils.confirm(locale.get(gears.size() == 1 ? "question/discard" : "question/discard_multi", Utils.properlyJoin("").apply(names)), event.channel(),
					w -> {
						DAO.applyNative(GearAffix.class, "DELETE FROM gear_affix WHERE gear_id IN ?1", ids);
						DAO.applyNative(Gear.class, "DELETE FROM gear WHERE id IN ?1", ids);

						event.channel().sendMessage(locale.get(gears.size() == 1 ? "success/discard" : "success/discard_multi")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
