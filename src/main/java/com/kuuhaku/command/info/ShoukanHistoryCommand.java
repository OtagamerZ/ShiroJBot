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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.shoukan.RaceStats;
import com.kuuhaku.model.records.shoukan.history.Match;
import com.kuuhaku.model.records.shoukan.history.Player;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(
		name = "shoukan",
		path = "history",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class ShoukanHistoryCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/history_title", acc.getName()))
				.setDescription(locale.get("str/history_index"));

		event.channel().sendMessageEmbeds(eb.build()).queue(s ->
				Pages.buttonize(s, Utils.with(new LinkedHashMap<>(), m -> {
							m.put(Utils.parseEmoji("📔"), w -> viewMatches(locale, s, acc));
							m.put(Utils.parseEmoji("📊"), w -> viewRaces(locale, s, acc));
						}),
						true, true, 1, TimeUnit.MINUTES, u -> u.equals(event.user()))
		);
	}

	private void viewMatches(I18N locale, Message msg, Account acc) {
		List<Match> matches = acc.getMatches();
		if (matches.isEmpty()) {
			msg.editMessage(locale.get("error/no_matches")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/history_title", acc.getName()))
				.setDescription(locale.get("str/history_matches",
						acc.getShoukanRanking(),
						Utils.roundToString(acc.getWinrate(), 2) + "%",
						matches.size()
				));

		List<Page> pages = Utils.generatePages(eb, matches, 10, 5,
				m -> {
					String out = Stream.of(m.info().top(), m.info().bottom())
							.map(p -> {
								Account a = DAO.find(Account.class, p.uid());
								String icon;
								if (p.origin().isPure()) {
									icon = Utils.getEmoteString(p.origin().major().name());
								} else {
									icon = Utils.getEmoteString(p.origin().synergy().name());
								}

								if (icon.isBlank()) return a.getName();
								else return icon + " " + a.getName();
							})
							.collect(Collectors.joining(" _VS_ "));

					FieldMimic fm = new FieldMimic(out, "");
					Player winner = m.info().winnerPlayer();
					if (winner == null) {
						fm.appendLine(locale.get("str/draw"));
					} else if (winner.uid().equals(acc.getUid())) {
						fm.appendLine(locale.get("str/win"));
					} else {
						fm.appendLine(locale.get("str/lose"));
					}

					fm.append(" | " + locale.get("str/turns_inline", m.turns().size()));
					fm.appendLine(Constants.TIMESTAMP_R.formatted(m.info().timestamp()));
					return fm.toString();
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		msg.editMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(acc.getUid()))
		);
	}

	private void viewRaces(I18N locale, Message msg, Account acc) {
		List<RaceStats> races = DAO.queryAllUnmapped("""
						SELECT x.flag
						     , x.variant
						     , x.played
						     , x.won
						FROM (
						         SELECT cast(x.flag AS INT)         AS flag
						              , cast(x.variant AS BOOLEAN)  AS variant
						              , count(1)                    AS played
						              , count(nullif(x.won, FALSE)) AS won
						         FROM (
						                  SELECT race_flag(x.major) | race_flag(x.minor) AS flag
						                       , x.variant
						                       , x.won
						                  FROM (
						                           SELECT um.info -> um.side -> 'origin' ->> 'major'      AS major
						                                , um.info -> um.side -> 'origin' -> 'minor' ->> 0 AS minor
						                                , um.info -> um.side -> 'origin' ->> 'variant'    AS variant
						                                , (um.info ->> 'winner') = upper(um.side)         AS won
						                           FROM user_matches(?1) um
						                       ) x
						              ) x
						         GROUP BY flag, x.variant
						     ) x
						ORDER BY cast(x.won AS NUMERIC) / x.played DESC
						""", acc.getUid()).stream()
				.map(o -> Utils.map(RaceStats.class, o))
				.toList();

		if (races.isEmpty()) {
			msg.editMessage(locale.get("error/no_matches")).queue();
			return;
		}

		Race favorite = races.stream()
				.max(Comparator.comparingInt(RaceStats::played).thenComparingInt(RaceStats::won))
				.map(RaceStats::race)
				.orElse(Race.NONE);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/history_title", acc.getName()))
				.setDescription(locale.get("str/history_races",
						Utils.getEmoteString(favorite.name()) + " " + favorite.getName(locale))
				);

		List<Page> pages = Utils.generatePages(eb, races, 10, 5,
				rs -> {
					Race r = rs.race();

					FieldMimic fm = new FieldMimic(
							Utils.getEmoteString(r.name()) + " " + r.getName(locale),
							locale.get("str/history_races_matches",
									Utils.roundToString(Calc.prcnt(rs.won(), rs.played()), 2) + "%",
									rs.played()
							)
					);

					return fm.toString();
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		msg.editMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(acc.getUid()))
		);
	}
}
