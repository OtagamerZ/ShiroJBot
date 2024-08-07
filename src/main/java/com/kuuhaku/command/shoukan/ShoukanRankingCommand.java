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

package com.kuuhaku.command.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.RankShoukanEntry;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;

@Command(
		name = "shoukan",
		path = "ranking",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class ShoukanRankingCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<RankShoukanEntry> rank = DAO.queryAllUnmapped("SELECT * FROM v_shoukan_ranking LIMIT 10").stream()
				.map(o -> Utils.map(RankShoukanEntry.class, o))
				.toList();

		if (rank.isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_ranking")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/rank_title", "Shoukan"));

		for (int i = 0; i < rank.size(); i++) {
			RankShoukanEntry e = rank.get(i);
			if (i < 3) {
				eb.appendDescription("**");
			}

			if (e.uid().equals(event.user().getId())) {
				eb.appendDescription("__");
			}

			eb.appendDescription(switch (i) {
				case 0 -> "\uD83E\uDD47";
				case 1 -> "\uD83E\uDD48";
				case 2 -> "\uD83E\uDD49";
				default -> i + 1;
				} + " - " + e.name() + "`🔰" + e.score() + "`");

			if (e.uid().equals(event.user().getId())) {
				eb.appendDescription("__");
			}

			if (i < 3) {
				eb.appendDescription("**");
			}

			eb.appendDescription("\n> -# " + locale.get("str/matches", e.matches()) + " | " + locale.separate(e.winrate()) + "% WR\n\n");
		}

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}
}
