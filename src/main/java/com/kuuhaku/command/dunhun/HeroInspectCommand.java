/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.dunhun.GearStats;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.stream.Collectors;

@Command(
		name = "hero",
		path = "inspect",
		category = Category.STAFF
)
@Syntax("<id:number:r>")
public class HeroInspectCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero();
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		Gear g = DAO.find(Gear.class, args.getInt("id"));
		if (g == null || !g.getOwner().equals(h)) {
			event.channel().sendMessage(locale.get("error/gear_not_found")).queue();
			return;
		}

		g.load(locale, null);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(g.getName(locale));

		JSONArray tags = g.getTags();
		if (!tags.isEmpty()) {
			String tgs = tags.stream().map(t -> {
				try {
					String key = String.valueOf(t);
					String out = locale.get(key, args);
					if (out.isBlank() || out.equalsIgnoreCase(key)) {
						out = LocalizedString.get(locale, key, "");
					}

					return Utils.getOr(out, key);
				} catch (MissingFormatArgumentException e) {
					return "";
				}
			}).collect(Collectors.joining(", "));

			eb.appendDescription("-# " + tgs + "\n\n");
		}

		GearStats stats = g.getBasetype().getStats();
		if (g.getDmg() != 0) {
			eb.appendDescription(locale.get("str/attack") + ": " + g.getDmg() + "\n");
		}
		if (g.getDfs() != 0) {
			eb.appendDescription(locale.get("str/defense") + ": " + g.getDfs() + "\n");
		}
		if (g.getCritical() != 0) {
			eb.appendDescription(locale.get("str/critical_chance") + ": " + Utils.roundToString(g.getCritical(), 2) + "%\n");
		}

		eb.appendDescription("\n");

		if (stats.str() + stats.dex() + stats.wis() + stats.vit() > 0) {
			eb.appendDescription("-# " + locale.get("str/required_attributes") + "\n");
		}

		List<String> attrs = new ArrayList<>();
		if (stats.str() > 0) attrs.add("STR: " + stats.str() + " ");
		if (stats.dex() > 0) attrs.add("DEX: " + stats.dex() + " ");
		if (stats.wis() > 0) attrs.add("WIS: " + stats.wis() + " ");
		if (stats.vit() > 0) attrs.add("VIT: " + stats.vit() + " ");

		if (!attrs.isEmpty()) {
			eb.appendDescription(String.join(" | ", attrs) + "\n\n");
		}

		GearAffix imp = g.getImplicit();
		if (imp != null) {
			eb.appendDescription("-# " + locale.get("str/implicit") + "\n");
			eb.appendDescription(imp.getDescription(locale, true) + "\n");
			if (!g.getAffixes().isEmpty()) {
				eb.appendDescription("────────────────────\n");
			}
		}

		List<GearAffix> affs = g.getAffixes().stream()
				.sorted(Comparator
						.<GearAffix, Boolean>comparing(ga -> ga.getAffix().getType() == AffixType.SUFFIX, Boolean::compareTo)
						.thenComparing(ga -> ga.getAffix().getId())
				)
				.toList();

		for (GearAffix ga : affs) {
			eb.appendDescription("-# %s - %s\n".formatted(
					locale.get("str/" + ga.getAffix().getType().name()), ga.getName(locale)
			));
			eb.appendDescription(ga.getDescription(locale, true) + "\n\n");
		}

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}
}
