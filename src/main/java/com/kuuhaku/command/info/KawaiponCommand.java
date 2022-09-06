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

package com.kuuhaku.command.info;

import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Command(
		name = "kawaipon",
		category = Category.INFO
)
@Signature("<anime:text> <kind:word>[n,c]")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class KawaiponCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = data.profile().getAccount().getKawaipon();

		if (!args.has("anime")) {
			int total = DAO.queryNative(Integer.class, "SELECT SUM(count) FROM aux.card_counter");
			Pair<Integer, Integer> count = kp.countCards();
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/kawaipon_collection", event.user().getName()))
					.setFooter(locale.get("str/owned_cards",
							Calc.prcntToInt(count.getFirst() + count.getSecond(), total * 2),
							Calc.prcntToInt(count.getFirst(), total),
							Calc.prcntToInt(count.getSecond(), total)
					));

			List<Page> pages = new ArrayList<>();
			int max = (int) Math.ceil(total / 50d);
			for (int i = 1; i <= max; i++) {
				eb.setImage((Constants.API_ROOT + "kawaipon/%s/%s?page=%s").formatted(
						locale, kp.getUid(), i
				));
				pages.add(new InteractPage(eb.build()));
			}

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		Anime anime = DAO.find(Anime.class, args.getString("anime").toUpperCase(Locale.ROOT));
		if (anime == null || !anime.isVisible()) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM anime WHERE visible");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("anime").toUpperCase(Locale.ROOT), names);
			event.channel().sendMessage(locale.get("error/unknown_anime", sug.getFirst())).queue();
			return;
		}

		int total = anime.getCount();
		Pair<Integer, Integer> count = kp.countCards(anime);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/kawaipon_collection", event.user().getName()))
				.setFooter(locale.get("str/owned_cards",
						Calc.prcntToInt(count.getFirst() + count.getSecond(), total * 2),
						Calc.prcntToInt(count.getFirst(), total),
						Calc.prcntToInt(count.getSecond(), total)
				));

		List<Page> pages = new ArrayList<>();
		int max = (int) Math.ceil(total / 50d);
		for (int i = 1; i <= max; i++) {
			eb.setImage((Constants.API_ROOT + "kawaipon/%s/%s?anime=%s&type=%s&page=%s").formatted(
				locale, kp.getUid(), anime.getId(), i, args.getString("kind", "n")
			));
			pages.add(new InteractPage(eb.build()));
		}

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
