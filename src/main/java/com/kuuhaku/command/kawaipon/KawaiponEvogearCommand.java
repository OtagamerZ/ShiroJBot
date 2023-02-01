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

package com.kuuhaku.command.kawaipon;

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
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.List;

@Command(
		name = "kawaipon",
		subname = "evogear",
		category = Category.INFO
)
@Signature("<tier:number>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class KawaiponEvogearCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		if (!args.has("tier")) {
			int total = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM evogear WHERE tier > 0");

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/available_cards", locale.get("type/evogear")))
					.setTitle(locale.get("type/evogear"));

			List<Page> pages = new ArrayList<>();
			int max = (int) Math.ceil(total / 50d);
			for (int i = 1; i <= max; i++) {
				String url = (Constants.API_ROOT + "shoukan/%s/evogear?uid=%s&v=%s&page=%s").formatted(
						locale, event.user().getId(), System.currentTimeMillis(), i
				);

				eb.setImage(url).setDescription(locale.get("str/fallback_url", url));
				pages.add(new InteractPage(eb.build()));
			}

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		int tier = args.getInt("tier");
		int total = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM evogear WHERE tier = ?1 AND tier > 0", tier);
		if (total == 0) {
			event.channel().sendMessage(locale.get("error/empty_tier")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/available_cards", locale.get("type/evogear")))
				.setThumbnail(Constants.ORIGIN_RESOURCES + "shoukan/icons/tier_" + tier + "_full.png");

		List<Page> pages = new ArrayList<>();
		int max = (int) Math.ceil(total / 50d);
		for (int i = 1; i <= max; i++) {
			String url = (Constants.API_ROOT + "shoukan/%s/evogear?tier=%s&uid=%s&v=%s&page=%s").formatted(
					locale, tier, event.user().getId(), System.currentTimeMillis(), i
			);

			eb.setImage(url).setDescription(locale.get("str/fallback_url", url));
			pages.add(new InteractPage(eb.build()));
		}

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
