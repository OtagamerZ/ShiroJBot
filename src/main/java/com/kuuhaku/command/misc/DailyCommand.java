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
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "daily",
		category = Category.MISC
)
public class DailyCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		long cd = acc.collectDaily();

		if (cd > 0) {
			event.channel().sendMessage(locale.get("error/daily_collected", Utils.toStringDuration(locale, cd))).queue();
			return;
		} else if (cd == -1) {
			event.channel().sendMessage(locale.get("error/daily_limit", Utils.toStringDuration(locale, cd))).queue();
			return;
		}

		event.channel().sendMessage(locale.get("success/daily")).queue();

		acc.addVote(); // TODO Remove
		if (acc.getStreak() > 0 && acc.getStreak() % 7 == 0) {
			int gems = Math.min((int) Calc.getFibonacci(acc.getStreak() / 7), 3);
			acc.addGems(gems, "Vote streak " + acc.getStreak());
			data.notify(locale.get("achievement/gem", event.user().getAsMention(), gems, acc.getStreak()));
		}
	}
}