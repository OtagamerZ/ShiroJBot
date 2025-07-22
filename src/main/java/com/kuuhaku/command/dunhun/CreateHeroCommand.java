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
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;

@Command(
		name = "hero",
		path = "new",
		category = Category.DEV
)
@Syntax("<name:word:r> <race:word:r> <url:text>")
public class CreateHeroCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		} else if (d.getHero(locale) != null) {
			event.channel().sendMessage(locale.get("error/has_hero", data.config().getPrefix())).queue();
			return;
		}

		String url = args.getString("url");
		if (url.isBlank()) {
			if (!event.message().getAttachments().isEmpty()) {
				for (Message.Attachment att : event.message().getAttachments()) {
					if (att.isImage()) {
						url = att.getUrl();
						break;
					}
				}
			}
		}

		if (url.isBlank()) {
			event.channel().sendMessage(locale.get("error/image_required")).queue();
			return;
		} else if (!UrlValidator.getInstance().isValid(url)) {
			event.channel().sendMessage(locale.get("error/invalid_url")).queue();
			return;
		}

		long size = IO.getImageSize(url);
		if (size == 0) {
			event.channel().sendMessage(locale.get("error/invalid_url")).queue();
			return;
		} else if (size > Hero.MAX_IMG_SIZE) {
			event.channel().sendMessage(locale.get("error/image_too_big", (Hero.MAX_IMG_SIZE / 1024 / 1024) + " MB")).queue();
			return;
		} else if (!ImageIO.getImageReadersByFormatName(Utils.getOr(IO.getImageType(url), "")).hasNext()) {
			event.channel().sendMessage(locale.get("error/format_not_supported")).queue();
			return;
		}

		String name = StringUtils.stripAccents(args.getString("name").toUpperCase());
		if (name.length() > 20) {
			event.channel().sendMessage(locale.get("error/name_too_long", 20)).queue();
			return;
		} else if (DAO.find(Hero.class, name) != null) {
			event.channel().sendMessage(locale.get("error/name_exists")).queue();
			return;
		}

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

		try {
			Hero h = new Hero(data.profile().getAccount(), name, race);

			String finalUrl = url;
			Utils.confirm(locale.get("question/hero_creation", h.getName()), event.channel(), w -> {
						BufferedImage img = IO.getImage(finalUrl);
						h.setImage(img);
						h.save();

						event.channel().sendMessage(locale.get("success/hero_created"))
								.addFiles(FileUpload.fromData(IO.getBytes(h.render(locale), "png"), "hero.png"))
								.queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
