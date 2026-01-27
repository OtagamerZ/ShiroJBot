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

import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "hero",
		path = "respecialize",
		category = Category.STAFF
)
@Syntax({
		"<type:word>[attributes,skills]"
})
public class HeroRespecCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero(locale);
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		String type = args.getString("type", null);

		int points = 0;
		if (type == null || type.equals("attributes")) {
			points += h.getStats().getAttributePoints();
		}
		if (type == null || type.equals("skills")) {
			points += h.getStats().getSkillPoints();
		}

		int cost = Calc.round(300 * Math.pow(1.075, points));

		Account acc = data.profile().getAccount();
		if (!acc.hasEnough(cost, Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/respec", points, cost), event.channel(), w -> {
						if (acc.hasChanged()) {
							event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
							return true;
						}

						h.apply(n -> {
							if (type == null || type.equals("attributes")) {
								n.getStats().setAttributes(new Attributes());
							}
							if (type == null || type.equals("skills")) {
								n.getStats().getUnlockedSkills().clear();
							}
						});

						acc.consumeCR(cost, "Respecialized (" + Utils.getOr(type, "all") + ") ");

						event.channel().sendMessage(locale.get("success/respec")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
