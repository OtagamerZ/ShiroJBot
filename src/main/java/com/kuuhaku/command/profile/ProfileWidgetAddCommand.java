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
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.awt.*;
import java.awt.image.BufferedImage;

@Command(
		name = "profile",
		path = {"widget", "add"},
		category = Category.MISC
)
@Signature("<text:text:r>")
public class ProfileWidgetAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		AccountSettings settings = data.profile().getAccount().getSettings();

		Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(15));

		String text = args.getString("text").replace("\n", " ");
		if (Graph.getStringBounds(g2d, text).getWidth() > 375) {
			g2d.dispose();
			event.channel().sendMessage(locale.get("error/too_long")).queue();
			return;
		}
		g2d.dispose();

		if (settings.getWidgets().size() >= 7) {
			event.channel().sendMessage(locale.get("error/too_many_widgets")).queue();
			return;
		}

		settings.getWidgets().add(text);
		event.channel().sendMessage(locale.get("success/profile_widget_add")).queue();

		settings.save();
	}
}
