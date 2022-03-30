/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.kuuhaku.utils.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.time.temporal.TemporalAccessor;

public class ColorlessWebhookEmbedBuilder extends WebhookEmbedBuilder {
	@Nonnull
	@Override
	public WebhookEmbed build() {
		super.setColor(Color.decode("#2f3136").getRGB());
		return super.build();
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setTimestamp(@Nullable TemporalAccessor timestamp) {
		return (ColorlessWebhookEmbedBuilder) super.setTimestamp(timestamp);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setColor(@Nullable Integer color) {
		return (ColorlessWebhookEmbedBuilder) super.setColor(color);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setDescription(@Nullable String description) {
		return (ColorlessWebhookEmbedBuilder) super.setDescription(description);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setThumbnailUrl(@Nullable String thumbnailUrl) {
		return (ColorlessWebhookEmbedBuilder) super.setThumbnailUrl(thumbnailUrl);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setImageUrl(@Nullable String imageUrl) {
		return (ColorlessWebhookEmbedBuilder) super.setImageUrl(imageUrl);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setFooter(@Nullable WebhookEmbed.EmbedFooter footer) {
		return (ColorlessWebhookEmbedBuilder) super.setFooter(footer);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setTitle(@Nullable WebhookEmbed.EmbedTitle title) {
		return (ColorlessWebhookEmbedBuilder) super.setTitle(title);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setAuthor(@Nullable WebhookEmbed.EmbedAuthor author) {
		return (ColorlessWebhookEmbedBuilder) super.setAuthor(author);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder addField(@Nonnull WebhookEmbed.EmbedField field) {
		return (ColorlessWebhookEmbedBuilder) super.addField(field);
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setFooter(String text) {
		return (ColorlessWebhookEmbedBuilder) super.setFooter(new WebhookEmbed.EmbedFooter(text, null));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setFooter(String text, String icon) {
		return (ColorlessWebhookEmbedBuilder) super.setFooter(new WebhookEmbed.EmbedFooter(text, icon));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setTitle(String text) {
		return (ColorlessWebhookEmbedBuilder) super.setTitle(new WebhookEmbed.EmbedTitle(text, null));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setTitle(String text, String icon) {
		return (ColorlessWebhookEmbedBuilder) super.setTitle(new WebhookEmbed.EmbedTitle(text, icon));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setAuthor(String text) {
		return (ColorlessWebhookEmbedBuilder) super.setAuthor(new WebhookEmbed.EmbedAuthor(text, null, null));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setAuthor(String text, String icon) {
		return (ColorlessWebhookEmbedBuilder) super.setAuthor(new WebhookEmbed.EmbedAuthor(text, icon, null));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder setAuthor(String text, String icon, String url) {
		return (ColorlessWebhookEmbedBuilder) super.setAuthor(new WebhookEmbed.EmbedAuthor(text, icon, url));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder addBlankField(boolean inline) {
		return (ColorlessWebhookEmbedBuilder) super.addField(new WebhookEmbed.EmbedField(inline, Constants.VOID, Constants.VOID));
	}

	public @Nonnull ColorlessWebhookEmbedBuilder addField(String title, String value, boolean inline) {
		return (ColorlessWebhookEmbedBuilder) super.addField(new WebhookEmbed.EmbedField(inline, title, value));
	}
}
