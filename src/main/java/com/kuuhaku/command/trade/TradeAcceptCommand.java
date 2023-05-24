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

package com.kuuhaku.command.trade;

import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.common.Trade;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

@Command(
		name = "trade",
		path = "accept",
		category = Category.MISC
)
public class TradeAcceptCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Trade trade = Trade.getPending().get(event.user().getId());
		if (trade == null) {
			event.channel().sendMessage(locale.get("error/not_in_trade")).queue();
			return;
		} else if (trade.isFinalizing()) {
			event.channel().sendMessage(locale.get("error/trade_finalizing")).queue();
			return;
		}

		User other;
		if (trade.getLeft().getUid().equals(event.user().getId())) {
			other = trade.getRight().getUser();
		} else {
			other = trade.getLeft().getUser();
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/trade_title", trade.getLeft().getName(), trade.getRight().getName()))
				.addField(trade.getLeft().getName(), trade.toString(locale, true), true)
				.addField(trade.getRight().getName(), trade.toString(locale, false), true);

		try {
			Utils.confirm(
					locale.get("question/trade_close", other.getAsMention(), event.user().getAsMention()),
					eb.build(), event.channel(),
					w -> {
						if (!trade.validate()) {
							event.channel().sendMessage(locale.get("error/trade_invalid")).queue();
						} else {
							trade.accept();
							event.channel().sendMessage(locale.get("success/trade_accept")).queue();
						}

						Trade.getPending().remove(event.user().getId());
						return true;
					}, m -> trade.setFinalizing(false), other
			);
			trade.setFinalizing(true);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
