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

package com.kuuhaku.command.moderation;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

@Command(
		name = "locale",
		path = "channel",
		category = Category.MODERATION
)
@Syntax("<channel:channel> <locale:word>")
public class LocaleChannelCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildChannel channel;
		if (args.has("channel")) {
			channel = event.channels(0);
			if (channel == null) {
				event.channel().sendMessage(locale.get("error/invalid_mention")).queue();
				return;
			}
		} else {
			channel = event.channel();
		}

		GuildSettings settings = data.config().getSettings();
		if (args.has("locale")) {
			I18N loc = args.getEnum(I18N.class, "locale");
			if (loc == null) {
				event.channel().sendMessage(locale.get("error/invalid_locale")).queue();
				return;
			}

			settings.getChannelLocales().put(channel.getId(), loc);
			event.channel().sendMessage(locale.get("success/locale_changed")).queue();
		} else {
			settings.getChannelLocales().remove(channel.getId());
			event.channel().sendMessage(locale.get("success/channel_locale_reset")).queue();
		}

		data.config().save();
	}
}
