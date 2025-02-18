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
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl;

@Command(
		name = "setdrop",
		category = Category.MODERATION
)
@Syntax(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<channel:channel:r>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class SetDropCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();
		if (args.has("action")) {
			settings.getDropChannels().clear();
			settings.save();

			event.channel().sendMessage(locale.get("success/drop_channel_clear")).queue();
			return;
		}

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

		if (settings.getDropChannels().stream().anyMatch(t -> t.equals(channel))) {
			settings.getDropChannels().removeIf(t -> t.equals(channel));
			event.channel().sendMessage(locale.get("success/drop_channel_remove", channel.getAsMention())).queue();
		} else {
			if (!(channel instanceof TextChannelImpl tc)) {
				event.channel().sendMessage(locale.get("error/invalid_channel")).queue();
				return;
			}

			settings.getDropChannels().add(tc);
			event.channel().sendMessage(locale.get("success/drop_channel_add", channel.getAsMention())).queue();
		}

		settings.save();
	}
}
