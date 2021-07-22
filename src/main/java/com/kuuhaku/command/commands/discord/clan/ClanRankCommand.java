/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.clan;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.model.records.ClanRanking;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;

import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "clanrank",
		aliases = {"crank", "topclan", "topc"},
		usage = "req_actual",
		category = Category.CLAN
)
@Requires({Permission.MESSAGE_ATTACH_FILES})
public class ClanRankCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		}

		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			try {
				if (args.length == 0) {
					List<ClanRanking> rank = ClanDAO.getClanRanking();

					CategoryChart chart = Helper.buildBarChart(
							"Ranking dos Clans",
							Pair.of("", "Pontos"),
							rank.stream().map(ClanRanking::getColor).collect(Collectors.toList())
					);

					for (ClanRanking cr : rank) {
						chart.addSeries(cr.name(),
								List.of("Clan"),
								List.of(cr.score())
						);
					}

					channel.sendFile(Helper.writeAndGet(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "ranking", "png")).queue();
					m.delete().queue();
				}
			} catch (Exception e) {
				m.editMessage(I18n.getString("err_clan-rank")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
