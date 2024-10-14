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
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.ArrayList;
import java.util.List;

@Command(
		name = "emote",
		path = "remove",
		category = Category.MODERATION
)
@Syntax("<emote:text:r>")
@Requires(Permission.MANAGE_GUILD_EXPRESSIONS)
public class EmoteRemoveCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<CustomEmoji> emotes = event.message().getMentions().getCustomEmojis();
		if (emotes.isEmpty()) {
			event.channel().sendMessage(locale.get("error/no_emotes")).queue();
			return;
		}

		List<RestAction<Void>> acts = new ArrayList<>();
		for (CustomEmoji e : emotes) {
			RichCustomEmoji emj = event.guild().getEmojiById(e.getId());
			if (emj == null) continue;

			acts.add(emj.delete());
		}

		RestAction.allOf(acts)
				.flatMap(s -> event.channel().sendMessage(locale.get("success/emotes_removed", s.size())))
				.queue();
	}
}
