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
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GoodbyeSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

@Command(
		name = "goodbye",
		category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<channel:channel:r>",
		"<message:text:r>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class GoodbyeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GoodbyeSettings settings = data.config().getGoodbyeSettings();
		if (args.has("action")) {
			settings.setMessage(locale.get("default/goodbye_message"));
			settings.setChannel(null);
			settings.save();

			event.channel().sendMessage(locale.get("success/goodbye_clear")).queue();
			return;
		} else if (args.has("channel")) {
			GuildChannel channel = event.channels(0);
			if (channel == null) {
				event.channel().sendMessage(locale.get("error/invalid_mention")).queue();
				return;
			}

			if (!(channel instanceof GuildMessageChannel gmc)) {
				event.channel().sendMessage(locale.get("error/invalid_channel")).queue();
				return;
			}

			settings.setChannel(gmc);
			settings.save();

			event.channel().sendMessage(locale.get("success/goodbye_channel_save")).queue();
			return;
		}

		String msg = args.getString("message");
		if (msg == null) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setDescription(settings.getMessage());

			GuildMessageChannel chn = settings.getChannel();
			event.channel().sendMessage(locale.get("str/current_goodbye_message",
					chn == null ? "`" + locale.get("str/none") + "`" : chn.getAsMention()
			)).setEmbeds(eb.build()).queue();
			return;
		}

		settings.setMessage(msg);
		settings.save();

		event.channel().sendMessage(locale.get("success/goodbye_message_save")).queue();
	}
}
