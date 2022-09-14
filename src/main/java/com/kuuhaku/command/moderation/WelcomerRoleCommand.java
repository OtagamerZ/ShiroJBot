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
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;

import java.util.Set;

@Command(
		name = "welcomer",
		category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<role:role:r>"
})
public class WelcomerRoleCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();
		if (args.containsKey("action")) {
			settings.setWelcomer(null);
			settings.save();

			event.channel().sendMessage(locale.get("success/welcomer_role_clear")).queue();
			return;
		} else if (args.containsKey("role")) {
			settings.setWelcomer(event.message().getMentionedRoles().get(0));
			settings.save();

			event.channel().sendMessage(locale.get("success/welcomer_role_save")).queue();
			return;
		}

		Role role = settings.getWelcomer();
		event.channel().sendMessage(locale.get("str/current_welcomer_role",
				role == null ? "`" + locale.get("str/none") + "`" : role.getAsMention()
		)).allowedMentions(Set.of()).queue();
	}
}
