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

package com.kuuhaku.command.profile;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.AccountSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.awt.*;
import java.awt.image.BufferedImage;

@Command(
		name = "profile",
		path = "widget",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[add] <text:text:r>",
		"<action:word:r>[set] <id:number:r> <text:text:r>",
		"<action:word:r>[remove] <id:number:r>",
		"<action:word:r>[clear]"
})
public class ProfileWidgetCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		AccountSettings settings = data.profile().getAccount().getSettings();

		String op = args.getString("action").toLowerCase();
		switch (op) {
			case "add", "set" -> {
				Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
				g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(15));

				String text = args.getString("text").replace("\n", " ");
				if (Graph.getStringBounds(g2d, text).getWidth() > 375) {
					g2d.dispose();
					event.channel().sendMessage(locale.get("error/too_long")).queue();
					return;
				}
				g2d.dispose();

				if (op.equals("set")) {
					int id = args.getInt("id", 0) - 1;
					if (!Utils.between(id, 0, settings.getWidgets().size() - 1)) {
						event.channel().sendMessage(locale.get("error/invalid_value_range", 1, settings.getWidgets().size() - 1)).queue();
						return;
					}

					settings.getWidgets().set(id, text);
					event.channel().sendMessage(locale.get("success/profile_widget_set")).queue();
				} else {
					if (settings.getWidgets().size() >= 7) {
						event.channel().sendMessage(locale.get("error/too_many_widgets")).queue();
						return;
					}

					settings.getWidgets().add(text);
					event.channel().sendMessage(locale.get("success/profile_widget_add")).queue();
				}

				settings.save();
			}
			case "remove" -> {
				int id = args.getInt("id", 0) - 1;
				if (!Utils.between(id, 0, settings.getWidgets().size() - 1)) {
					event.channel().sendMessage(locale.get("error/invalid_value_range", 1, settings.getWidgets().size() - 1)).queue();
					return;
				}

				event.channel().sendMessage(locale.get("success/profile_widget_remove")).queue();
				settings.getWidgets().remove(id);
				settings.save();
			}
			case "clear" -> {
				event.channel().sendMessage(locale.get("success/profile_widget_clear")).queue();
				settings.getWidgets().clear();
				settings.save();
			}
		}
	}
}
