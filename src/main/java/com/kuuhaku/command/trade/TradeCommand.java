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

package com.kuuhaku.command.trade;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Trade;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

@Command(
		name = "trade",
		category = Category.MISC
)
@Signature("<user:user:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class TradeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Trade check = Trade.getPending().get(event.user().getId());
		if (check != null) {
			Account other;
			if (check.getLeft().getUid().equals(event.user().getId())) {
				other = check.getRight();
			} else {
				other = check.getLeft();
			}

			event.channel().sendMessage(locale.get("error/running_trade", other.getName())).queue();
			return;
		}

		User other = event.message().getMentionedUsers().get(0);
		if (other.equals(event.user())) {
			event.channel().sendMessage(locale.get("error/self_not_allowed")).queue();
			return;
		}

		Trade trade = new Trade(event.user().getId(), other.getId());
		Utils.confirm(
				locale.get("question/trade_open", other.getAsMention(), event.user().getAsMention()), event.channel(),
				wrapper -> {
					if (!wrapper.getUser().equals(other)) return;

					Trade.getPending().put(trade, event.user().getId(), other.getId());
					event.channel().sendMessage(locale.get("success/trade_open", data.config().getPrefix())).queue();
				}, m -> trade.setFinalizing(false),
				event.user(), other
		);
	}
}
