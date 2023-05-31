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

package com.kuuhaku.command.info;

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.time.temporal.ChronoField;

@Command(
		name = "atm",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class WalletCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/wallet", event.member().getEffectiveName()))
				.addField(Constants.VOID, locale.get("str/wallet_info_1",
						acc.getBalance(),
						acc.getDebit(),
						acc.getGems()
				), true)
				.addField(Constants.VOID, locale.get("str/wallet_info_2",
						acc.getLastVote() == null ? locale.get("str/never") : Constants.TIMESTAMP.formatted(acc.getLastVote().getLong(ChronoField.INSTANT_SECONDS)),
						acc.getStreak()
				), true);

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}
}