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

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.validator.routines.UrlValidator;

import java.awt.image.BufferedImage;

@Command(
		name = "hero",
		path = "image",
		category = Category.MISC
)
@Syntax(allowEmpty = true, value = "<url:text:r>")
public class HeroImageCommand implements Executable {
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
		} else if (url.length() > 255) {
			event.channel().sendMessage(locale.get("error/url_too_long")).queue();
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
		}

		h.setImage(IO.getImage(url));

		event.channel().sendMessage(locale.get("success/hero_image", h.getName())).queue();
	}
}
