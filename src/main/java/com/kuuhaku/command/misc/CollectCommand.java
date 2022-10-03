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

package com.kuuhaku.command.misc;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.common.SingleUseReference;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Spawn;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "collect",
		category = Category.MISC
)
public class CollectCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Kawaipon kp = acc.getKawaipon();

		SingleUseReference<KawaiponCard> card = Spawn.getSpawnedCard(event.channel());
		try {
			if (!card.isValid()) {
				event.channel().sendMessage(locale.get("error/no_card")).queue();
				return;
			} else if (card.peekProperty(kc -> kp.getCollection().contains(kc))) {
				event.channel().sendMessage(locale.get("error/owned")).queue();
				return;
			} else if (card.peekProperty(kc -> !acc.hasEnough(kc.getPrice(), Currency.CR))) {
				event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
				return;
			}
		} catch (NullPointerException e) {
			event.channel().sendMessage(locale.get("error/no_card")).queue();
			return;
		}

		KawaiponCard kc = card.get();

		acc.consumeCR(kc.getPrice(), "Collected " + kc);
		kc.collect(kp);

		event.channel().sendMessage(locale.get("success/collected", event.user().getAsMention(), kc)).queue();
	}
}
