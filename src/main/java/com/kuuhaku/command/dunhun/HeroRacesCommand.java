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

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.RaceBonus;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.dunhun.RaceValues;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

@Command(
		name = "hero",
		path = "races",
		category = Category.MISC
)
@Syntax("<race:word>")
public class HeroRacesCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

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

			RaceValues bonus = DAO.find(RaceBonus.class, race).getValues();

			XStringBuilder sb = new XStringBuilder();
			if (bonus.hp() != 0) sb.appendNewLine(locale.get("str/bonus_hp", Utils.sign(bonus.hp())));
			if (bonus.attack() != 0) sb.appendNewLine(locale.get("str/bonus_attack", Utils.sign(bonus.attack())));
			if (bonus.defense() != 0) sb.appendNewLine(locale.get("str/bonus_defense", Utils.sign(bonus.defense())));
			if (bonus.dodge() != 0) sb.appendNewLine(locale.get("str/bonus_dodge", Utils.sign(bonus.dodge())));
			if (bonus.parry() != 0) sb.appendNewLine(locale.get("str/bonus_parry", Utils.sign(bonus.parry())));
			if (bonus.critical() != 0) sb.appendNewLine(locale.get("str/bonus_critical", Utils.sign(bonus.critical())));
			if (bonus.power() != 0) sb.appendNewLine(locale.get("str/bonus_power", Utils.sign(bonus.power())));

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/race_bonus") + " (" + locale.get("race/" + race.name()) + ")")
					.setDescription(sb.toString());

			event.channel().sendMessageEmbeds(eb.build()).queue();
			return;
		}

		List<RaceBonus> bonuses = Arrays.stream(Race.validValues())
				.map(r -> DAO.find(RaceBonus.class, r))
				.toList();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/race_bonus"));

		List<Page> pages = Utils.generatePages(eb, bonuses, 10, 5, r -> {
			RaceValues bonus = r.getValues();
			XStringBuilder sb = new XStringBuilder();
			if (bonus.hp() != 0) sb.appendNewLine(locale.get("str/bonus_hp", Utils.sign(bonus.hp())));
			if (bonus.attack() != 0) sb.appendNewLine(locale.get("str/bonus_attack", Utils.sign(bonus.attack())));
			if (bonus.defense() != 0) sb.appendNewLine(locale.get("str/bonus_defense", Utils.sign(bonus.defense())));
			if (bonus.dodge() != 0) sb.appendNewLine(locale.get("str/bonus_dodge", Utils.sign(bonus.dodge())));
			if (bonus.parry() != 0) sb.appendNewLine(locale.get("str/bonus_parry", Utils.sign(bonus.parry())));
			if (bonus.critical() != 0) sb.appendNewLine(locale.get("str/bonus_critical", Utils.sign(bonus.critical())));
			if (bonus.power() != 0) sb.appendNewLine(locale.get("str/bonus_power", Utils.sign(bonus.power())));

			return new FieldMimic(
					locale.get("race/" + r.getId().name()),
					bonus.toString()
			).toString();
		});

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
