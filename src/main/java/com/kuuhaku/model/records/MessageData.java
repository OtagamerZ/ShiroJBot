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

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.List;

public record MessageData(net.dv8tion.jda.api.entities.Guild guild, MessageChannel channel, String messageId) {
	public MessageData(GenericMessageEvent event) {
		this(
				event.isFromGuild() ? event.getGuild() : null,
				event.getChannel(),
				event.getMessageId()
		);
	}

	public record Guild(net.dv8tion.jda.api.entities.Guild guild, GuildMessageChannel channel, Message message, Member member, long interceptMillis) {
		public Guild(Message message) {
			this(message.getGuild(), message.getGuildChannel(), message, message.getMember(), System.currentTimeMillis());
		}

		public User user() {
			return member.getUser();
		}

		public Member me() {
			return guild.getSelfMember();
		}

		public IMentionable mentions(Message.MentionType type, int idx) {
			List<IMentionable> mentions = message.getMentions().getMentions(type);
			if (mentions.size() <= idx) return null;

			return mentions.get(idx);
		}

		public User users(int idx) {
			UserSnowflake usr = (UserSnowflake) mentions(Message.MentionType.USER, idx);
			if (usr instanceof Member m) return m.getUser();

			return (User) usr;
		}

		public Member members(int idx) {
			UserSnowflake usr = (UserSnowflake) mentions(Message.MentionType.USER, idx);
			if (usr instanceof Member m) return m;

			return null;
		}

		public Role roles(int idx) {
			return (Role) mentions(Message.MentionType.ROLE, idx);
		}

		public GuildChannel channels(int idx) {
			return (GuildChannel) mentions(Message.MentionType.CHANNEL, idx);
		}
	}

	public record Private(PrivateChannel channel, Message message, User user, long interceptMillis) {
		public Private(Message message) {
			this(message.getChannel().asPrivateChannel(), message, message.getAuthor(), System.currentTimeMillis());
		}

		public IMentionable mentions(Message.MentionType type, int idx) {
			List<IMentionable> mentions = message.getMentions().getMentions(type);
			if (mentions.size() <= idx) return null;

			return mentions.get(idx);
		}

		public User users(int idx) {
			return (User) mentions(Message.MentionType.USER, idx);
		}

		public Role roles(int idx) {
			return (Role) mentions(Message.MentionType.ROLE, idx);
		}

		public GuildChannel channels(int idx) {
			return (GuildChannel) mentions(Message.MentionType.CHANNEL, idx);
		}
	}
}
