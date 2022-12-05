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

package com.kuuhaku.command.stash;

import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.StashItem;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "stash",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[help]",
		"<params:text>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class StashCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStash().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		String[] content = args.getString("params").split("\\s+");
		Pair<CommandLine, Options> cli = Utils.getCardCLI(locale, content, false);
		if (args.containsKey("action")) {
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

		Map<String, String> filters = Map.ofEntries(
				Map.entry("n", "AND c.card.id LIKE '%%'||?%s||'%%'"),
				Map.entry("r", "AND CAST(c.card.rarity AS STRING) LIKE '%%'||?%s||'%%'"),
				Map.entry("a", "AND c.card.anime.id LIKE '%%'||?%s||'%%'"),
				Map.entry("c", "AND kc.chrome = TRUE"),
				Map.entry("k", "AND c.type = 'KAWAIPON'"),
				Map.entry("e", "AND c.type = 'EVOGEAR'"),
				Map.entry("f", "AND c.type = 'FIELD'"),
				Map.entry("v", "AND c.deck IS NULL")
		);

		XStringBuilder query = new XStringBuilder("""
				SELECT c FROM StashedCard c
				LEFT JOIN KawaiponCard kc ON kc.uuid = c.uuid
				LEFT JOIN Evogear e ON e.card = c.card
				WHERE c.kawaipon.uid = ?1
				""");
		List<Object> params = new ArrayList<>();
		params.add(event.user().getId());

		AtomicInteger i = new AtomicInteger(2);
		Option[] opts = cli.getFirst().getOptions();
		for (Option opt : opts) {
			String filter = filters.get(opt.getOpt());
			if (filter.contains("%s")) {
				filter = filter.formatted(i.getAndIncrement());
			}

			query.appendNewLine(filter);

			if (opt.hasArg()) {
				params.add(opt.getValue().toUpperCase());
			}
		}

		query.appendNewLine("""
				ORDER BY c.card.anime
						, COALESCE(
							e.tier,
					   		CASE c.card.rarity
					      		WHEN 'COMMON' THEN 1
					      		WHEN 'UNCOMMON' THEN 1.5
					      		WHEN 'RARE' THEN 2
					      		WHEN 'ULTRA_RARE' THEN 2.5
					      		WHEN 'LEGENDARY' THEN 3
				    	  		ELSE 1
				       		END
				       	) DESC
				       	, c.type
					   	, c.card.id
				""");

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		ThrowingFunction<Integer, Page> loader = p -> {
			List<StashedCard> results = DAO.queryBuilder(
					StashedCard.class,
					query.toString(),
					q -> q.setFirstResult(p * 10).setMaxResults(10).getResultList(),
					params.toArray()
			);

			eb.setAuthor(locale.get("str/search_result_stash", results.size(), kp.getStashUsage(), kp.getMaxCapacity()));
			return Utils.generatePage(eb, results, 5, sc -> new StashItem(locale, sc).toString());
		};

		if (loader.apply(0) == null) {
			event.channel().sendMessage(locale.get("error/no_result")).queue();
			return;
		}

		Utils.paginate(loader, event.channel(), event.user());
	}
}
