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
import com.kuuhaku.interfaces.annotations.GachaType;
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
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Command(
		name = "gacha",
		path = "pool",
		category = Category.INFO
)
@Signature("<type:word:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class GachaPoolCommand implements Executable {
	@Override
	@SuppressWarnings("unchecked")
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		String id = args.getString("type");
		Class<? extends Gacha> chosen = null;
		Set<String> types = new HashSet<>();
		for (Class<?> gacha : Gacha.getGachas()) {
			GachaType type = gacha.getAnnotation(GachaType.class);
			if (type.value().equalsIgnoreCase(id)) {
				chosen = (Class<? extends Gacha>) gacha;
				break;
			}

			types.add(type.value().toUpperCase());
		}

		if (chosen == null) {
			Pair<String, Double> sug = Utils.didYouMean(id.toUpperCase(), types);
			event.channel().sendMessage(locale.get("error/unknown_gacha", sug.getFirst())).queue();
			return;
		}

		GachaType type = chosen.getAnnotation(GachaType.class);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/gacha_pool", locale.get("gacha/" + type.value()).toLowerCase()));

		try {
			Gacha gacha = chosen.getConstructor(User.class).newInstance(event.user());
			List<Card> pool = new ArrayList<>(DAO.queryAll(Card.class, "SELECT c FROM Card c WHERE id IN ?1", gacha.getPool()));
			pool.sort(
					Comparator.<Card>comparingDouble(c -> gacha.rarityOf(c.getId()))
							.thenComparing(Card::getRarity, Comparator.reverseOrder())
							.thenComparing(Card::getId)
			);

			List<Page> pages = Utils.generatePages(eb, pool, 20, 10,
					c -> {
						String name = c.getName();

						return c.getRarity().getEmote(c) + name;
					},
					(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
			);

			Utils.paginate(pages, 1, true, event.channel(), event.user());
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
}