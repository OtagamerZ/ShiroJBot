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
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Command(
		name = "title",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<name:word:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class SelectTitleCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		if (args.isEmpty()) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/all_titles"));

			Map<String, List<Title>> titles = Title.getAllTitles().stream()
					.collect(Collectors.groupingBy(t -> Utils.getOr(Utils.extract(t.getId(), ".+(?=_(?:I|II|III|IV|V))"), "")));

			List<Page> pages = Utils.generatePages(eb, List.copyOf(titles.values()), 10, ts -> {
				ts.sort(Comparator.comparingInt(t -> t.getRarity().getIndex()));

				StringBuilder sb = new StringBuilder();
				for (Title t : ts) {
					LocalizedTitle info = t.getInfo(locale);

					sb.append("`ID: `");
					if (acc.hasTitle(t.getId())) {
						sb.append(t.getId());
					} else {
						sb.append(t.getId().replaceAll("[A-Z\\d-]", "?"));
					}
					sb.append("` - ").append(info.getDescription()).append("\n");
				}

				Title high = ts.stream()
						.filter(t -> acc.hasTitle(t.getId()))
						.findFirst()
						.orElse(ts.get(0));

				return new MessageEmbed.Field(high.getRarity().getEmote() + high.getInfo(locale).getName(), sb.toString(), true);
			});

			Utils.paginate(pages, event.channel(), event.user());
			return;
		}

		AccountTitle title = DAO.query(AccountTitle.class, "SELECT t FROM AccountTitle t WHERE t.account.uid = ?1 AND t.title.id = UPPER(?2)",
				acc.getUid(), args.getString("name")
		);
		if (title == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT t.title_id FROM account_title t WHERE t.account_uid = ?1", acc.getUid());

			Pair<String, Double> sug = Utils.didYouMean(args.getString("name").toUpperCase(Locale.ROOT), names);
			event.channel().sendMessage(locale.get("error/unknown_title", sug.getFirst())).queue();
			return;
		}

		title.setCurrent(true);
		title.save();

		event.channel().sendMessage(locale.get("success/title")).queue();
	}
}