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

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.AccountTitle;
import com.kuuhaku.model.persistent.user.LocalizedTitle;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.*;
import java.util.stream.Collectors;

@Command(
		name = "title",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<id:word:r>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class SelectTitleCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		if (args.isEmpty()) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/all_titles"));

			List<ArrayList<Title>> titles = Title.getAllTitles().stream()
					.collect(Collectors.groupingBy(t -> Utils.getOr(Utils.extract(t.getId(), ".+(?=_(?:I|II|III|IV|V))|.+"), "")))
					.values().stream()
					.map(ts -> ts.stream()
							.sorted(Comparator.comparing(t -> t.getRarity().ordinal()))
							.collect(ArrayList<Title>::new, (lst, t) -> {
								if (acc.hasTitle(t.getId())) {
									if (lst.isEmpty()) {
										lst.add(t);
									} else {
										lst.set(0, t);
									}
								} else {
									lst.add(t);
								}
							}, ArrayList::addAll)
					)
					.sorted(Comparator.comparing(t -> t.getFirst().getRarity().ordinal(), Comparator.reverseOrder()))
					.toList();

			List<Page> pages = Utils.generatePages(eb, titles, 10, 5, ts -> {
				StringBuilder sb = new StringBuilder();

				Title current = ts.getFirst();
				LocalizedTitle info = current.getInfo(locale);
				boolean has = acc.hasTitle(current.getId());
				sb.append("`ID: ");
				if (has) {
					sb.append(current.getId());
				} else {
					sb.append(current.getId().replaceAll("[A-Z\\d-]", "?"));
				}
				sb.append("`\n").append(info.getDescription());

				if (has) {
					Title next = Utils.getNext(current, ts);
					if (next != null) {
						sb.append("\n").append(locale.get("str/next_tier", next.getInfo(locale).getDescription()));
					}
				}

				String track = current.track(acc);
				if (track != null) {
					sb.append("\n").append(locale.get("str/current_tracker", track));
				}

				return new FieldMimic(current.getRarity().getEmote(null) + info.getName(), sb.toString()).toString();
			});

			Utils.paginate(pages, event.channel(), event.user());
			return;
		} else if (args.has("action")) {
			AccountTitle title = acc.getTitle();
			if (title != null) {
				title.setCurrent(false);
				title.save();
			}

			event.channel().sendMessage(locale.get("success/title_clear")).queue();
			return;
		}

		AccountTitle title = DAO.query(AccountTitle.class, "SELECT t FROM AccountTitle t WHERE t.account.uid = ?1 AND t.title.id = UPPER(?2)",
				acc.getUid(), args.getString("id")
		);
		if (title == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT t.title_id FROM account_title t WHERE t.account_uid = ?1", acc.getUid());

			Pair<String, Double> sug = Utils.didYouMean(args.getString("name").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_title", sug.getFirst())).queue();
			return;
		}

		title.setCurrent(true);
		title.save();

		event.channel().sendMessage(locale.get("success/title")).queue();
	}
}
