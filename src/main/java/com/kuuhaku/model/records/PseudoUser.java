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

package com.kuuhaku.model.records;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

public record PseudoUser(String name, String avatar, GuildMessageChannel channel) {
	public PseudoUser(User user, GuildMessageChannel channel) {
		this(user.getName(), user.getEffectiveAvatarUrl(), channel);
	}

	public PseudoUser(Member member, GuildMessageChannel channel) {
		this(member.getEffectiveName(), member.getEffectiveAvatarUrl(), channel);
	}

	public WebhookClient webhook() {
		if (!(channel instanceof StandardGuildMessageChannel chn)) return null;

		Webhook hook = Utils.getWebhook(chn);
		if (hook == null) return null;

		return WebhookClient.withUrl(hook.getUrl());
	}

	public void send(Message source, String text, WebhookEmbed... embeds) {
		try (WebhookClient hook = webhook()) {
			if (hook != null) {
				if (source != null) {
					source.delete().queue(null, Utils::doNothing);
				}

				WebhookMessage msg = new WebhookMessageBuilder()
						.setUsername(name)
						.setAvatarUrl(avatar)
						.setAllowedMentions(AllowedMentions.none())
						.setContent(text)
						.addEmbeds(embeds)
						.build();

				hook.send(msg);
			} else {
				channel.sendMessage(text).setMessageReference(source).queue();
			}
		}
	}
}
