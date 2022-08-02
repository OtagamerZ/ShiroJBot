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
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Command(
		name = "snipe",
		category = Category.MODERATION
)
@Signature("<channel:channel>")
@Requires(Permission.MESSAGE_HISTORY)
public class SnipeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		TextChannel channel;
		if (args.containsKey("channel")) {
			channel = event.message().getMentionedChannels().get(0);
		} else {
			channel = event.channel();
		}

		List<String> hist = channel.getHistory()
				.retrievePast(100)
				.complete().stream()
				.map(Message::getId)
				.toList();
		List<Message> messages = GuildListener.getMessages(event.guild()).stream()
				.filter(m -> m.getChannel().equals(channel))
				.filter(m -> !hist.contains(m.getId()))
				.toList();

		if (messages.isEmpty()) {
			event.channel().sendMessage(locale.get("error/no_deleted_messages", channel.getAsMention())).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(":detective: | " + locale.get("str/snipe_result"));

		Utils.paginate(
				Utils.generatePages(eb, messages, 10, 5, m -> new FieldMimic(
						locale.get("str/sent",
								m.getTimeCreated().toEpochSecond(),
								m.getAuthor().getName()
						), StringUtils.abbreviate(m.getContentRaw(), 100)
				).toString()),
				event.channel(),
				event.user()
		);
	}
}
