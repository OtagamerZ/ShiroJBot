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
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "kawaipon",
		subname = "senshi",
		category = Category.INFO
)
@Signature("<race:word>")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class KawaiponSenshiCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		if (!args.has("race")) {
			int total = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM senshi");

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/available_cards", locale.get("type/senshi")))
					.setTitle(locale.get("type/senshi"));

			List<Page> pages = new ArrayList<>();
			int max = (int) Math.ceil(total / 50d);
			for (int i = 1; i <= max; i++) {
				String url = (Constants.API_ROOT + "shoukan/%s/senshi?uid=%s&v=%s&page=%s").formatted(
						locale, event.user().getId(), System.currentTimeMillis(), i
				);

				eb.setImage(url).setDescription(locale.get("str/fallback_url", url));
				pages.add(new InteractPage(eb.build()));
			}

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		Race race = args.getEnum(Race.class, "race");
		if (race == null) {
			Pair<String, Double> sug = Utils.didYouMean(args.getString("race"), Arrays.stream(Race.values()).map(Race::name).toList());
			event.channel().sendMessage(locale.get("error/unknown_race", sug.getFirst())).queue();
			return;
		}

		int total = race.getCount();
		if (total == 0) {
			event.channel().sendMessage(locale.get("error/empty_race")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/available_cards", locale.get("type/senshi")))
				.setThumbnail(Constants.ORIGIN_RESOURCES + "shoukan/race/full/" + race + ".png")
				.setTitle(race.getName(locale) + " (`" + race.name() + "`)");

		if (Integer.bitCount(race.getFlag()) == 1) {
			eb.addField(locale.get("str/sub_races"), Utils.properlyJoin(locale.get("str/and")).apply(Arrays.stream(race.derivates()).map(r -> r.getName(locale)).toList()), false)
					.addField(locale.get("str/major_effect"), race.getMajor(locale), false)
					.addField(locale.get("str/minor_effect"), race.getMinor(locale), false);
		} else {
			eb.addField(locale.get("str/origins"), Arrays.stream(race.split()).map(r -> r.getName(locale)).collect(Collectors.joining(" + ")), false)
					.addField(locale.get("str/synergy_effect"), race.getSynergy(locale), false);
		}

		List<Page> pages = new ArrayList<>();
		int max = (int) Math.ceil(total / 50d);
		for (int i = 1; i <= max; i++) {
			String url = (Constants.API_ROOT + "shoukan/%s/senshi?race=%s&uid=%s&v=%s&page=%s").formatted(
					locale, race.getFlag(), event.user().getId(), System.currentTimeMillis(), i
			);

			eb.setImage(url).setDescription(race.getDescription(locale) + "\n\n" + locale.get("str/fallback_url", url));
			pages.add(new InteractPage(eb.build()));
		}

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}