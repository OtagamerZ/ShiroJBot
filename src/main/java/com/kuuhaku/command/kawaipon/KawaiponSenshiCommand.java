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

package com.kuuhaku.command.kawaipon;

import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.http.client.utils.URIBuilder;

import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Command(
		name = "kawaipon",
		path = "senshi",
		category = Category.INFO
)
@Signature("<race:word> <pure:word>[pure]")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class KawaiponSenshiCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck dk = data.profile().getAccount().getCurrentDeck();
		try {
			URIBuilder ub = new URIBuilder(Constants.API_ROOT + "shoukan/" + locale.name() + "/senshi")
					.setParameter("user", event.user().getId())
					.setParameter("frame", dk.getStyling().getFrame().name());

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/available_cards", locale.get("type/senshi")))
					.setTitle(locale.get("type/senshi"))
					.setImage("attachment://cards.jpg");

			AtomicReference<String> baseDesc = new AtomicReference<>("");

			if (args.has("race")) {
				Race race = args.getEnum(Race.class, "race");
				if (!Utils.equalsAny(race, Race.validValues())) {
					String sug = Utils.didYouMean(args.getString("race"), Arrays.stream(Race.validValues()).map(Race::name).toList());
					if (sug == null) {
						event.channel().sendMessage(locale.get("error/unknown_race_none")).queue();
					} else {
						event.channel().sendMessage(locale.get("error/unknown_race", sug)).queue();
					}
					return;
				}

				baseDesc.set(race.getDescription(locale) + "\n\n");

				boolean variant = race != Race.getByFlag(race.getFlag());
				eb.setThumbnail(Constants.ORIGIN_RESOURCES + "shoukan/race/full/" + race + ".png")
						.setTitle(race.getName(locale) + " (`" + race.name() + "`)");

				if (Integer.bitCount(race.getFlag()) == 1) {
					eb.addField(locale.get("str/sub_races"), Utils.properlyJoin(locale.get("str/and")).apply(Arrays.stream(race.derivates()).map(r -> r.getName(locale)).toList()), false)
							.addField(locale.get("str/major_effect"), race.getMajor(locale), false)
							.addField(locale.get("str/minor_effect"), race.getMinor(locale), false);
				} else {
					eb.addField(locale.get("str/origins"), race.split().stream().map(r -> r.getName(locale)).collect(Collectors.joining(" + ")), false)
							.addField(locale.get("str/synergy_effect"), race.getSynergy(locale), false);
				}

				ub.setParameter("race", String.valueOf(race.getFlag()))
						.setParameter("variant", variant ? "1" : "0")
						.setParameter("pure", args.has("pure") ? "1" : "0");
			}

			AtomicReference<Message> msg = new AtomicReference<>();
			ThrowingFunction<Integer, Page> loader = i -> {
				ub.setParameter("page", String.valueOf(i));

				try {
					String url = ub.build().toString();
					eb.setDescription(baseDesc.get() + locale.get("str/fallback_url", url));

					if (msg.get() != null) {
						BufferedImage img = IO.getImage(url);
						if (img == null) return null;

						msg.get().editMessageAttachments(FileUpload.fromData(IO.getBytes(img), "cards.jpg")).queue();
					}

					return InteractPage.of(eb.build());
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			};

			msg.set(Utils.paginate(loader, event.channel(), event.user()));

			BufferedImage img = IO.getImage(ub.build().toString());
			if (img != null) {
				msg.get().editMessageAttachments(FileUpload.fromData(IO.getBytes(img), "cards.jpg")).queue();
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
