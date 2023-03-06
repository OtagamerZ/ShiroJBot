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

package com.kuuhaku.model.persistent.converter;

import com.kuuhaku.Main;
import com.kuuhaku.util.Utils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

@Converter(autoApply = true)
public class ChannelConverter implements AttributeConverter<GuildMessageChannel, String> {
	@Override
	public String convertToDatabaseColumn(GuildMessageChannel channel) {
		if (channel == null) return null;

		return channel.getId();
	}

	@Override
	public GuildMessageChannel convertToEntityAttribute(String id) {
		return Main.getApp().getMessageChannelById(Utils.getOr(id, "1"));
	}
}
