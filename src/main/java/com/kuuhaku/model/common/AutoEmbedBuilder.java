/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.common.embed.Embed;
import com.kuuhaku.model.common.embed.Field;
import com.kuuhaku.utils.JSONUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

public class AutoEmbedBuilder extends EmbedBuilder {
	private final Embed e;

	public AutoEmbedBuilder(Embed embed) {
		e = embed;

		setTitle(
				StringUtils.abbreviate(e.getTitle().getName(), MessageEmbed.TITLE_MAX_LENGTH),
				e.getTitle().getUrl()
		);

		setAuthor(
				StringUtils.abbreviate(e.getAuthor().getName(), MessageEmbed.AUTHOR_MAX_LENGTH),
				e.getAuthor().getUrl(),
				e.getAuthor().getIcon()
		);

		setColor(e.getParsedColor());
		setDescription(StringUtils.abbreviate(e.getBody(), MessageEmbed.TEXT_MAX_LENGTH));
		setThumbnail(e.getThumbnail());
		setImage(e.getImage().getImage());
		if (e.showDate()) setTimestamp(LocalDateTime.now());

		setFooter(
				StringUtils.abbreviate(e.getFooter().getName(), MessageEmbed.TEXT_MAX_LENGTH),
				e.getFooter().getIcon()
		);

		List<Field> fields = e.getFields();
		for (Field field : fields) {
			addField(
					StringUtils.abbreviate(field.getName(), MessageEmbed.TITLE_MAX_LENGTH),
					StringUtils.abbreviate(field.getValue(), MessageEmbed.VALUE_MAX_LENGTH),
					field.getInline()
			);
		}
	}

	public AutoEmbedBuilder(String json) {
		this(JSONUtils.fromJSON(json, Embed.class));
	}

	public Embed getEmbed() {
		return e;
	}
}
