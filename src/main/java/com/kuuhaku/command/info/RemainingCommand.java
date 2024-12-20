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

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;

@Command(
		name = "remaining",
		category = Category.INFO
)
@Syntax("<anime:word:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class RemainingCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = data.profile().getAccount().getKawaipon();

		Anime anime = DAO.find(Anime.class, args.getString("anime").toUpperCase());
		if (anime == null || !anime.isVisible()) {
			String sug = Utils.didYouMean(args.getString("anime"), "SELECT id AS value FROM anime WHERE visible");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_anime_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_anime", sug)).queue();
			}
			return;
		}

		int total = anime.getCount();
		Pair<Integer, Integer> count = kp.countCards(anime);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/anime_cards", anime));

		String footer = locale.get("str/owned_cards",
				Calc.prcntToInt(count.getFirst() + count.getSecond(), total * 2),
				Calc.prcntToInt(count.getFirst(), total),
				Calc.prcntToInt(count.getSecond(), total)
		);

		List<Page> pages = Utils.generatePages(eb, anime.getCards(), 20, 10,
				c -> {
					String name = c.getName();

					String suffix = "";
					if (kp.hasCard(c, false)) {
						suffix += "N";
					}
					if (kp.hasCard(c, true)) {
						suffix += "C";
					}

					if (!suffix.isBlank()) {
						return "||" + c.getRarity().getEmote(c) + name + " **(" + suffix + ")**" + "||";
					} else {
						return c.getRarity().getEmote(c) + name;
					}
				},
				(p, t) -> eb.setFooter(footer + "\n" + locale.get("str/page", p + 1, t))
		);

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
