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

package com.kuuhaku.model.persistent.javatype;

import com.kuuhaku.Main;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.io.Serial;
import java.sql.Types;

public class ChannelJavaType extends AbstractClassJavaType<GuildMessageChannel> {
	@Serial
	private static final long serialVersionUID = -8058769337567861762L;

	public static final ChannelJavaType INSTANCE = new ChannelJavaType();

	public ChannelJavaType() {
		super(GuildMessageChannel.class, new ImmutableMutabilityPlan<>());
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getTypeConfiguration()
				.getJdbcTypeRegistry()
				.getDescriptor(Types.VARCHAR);
	}

	@Override
	public String toString(GuildMessageChannel value) {
		if (value == null) return null;

		return value.getId();
	}

	@Override
	public GuildMessageChannel fromString(CharSequence id) {
		return Main.getApp().getMessageChannelById(Utils.getOr(String.valueOf(id), "1"));
	}

	@Override
	public <X> X unwrap(GuildMessageChannel value, Class<X> type, WrapperOptions options) {
		if (value == null) return null;

		if (String.class.isAssignableFrom(type)) {
			return type.cast(value.getId());
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> GuildMessageChannel wrap(X value, WrapperOptions options) {
		return switch (value) {
			case null -> null;
			case String id -> Main.getApp().getMessageChannelById(Utils.getOr(id, "1"));
			case GuildMessageChannel c -> c;
			default -> throw unknownWrap(value.getClass());
		};

	}
}
