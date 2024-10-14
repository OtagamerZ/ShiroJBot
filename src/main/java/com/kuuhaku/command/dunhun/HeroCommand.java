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
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.Equipment;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AttrType;
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Command(
		name = "hero",
		category = Category.STAFF
)
public class HeroCommand implements Executable {
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
				AP: %s (%s)
				%s (%s/%s)
				""".formatted(
				h.getMaxHp(), Utils.sign((int) h.getModifiers().getMaxHp().get()),
				h.getMaxAp(), Utils.sign((int) h.getModifiers().getMaxAp().get()),
				locale.get("str/level", h.getStats().getLevel()),
				h.getStats().getXp(), h.getStats().getXpToNext()
		), true);

		Attributes attr = h.getAttributes();
		Attributes mods = h.getModifiers().getAttributes();
		eb.addField(Constants.VOID, """
				STR: %s (%s)
				DEX: %s (%s)
				WIS: %s (%s)
				VIT: %s (%s)
				""".formatted(
				attr.str(), Utils.sign(mods.str()),
				attr.dex(), Utils.sign(mods.dex()),
				attr.wis(), Utils.sign(mods.wis()),
				attr.vit(), Utils.sign(mods.vit())
		), true);

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(event.user()::equals)
				.addAction(Utils.parseEmoji("📋"),
						w -> updatePage(InteractPage.of(eb.build()), w.getMessage())
				)
				.addAction(Utils.parseEmoji("📖"),
						w -> updatePage(viewSkills(locale, h), w.getMessage())
				)
				.addAction(Utils.parseEmoji("🛡"),
						w -> updatePage(viewGear(locale, h), w.getMessage())
				)
				.addAction(Utils.parseEmoji("🧮"),
						w -> allocAttributes(locale, h, w.getMessage())
				);

		helper.apply(event.channel().sendMessageEmbeds(eb.build()))
				.addFiles(FileUpload.fromData(IO.getBytes(card.render(locale, d), "png"), "card.png"))
				.queue(s -> Pages.buttonize(s, helper));
	}

	private void updatePage(Page p, Message msg) {
		msg.editMessageEmbeds((MessageEmbed) p.getContent()).queue();
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
						imp.getDescription(locale).lines()
								.map(l -> "-# " + l)
								.forEach(sb::appendNewLine);
						sb.appendNewLine("-# ──────────────────");
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
			s.getDescription(locale, h).lines()
					.map(l -> "-# " + l + "\n")
					.forEach(eb::appendDescription);

			eb.appendDescription("\n");
		}

		return InteractPage.of(eb.build());
	}

	private void allocAttributes(I18N locale, Hero h, Message msg) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/attributes"))
				.setThumbnail("attachment://card.png");

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()));

		int[] attr = new int[4];
		Attributes alloc = h.getStats().getAttributes();
		Supplier<Integer> remaining = () -> Math.max(0, h.getStats().getPointsLeft() - (attr[0] + attr[1] + attr[2] + attr[3]));

		Consumer<BiConsumer<Character, Integer>> updateDesc = func -> {
			eb.clearFields();
			eb.setDescription(locale.get("str/remaining_points", remaining.get()));

			int i = 0;
			XStringBuilder sb = new XStringBuilder();
			for (AttrType at : AttrType.values()) {
				String name = locale.get("attr/" + at.name());

				int idx = i++;
				sb.appendNewLine("**" + name.charAt(0) + "**" + name.substring(1) + ": " + (alloc.get(at) + attr[idx]));
				sb.appendNewLine("-# " + locale.get("attr/" + at.name() + "_desc"));
				sb.nextLine();
				if (func != null) func.accept(name.charAt(0), idx);
			}

			eb.addField(Constants.VOID, sb.toString(), true);
			msg.editMessageEmbeds(eb.build()).queue();
		};

		updateDesc.accept((ch, i) -> helper.addAction(Utils.parseEmoji(Utils.fancyLetter(ch)), w -> {
			if (remaining.get() <= 0) return;

			attr[i]++;
			updateDesc.accept(null);
		}));

		helper.addAction(Utils.parseEmoji("✅"), w -> {
			h.getStats().setAttributes(alloc.merge(new Attributes(attr[0], attr[1], attr[2], attr[3])));
			h.save();

			msg.getChannel().sendMessage(locale.get("success/points_allocated"))
					.flatMap(ms -> w.getMessage().delete())
					.queue();
		});

		helper.apply(msg.editMessageEmbeds(eb.build())).queue(s -> Pages.buttonize(s, helper));
	}
}
