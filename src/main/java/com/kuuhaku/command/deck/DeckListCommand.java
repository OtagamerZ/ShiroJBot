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

package com.kuuhaku.command.deck;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "deck",
		subname = "list",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class DeckListCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Origin o = d.getOrigins();
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setDescription("%s\n%s %s".formatted(
						locale.get("str/major_effect"),
						Utils.getEmoteString(Constants.EMOTE_REPO_4, o.major().name()),
						o.major().getMajor(locale)
				));

		Race[] minors = o.minor();
		if (minors.length == 0) {
			eb.appendDescription("\n%s\n%s".formatted(
					locale.get("str/minor_effect"), Race.NONE.getMinor(locale)
			));
		} else {
			eb.appendDescription("\n" + locale.get("str/minor_effect"));
			for (Race r : minors) {
				eb.appendDescription("\n" + Utils.getEmoteString(Constants.EMOTE_REPO_4, r.name()) + " " + r.getMinor(locale));
			}
		}

		Race syn = o.synergy();
		if (syn != Race.NONE) {
			eb.appendDescription("\n%s\n%s %s".formatted(
					locale.get("str/synergy_effect"),
					Utils.getEmoteString(Constants.EMOTE_REPO_4, syn.name()),
					syn.getSynergy(locale)
			));
		}

		Page home;
		Map<Emoji, Page> pages = new LinkedHashMap<>();
		pages.put(Utils.parseEmoji("⚔️"), home = Utils.generatePage(eb, Utils.padList(d.getSenshi(), 36), 12,
				s -> {
					eb.setTitle(locale.get("str/deck_title", event.member().getEffectiveName(), locale.get("type/senshi")));
					if (s == null) return "*" + locale.get("str/empty") + "*";

					return Utils.getEmoteString(Constants.EMOTE_REPO_4, s.getRace().name()) + " " + s;
				}
		));
		pages.put(Utils.parseEmoji("\uD83D\uDEE1️"), Utils.generatePage(eb, Utils.padList(d.getEvogear(), 24), 12,
				e -> {
					eb.setTitle(locale.get("str/deck_title", event.member().getEffectiveName(), locale.get("type/evogear")));
					if (e == null) return "*" + locale.get("str/empty") + "*";

					return Utils.getEmoteString(Constants.EMOTE_REPO_4, "tier_" + e.getTier()) + " " + e;
				}
		));
		pages.put(Utils.parseEmoji("\uD83C\uDFD4️"), Utils.generatePage(eb, Utils.padList(d.getFields(), 3), 12,
				f -> {
					eb.setTitle(locale.get("str/deck_title", event.member().getEffectiveName(), locale.get("type/field")));
					if (f == null) return "*" + locale.get("str/empty") + "*";

					return switch (f.getType()) {
						case NONE -> f.toString();
						case DAY -> ":sunny: " + f;
						case NIGHT -> ":crescent_moon: " + f;
						case DUNGEON -> ":japanese_castle: " + f;
					};
				}
		));

		assert home != null;
		event.channel().sendMessageEmbeds((MessageEmbed) home.getContent()).queue(s ->
				Pages.categorize(s, pages, true, 1, TimeUnit.MINUTES, u -> u.equals(event.user()))
		);
	}
}