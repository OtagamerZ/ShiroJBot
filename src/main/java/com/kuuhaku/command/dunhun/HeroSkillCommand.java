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
import com.kuuhaku.model.enums.dunhun.AttrType;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Command(
		name = "hero",
		path = "skill",
		category = Category.STAFF
)
@Syntax("<skill:word:r>")
public class HeroSkillCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero(locale);
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		Skill s = DAO.find(Skill.class, args.getString("skill").toUpperCase());
		if (s == null) {
			String sug = Utils.didYouMean(args.getString("skill"), "SELECT id AS value FROM skill");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_skill_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_skill", sug)).queue();
			}
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setThumbnail("attachment://thumb.png");

		List<String> tags = new ArrayList<>();
		tags.add(locale.get("str/type_" + (s.getStats().isSpell() ? "spell" : "martial")));
		tags.addAll(s.getRequirements().tags().stream()
				.map(t -> LocalizedString.get(locale, "tag/" + t, "???"))
				.toList());

		eb.setTitle(s.getName(locale))
				.appendDescription("-# " + String.join(", ", tags) + "\n");

		if (s.getStats().getCost() > 0) {
			eb.appendDescription("-# " + locale.get("str/cost", StringUtils.repeat('◈', s.getStats().getCost())) + "\n");
		}

		if (s.getStats().getReservation() > 0) {
			eb.appendDescription("-# " + locale.get("str/reservation", "~~" + StringUtils.repeat('◇', s.getStats().getReservation()) + "~~") + "\n");
		}

		if (s.getStats().getCooldown() > 0) {
			eb.appendDescription("-# " + locale.get("str/cooldown", locale.get("str/turns_inline", s.getStats().getCooldown())) + "\n");
		}

		if (s.getStats().getEfficiency() > 0) {
			String eff = Utils.roundToString(s.getStats().getEfficiency() * 100, 0) + "%";
			eb.appendDescription("-# " + locale.get("str/added_efficiency", eff) + "\n");
		}

		if (s.getStats().getCritical() > 0) {
			double crit = h.getModifiers().getCritical(s.getStats().getCritical());
			String text = "**" + Utils.roundToString(crit, 1) + "**";
			eb.appendDescription("-# " + locale.get("str/bonus_critical", text) + "\n");
		}

		Attributes reqs = s.getRequirements().attributes();
		if (reqs.str() + reqs.dex() + reqs.wis() + reqs.vit() > 0) {
			eb.appendDescription("-# " + locale.get("str/required_attributes") + "\n");
		}

		List<String> attrs = new ArrayList<>();
		if (s.getRequirements().level() > 0) attrs.add(locale.get("str/level", s.getRequirements().level()));

		for (AttrType t : AttrType.values()) {
			if (t.ordinal() >= AttrType.LVL.ordinal()) break;

			if (reqs.get(t) > 0) attrs.add(t + ": " + reqs.get(t) + " ");
		}

		if (!attrs.isEmpty()) {
			eb.appendDescription(String.join(" | ", attrs) + "\n");
		}

		eb.appendDescription("\n" + s.getDescription(locale, h));

		MessageCreateAction ma = event.channel().sendMessageEmbeds(eb.build());

		File icon = IO.getResourceAsFile("dunhun/icons/type_" + (s.getStats().isSpell() ? "spell" : "martial") + ".png");
		if (icon != null) {
			ma.addFiles(FileUpload.fromData(icon, "thumb.png"));
		}

		ma.queue();
	}
}
