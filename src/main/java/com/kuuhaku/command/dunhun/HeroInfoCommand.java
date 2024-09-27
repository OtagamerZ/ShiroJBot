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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.helper.CategorizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.Equipment;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.Attributes;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "hero",
		path = "info",
		category = Category.STAFF
)
public class HeroInfoCommand implements Executable {
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

		Senshi card = h.asSenshi(locale);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/hero_info", h.getName()))
				.setImage("attachment://card.png");

		eb.addField(Constants.VOID, """
				HP: %s (%s)
				%s (%s/%s)
				""".formatted(
				h.getMaxHp(), Utils.sign(h.getModifiers().getMaxHp()),
				locale.get("str/level", h.getStats().getLevel()),
				h.getStats().getXp(), h.getStats().getXpToNext()
		), true);

		Attributes attr = h.getAttributes();
		eb.addField(Constants.VOID, """
				STR: %s (%s)
				DEX: %s (%s)
				WIS: %s (%s)
				VIT: %s (%s)
				""".formatted(
				attr.str(), Utils.sign(h.getModifiers().getStrength()),
				attr.dex(), Utils.sign(h.getModifiers().getDexterity()),
				attr.wis(), Utils.sign(h.getModifiers().getWisdom()),
				attr.vit(), Utils.sign(h.getModifiers().getVitality())
		), true);

		Map<Emoji, Page> pages = new LinkedHashMap<>();
		pages.put(Utils.parseEmoji("📋"), InteractPage.of(eb.build()));
		pages.put(Utils.parseEmoji("📖"), viewSkills(locale, h));
		pages.put(Utils.parseEmoji("🛡"), viewGear(locale, h));

		CategorizeHelper helper = new CategorizeHelper(pages, true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(event.user()::equals);

		helper.apply(event.channel().sendMessageEmbeds(eb.build()))
				.addFiles(FileUpload.fromData(IO.getBytes(card.render(locale, d), "png"), "card.png"))
				.queue(s -> Pages.categorize(s, helper));
	}

	private Page viewGear(I18N locale, Hero h) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/equipment"))
				.setThumbnail("attachment://card.png");

		Equipment equips = h.getEquipment();
		XStringBuilder sb = new XStringBuilder();
		for (GearSlot gs : GearSlot.values()) {
			equips.withSlot(gs, g -> {
				if (g == null) {
					sb.appendNewLine("*" + locale.get("str/empty") + "*");
				} else {
					sb.appendNewLine("`" + g.getId() + "` - " + g.getName(locale));

					GearAffix imp = g.getImplicit();
					if (imp != null) {
						sb.appendNewLine("-# " + imp.getDescription(locale));
						sb.appendNewLine("-# ────────────────────");
					}

					for (String l : g.getAffixLines(locale)) {
						sb.appendNewLine("-# " + l);
					}
				}

				return g;
			});

			eb.addField(locale.get("str/" + gs.name()), sb.toString(), true);
			sb.clear();
		}

		return InteractPage.of(eb.build());
	}

	private Page viewSkills(I18N locale, Hero h) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/skills"))
				.setThumbnail("attachment://card.png");

		List<Skill> skills = h.getSkills();
		for (int i = 0; i < 5; i++) {
			if (skills.size() <= i) {
				eb.appendDescription("*" + locale.get("str/empty") + "*\n\n");
				continue;
			}

			Skill s = skills.get(i);
			eb.appendDescription(s.getInfo(locale).getName() + "\n");
			s.getInfo(locale).getDescription().lines()
					.map(l -> "-# " + l + "\n")
					.forEach(eb::appendDescription);

			eb.appendDescription("\n");
		}

		return InteractPage.of(eb.build());
	}
}
