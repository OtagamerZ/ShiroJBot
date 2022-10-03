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

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.gacha.*;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Command(
		name = "gacha",
		subname = "pool",
		category = Category.INFO
)
@Signature("<type:word:r>[basic,premium,summoner,daily]")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class GachaPoolCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		String type = args.getString("type");
		Gacha<String> gacha = switch (type) {
			case "premium" -> new PremiumGacha();
			case "summoner" -> new SummonersGacha();
			case "daily" -> new DailyGacha();
			default -> new BasicGacha();
		};

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/gacha_pool", locale.get("gacha/" + type).toLowerCase()));

		List<Card> pool = new ArrayList<>(DAO.queryAll(Card.class, "SELECT c FROM Card c WHERE id IN ?1", gacha.getPool()));
		pool.sort(
				Comparator.<Card>comparingDouble(c -> gacha.rarityOf(c.getId()))
						.thenComparing(Card::getRarity, Comparator.reverseOrder())
						.thenComparing(Card::getId)
		);

		List<Page> pages = Utils.generatePages(eb, pool, 20, 10,
				c -> {
					String name = c.getName();

					return c.getRarity().getEmote() + name;
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}