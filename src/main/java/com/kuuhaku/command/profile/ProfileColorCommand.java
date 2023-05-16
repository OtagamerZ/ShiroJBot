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
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.AccountSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.awt.*;

@Command(
		name = "profile",
		subname = "color",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<text:text:r>")
public class ProfileColorCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		AccountSettings settings = data.profile().getAccount().getSettings();

		String text = args.getString("text");
		if (text.isBlank()) {
			settings.setColor(Color.BLACK);
			event.channel().sendMessage(locale.get("success/profile_color_clear")).queue();
		} else {
			if (!Utils.match(text, "#[\\da-fA-F]{6}")) {
				event.channel().sendMessage(locale.get("error/invalid_color")).queue();
				return;
			}

			settings.setColor(Color.decode(text));
			event.channel().sendMessage(locale.get("success/profile_color_set")).queue();
		}

		settings.save();
	}
}