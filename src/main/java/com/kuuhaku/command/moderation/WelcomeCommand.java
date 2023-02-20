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
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.WelcomeSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

@Command(
		name = "welcome",
		category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<channel:channel:r>",
		"<message:text:r>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class WelcomeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		WelcomeSettings settings = data.config().getWelcomeSettings();
		if (args.has("action")) {
			settings.setMessage(locale.get("default/welcome_message"));
			settings.setChannel(null);
			settings.save();

			event.channel().sendMessage(locale.get("success/welcome_clear")).queue();
			return;
		} else if (args.has("channel")) {
			settings.setChannel(event.message().getMentionedChannels().get(0));
			settings.save();

			event.channel().sendMessage(locale.get("success/welcome_channel_save")).queue();
			return;
		}

		String msg = args.getString("message");
		if (msg == null) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setDescription(settings.getMessage());

			TextChannel chn = settings.getChannel();
			event.channel().sendMessage(locale.get("str/current_welcome_message",
					chn == null ? "`" + locale.get("str/none") + "`" : chn.getAsMention()
			)).setEmbeds(eb.build()).queue();
			return;
		}

		settings.setMessage(msg);
		settings.save();

		event.channel().sendMessage(locale.get("success/welcome_message_save")).queue();
	}
}
