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

package com.kuuhaku.model.records;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public record MessageData(net.dv8tion.jda.api.entities.Guild guild, MessageChannel channel, String messageId) {
	public MessageData(GenericMessageEvent event) {
		this(
				event.isFromGuild() ? event.getGuild() : null,
				event.getChannel(),
				event.getMessageId()
		);
	}

	public record Guild(net.dv8tion.jda.api.entities.Guild guild, GuildMessageChannel channel, Message message, Member member) {
		public Guild(Message message) {
			this(message.getGuild(), message.getGuildChannel(), message, message.getMember());
		}

		public User user() {
			return member.getUser();
		}
	}

	public record Private(PrivateChannel channel, Message message, User user) {
		public Private(Message message) {
			this(message.getChannel().asPrivateChannel(), message, message.getAuthor());
		}
	}
}
