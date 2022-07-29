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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Giftcode;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "giftcode",
		category = Category.MISC
)
@Signature("<code:text:r>")
public class GiftcodeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Giftcode code = DAO.find(Giftcode.class, args.getString("code"));
		if (code == null) {
			event.channel().sendMessage(locale.get("error/invalid_code")).queue();
			return;
		} else if (code.getRedeemer() != null) {
			event.channel().sendMessage(locale.get("error/giftcode_used")).queue();
			return;
		}

		if (code.redeem(data.profile().getAccount())) {
			event.channel().sendMessage(locale.get("success/giftcode", code.getDescription())).queue();
		} else {
			event.channel().sendMessage(locale.get("error/giftcode")).queue();
		}
	}
}