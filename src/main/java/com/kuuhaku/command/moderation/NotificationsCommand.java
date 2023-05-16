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
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

@Command(
		name = "notifications",
		category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<channel:channel:r>"
})
public class NotificationsCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();
		if (args.has("action")) {
			settings.getKawaiponChannels().clear();
			settings.save();

			event.channel().sendMessage(locale.get("success/notifications_channel_clear")).queue();
			return;
		} else if (!args.has("channel")) {
			if (settings.isFeatureEnabled(GuildFeature.NOTIFICATIONS)) {
				settings.getFeatures().remove(GuildFeature.NOTIFICATIONS);
				event.channel().sendMessage(locale.get("success/notifications_disable")).queue();
			} else {
				settings.getFeatures().add(GuildFeature.NOTIFICATIONS);
				event.channel().sendMessage(locale.get("success/notifications_enable")).queue();
			}

			settings.save();
			return;
		}

		GuildChannel channel = event.message().getMentions().getChannels().get(0);
		if (!(channel instanceof GuildMessageChannel gmc)) {
			event.channel().sendMessage(locale.get("error/invalid_channel")).queue();
			return;
		}

		settings.setNotificationsChannel(gmc);
		settings.save();

		event.channel().sendMessage(locale.get("success/notifications_channel_set", channel.getAsMention())).queue();
	}
}
