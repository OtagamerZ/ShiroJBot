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
import com.kuuhaku.model.common.AutoEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GoodbyeSettings;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.persistent.guild.WelcomeSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.embed.Embed;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "embed",
		path = "set",
		category = Category.MODERATION
)
@Syntax("<json:text:r>")
public class SetEmbedCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();

		try {
			Embed embed = new AutoEmbedBuilder(args.getString("json")).getEmbed();
			settings.setEmbed(embed);
			settings.save();

			if (embed.body() != null) {
				WelcomeSettings ws = data.config().getWelcomeSettings();
				ws.setMessage(embed.body());
				ws.save();

				GoodbyeSettings gs = data.config().getGoodbyeSettings();
				gs.setMessage(embed.body());
				gs.save();
			}

			event.channel().sendMessage(locale.get("success/set_embed")).queue();
		} catch (IllegalArgumentException e) {
			event.channel().sendMessage(locale.get("error/invalid_json")).queue();
		}
	}
}
