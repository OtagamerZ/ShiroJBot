/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.github.ygimenez.model.helper.PaginateHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.shoukan.CodexEntry;
import com.kuuhaku.model.records.shoukan.RaceStats;
import com.kuuhaku.model.persistent.shoukan.history.Match;
import com.kuuhaku.model.persistent.shoukan.history.HistoryPlayer;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.*;
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

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(event.user()::equals)
				.addAction(Utils.parseEmoji("🗃"), w -> viewMatches(locale, w.getMessage(), acc))
				.addAction(Utils.parseEmoji("📊"), w -> viewRaces(locale, w.getMessage(), acc))
				.addAction(Utils.parseEmoji("📔"), w -> codexTracker(locale, w.getMessage(), acc));

		helper.apply(event.channel().sendMessageEmbeds(eb.build())).queue(s -> Pages.buttonize(s, helper));
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
					String out = Stream.of(m.historyInfo().top(), m.historyInfo().bottom())
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
					HistoryPlayer winner = m.historyInfo().getWinnerPlayer();
					if (winner == null) {
						fm.appendLine(locale.get("str/draw"));
					} else if (winner.uid().equals(acc.getUid())) {
						fm.appendLine(locale.get("str/win"));
					} else {
						if (m.historyInfo().winCondition().equalsIgnoreCase("wo")) {
							fm.appendLine(locale.get("str/wo"));
						} else {
							fm.appendLine(locale.get("str/lose"));
						}
					}

					fm.append(" | " + locale.get("str/turns_inline", m.turns().size()));
					fm.appendLine(Constants.TIMESTAMP_R.formatted(m.historyInfo().timestamp()));
					return fm.toString();
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		PaginateHelper helper = new PaginateHelper(pages, true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(u -> u.getId().equals(acc.getUid()));

		helper.apply(msg.editMessageEmbeds(Utils.getEmbeds(pages.getFirst()))).queue(s -> Pages.paginate(s, helper));
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
						ORDER BY cast(x.won AS NUMERIC) / x.played DESC, x.played DESC
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
									Utils.roundToString(rs.won() * 100d / rs.played(), 2) + "%",
									rs.played()
							)
					);

					return fm.toString();
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		PaginateHelper helper = new PaginateHelper(pages, true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(u -> u.getId().equals(acc.getUid()));

		helper.apply(msg.editMessageEmbeds(Utils.getEmbeds(pages.getFirst()))).queue(s -> Pages.paginate(s, helper));
	}

	private void codexTracker(I18N locale, Message msg, Account acc) {
		Set<Race> races = DAO.queryAllUnmapped("""
						SELECT cast(flag AS INT) AS flag
						     , variant
						FROM v_codex_progress
						WHERE uid = ?1
						""", acc.getUid()).stream()
				.map(o -> Utils.map(CodexEntry.class, o))
				.filter(Objects::nonNull)
				.map(CodexEntry::race)
				.collect(Collectors.toSet());

		List<Race> all = Arrays.stream(Race.validValues())
				.filter(r -> Integer.bitCount(r.getFlag()) == 2)
				.toList();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/history_codex", acc.getName(), races.size(), all.size()));

		List<Page> pages = Utils.generatePages(eb, all, 10, 5,
				r -> {
					if (races.contains(r)) {
						return "||" + Utils.getEmoteString(r.name()) + " " + r.getName(locale) + "||\n";
					}

					return Utils.getEmoteString(r.name()) + " " + r.getName(locale) + "\n";
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		PaginateHelper helper = new PaginateHelper(pages, true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(u -> u.getId().equals(acc.getUid()));

		helper.apply(msg.editMessageEmbeds(Utils.getEmbeds(pages.getFirst()))).queue(s -> Pages.paginate(s, helper));
	}
}
