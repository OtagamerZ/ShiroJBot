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

package com.kuuhaku.model.common;

import com.kuuhaku.Constants;
import com.kuuhaku.model.records.embed.Embed;
import com.kuuhaku.model.records.embed.Field;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public class AutoEmbedBuilder extends EmbedBuilder {
	private final Embed e;

	public AutoEmbedBuilder(Embed embed) {
		e = embed;

		if (e.title() != null) {
			setTitle(
					StringUtils.abbreviate(e.title().name(), MessageEmbed.TITLE_MAX_LENGTH),
					e.title().url()
			);
		}

		if (e.author() != null) {
			setAuthor(
					StringUtils.abbreviate(e.author().name(), MessageEmbed.AUTHOR_MAX_LENGTH),
					e.author().url(),
					e.author().icon()
			);
		}

		setColor(e.getParsedColor());
		setDescription(Utils.getOr(StringUtils.abbreviate(e.body(), MessageEmbed.TEXT_MAX_LENGTH), Constants.VOID));
		setThumbnail(e.thumbnail());
		if (e.image() != null) try {
			setImage(e.image().getRandomImage());
		} catch (IllegalArgumentException ignore) {
		}
		if (e.showDate()) {
			setTimestamp(OffsetDateTime.now(ZoneId.of("GMT-3")));
		}

		if (e.footer() != null) {
			setFooter(
					StringUtils.abbreviate(e.footer().name(), MessageEmbed.TEXT_MAX_LENGTH),
					e.footer().icon()
			);
		}

		if (e.fields() != null) {
			List<Field> fields = e.fields();
			for (Field field : fields) {
				addField(
						StringUtils.abbreviate(field.name(), MessageEmbed.TITLE_MAX_LENGTH),
						StringUtils.abbreviate(field.value(), MessageEmbed.VALUE_MAX_LENGTH),
						field.inline()
				);
			}
		}
	}

	public AutoEmbedBuilder(String json) {
		this(JSONUtils.fromJSON(json, Embed.class));
	}

	public AutoEmbedBuilder() {
		this("{}");
	}

	public Embed getEmbed() {
		return e;
	}

	@Override
	public String toString() {
		return JSONUtils.toJSON(e);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AutoEmbedBuilder that = (AutoEmbedBuilder) o;
		return Objects.equals(e, that.e);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(e);
	}
}
