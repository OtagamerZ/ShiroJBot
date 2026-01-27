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

package com.kuuhaku.command.misc;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.records.SingleUseReference;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Spawn;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.math.NumberUtils;

@Command(
		name = "collect",
		category = Category.MISC
)
public class CollectCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Kawaipon kp = acc.getKawaipon();

		SingleUseReference<StashedCard> card = Spawn.getSpawnedCard(event.channel());
		try {
			if (!card.isValid()) {
				event.channel().sendMessage(locale.get("error/no_card")).queue();
				return;
			} else if (Boolean.TRUE.equals(card.peekProperty(kp::hasCard))) {
				event.channel().sendMessage(locale.get("error/owned")).queue();
				return;
			} else if (Boolean.TRUE.equals(card.peekProperty(sc -> !acc.hasEnough(sc.getCollectPrice(), Currency.CR)))) {
				event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
				return;
			}

			StashedCard sc = card.get();
			sc.setKawaipon(kp);
			sc.setInCollection(true);
			if (acc.consumeItem("special_spice")) {
				sc.setChrome(true);
			}
			sc.save();

			acc.consumeCR(sc.getCollectPrice(), "Collected " + sc);
			acc.setDynValue("collected", NumberUtils.toInt(acc.getDynValue("collected", "0")) + 1);
			event.channel().sendMessage(locale.get("success/collected", event.user().getAsMention(), sc)).queue();
		} catch (NullPointerException e) {
			event.channel().sendMessage(locale.get("error/no_card")).queue();
		}
	}
}
