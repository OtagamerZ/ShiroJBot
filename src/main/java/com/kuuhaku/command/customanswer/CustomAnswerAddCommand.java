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

package com.kuuhaku.command.customanswer;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.CustomAnswer;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "custom",
		subname = "add",
		category = Category.MODERATION
)
@Signature("<json:text:r>")
public class CustomAnswerAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		JSONObject struct = args.getJSONObject("json");
		struct.put("author", event.user().getId());

		if (!Utils.between(struct.getString("trigger").length(), 3, 256)) {
			event.channel().sendMessage(locale.get("error/trigger_length")).queue();
			return;
		} else if (!Utils.between(struct.getString("answer").length(), 1, 256)) {
			event.channel().sendMessage(locale.get("error/answer_length")).queue();
			return;
		} else if (!Utils.between(struct.getString("answer").length(), 1, 256)) {
			event.channel().sendMessage(locale.get("error/answer_length")).queue();
			return;
		}

		for (Object chn : struct.getJSONArray("channels")) {
			String id = String.valueOf(chn);

			if (!StringUtils.isNumeric(id) || event.guild().getTextChannelById(id) == null) {
				event.channel().sendMessage(locale.get("error/invalid_channel", id)).queue();
				return;
			}
		}

		for (Object usr : struct.getJSONArray("users")) {
			String id = String.valueOf(usr);

			if (!StringUtils.isNumeric(id) || bot.getUserById(id) == null) {
				event.channel().sendMessage(locale.get("error/invalid_user", id)).queue();
				return;
			}
		}

		if (struct.has("chance")) {
			struct.put("chance", Utils.clamp(struct.getInt("chance"), 1, 100));
		}

		try {
			GuildSettings settings = data.config().getSettings();

			CustomAnswer ca = new CustomAnswer(
					settings, event.user().getId(),
					struct.getString("trigger"),
					struct.getString("answer"),
					struct.getInt("chance", 100),
					struct.getJSONArray("channels"),
					struct.getJSONArray("users")
			);

			settings.getCustomAnswers().add(ca);
			settings.save();

			event.channel().sendMessage(locale.get("success/custom_answer_add")).queue();
		} catch (Exception e) {
			event.channel().sendMessage(locale.get("error/custom_answer")).queue();
		}
	}
}
