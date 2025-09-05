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

package com.kuuhaku.command.stash;

import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.CardFilter;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.StashItem;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "stash",
		category = Category.MISC
)
@Syntax({
		"<action:word:r>[help]",
		"<params:text>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class StashCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStashUsage() == 0) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		String[] content = args.getString("params").split("\\s+");
		Pair<CommandLine, Options> cli = Utils.getCardCLI(locale, content, false);
		if (args.has("action")) {
			XStringBuilder sb = new XStringBuilder();
			for (Option opt : cli.getSecond().getOptions()) {
				sb.appendNewLine("`-%s --%s` - %s".formatted(
						opt.getOpt(),
						opt.getLongOpt(),
						opt.getDescription()
				));
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/search_params"))
					.setDescription(sb.toString());

			event.channel().sendMessageEmbeds(eb.build()).queue();
			return;
		}

		XStringBuilder query = new XStringBuilder(CardFilter.BASE_QUERY);
		query.appendNewLine("WHERE c.kawaipon.uid = ?1");

		List<Object> params = new ArrayList<>();
		params.add(event.user().getId());

		AtomicInteger i = new AtomicInteger(2);
		Option[] opts = cli.getFirst().getOptions();
		for (Option opt : opts) {
			CardFilter sf = CardFilter.getByArgument(opt.getOpt());
			if (sf == null || sf.isMarketOnly()) continue;

			String filter = sf.getWhereClause();
			if (opt.hasArg()) {
				filter = filter.formatted(i.getAndIncrement());
				params.add(opt.getValue().toUpperCase());
			}

			query.appendNewLine(filter);
		}

		query.appendNewLine("ORDER BY c.lastChange DESC");

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		ThrowingFunction<Integer, Page> loader = p -> {
			try {
				List<StashedCard> results = DAO.queryBuilder(
						StashedCard.class,
						query.toString(),
						q -> q.setFirstResult(p * 10).setMaxResults(10).getResultList(),
						params.toArray()
				);

				eb.setAuthor(locale.get("str/search_result_stash", results.size(), kp.getStashUsage(), kp.getMaxCapacity()));
				return Utils.generatePage(eb, results, 5, sc -> new StashItem(locale, sc).toString());
			} catch (Exception e) {
				event.channel().sendMessage(locale.get("error/invalid_params")).queue();
				return null;
			}
		};

		if (loader.apply(0) == null) {
			event.channel().sendMessage(locale.get("error/no_result")).queue();
			return;
		}

		Utils.paginate(loader, event.channel(), event.user());
	}
}
