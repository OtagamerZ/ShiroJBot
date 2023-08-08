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
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

@Command(
		name = "starboard",
		category = Category.MODERATION
)
@Signature({
		"<action:word:r>[clear]",
		"<channel:channel:r>",
		"<value:number:r>"
})
public class StarboardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();
		if (args.has("action")) {
			settings.setStarboardThreshold(5);
			settings.setStarboardChannel(null);
			settings.save();

			event.channel().sendMessage(locale.get("success/starboard_clear")).queue();
			return;
		} else if (args.has("channel")) {
			GuildChannel channel = event.channels(0);
			if (channel == null) {
				event.channel().sendMessage(locale.get("error/invalid_mention", 0)).queue();
				return;
			}

			if (!(channel instanceof GuildMessageChannel gmc)) {
				event.channel().sendMessage(locale.get("error/invalid_channel")).queue();
				return;
			}

			settings.setStarboardChannel(gmc);
			settings.save();

			event.channel().sendMessage(locale.get("success/starboard_channel_save")).queue();
			return;
		}

		int value = args.getInt("value");
		if (!Utils.between(value, 3, 50)) {
			event.channel().sendMessage(locale.get("error/invalid_value_range", 3, 50)).queue();
			return;
		}

		settings.setStarboardThreshold(value);
		settings.save();

		event.channel().sendMessage(locale.get("success/starboard_threshold_save")).queue();
	}
}
