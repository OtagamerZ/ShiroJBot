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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Seasonal;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.util.Calendar;

@Command(
		name = "convert",
		path = "candy",
		category = Category.MISC
)
@Seasonal(exclude = Calendar.OCTOBER)
public class ConvertCandyCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		UserItem item = DAO.find(UserItem.class, "SPOOKY_CANDY");
		int candies = acc.getItemCount("spooky_candy");
		if (candies <= 0) {
			event.channel().sendMessage(locale.get("error/item_not_have")).queue();
			return;
		}

		try {
			String cr = locale.get("currency/cr", candies * 100);
			Utils.confirm(locale.get("question/item_convert", candies + " " + item.getName(locale), cr), event.channel(), w -> {
						if (acc.hasChanged()) {
							event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
							return true;
						}

						acc.consumeItem(item.getId(), candies, true);
						acc.addCR(candies * 100L, "Converted " + candies);

						event.channel().sendMessage(locale.get("success/item_convert")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}