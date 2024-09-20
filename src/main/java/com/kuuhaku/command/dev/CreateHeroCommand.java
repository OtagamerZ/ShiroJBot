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

package com.kuuhaku.command.dev;

import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.image.BufferedImage;
import java.util.Arrays;

@Command(
		name = "hero",
		path = "new",
		category = Category.STAFF
)
@Syntax("<name:word:r> <race:word:r>")
public class CreateHeroCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		BufferedImage img = event.message().getAttachments().stream()
				.filter(Message.Attachment::isImage)
				.findFirst()
				.map(att -> IO.getImage(att.getUrl()))
				.orElse(null);

		if (img == null) {
			event.channel().sendMessage(locale.get("error/image_required")).queue();
			return;
		}

		String name = args.getString("name");
		if (name.length() > 20) {
			event.channel().sendMessage(locale.get("error/name_too_long", 20)).queue();
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

			Utils.confirm(locale.get("question/hero_creation", h.getDisplayName()), event.channel(), w -> {
						h.setImage(img);
						h.save();

						Senshi s = h.asSenshi(locale);
						event.channel().sendMessage(locale.get("success/hero_created"))
								.addFiles(FileUpload.fromData(IO.getBytes(s.render(locale, d), "png"), "hero.png"))
								.queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
