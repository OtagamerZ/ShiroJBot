/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

@Command(
		name = "deny",
		category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {"<channel:channel:r>"})
public class DenyCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();

		TextChannel channel;
		if (args.containsKey("channel")) {
			channel = event.message().getMentionedChannels().get(0);
		} else {
			channel = event.channel();
		}

		if (settings.getDeniedChannels().stream().anyMatch(t -> t.getId().equals(channel.getId()))) {
			event.channel().sendMessage(locale.get("error/already_denied").formatted(
					channel == event.channel() ? "this channel" : channel.getAsMention()
			)).queue();
			return;
		}

		settings.getDeniedChannels().add(channel);
		settings.save();

		event.channel().sendMessage(locale.get("success/commands_denied").formatted(
				channel == event.channel() ? "this channel" : channel.getAsMention()
		)).queue();
	}
}
