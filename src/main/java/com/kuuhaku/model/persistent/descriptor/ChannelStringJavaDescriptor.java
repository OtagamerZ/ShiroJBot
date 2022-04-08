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

package com.kuuhaku.model.persistent.descriptor;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Utils;
import net.dv8tion.jda.api.entities.TextChannel;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

import java.io.Serial;

public class ChannelStringJavaDescriptor extends AbstractTypeDescriptor<TextChannel> {
	@Serial
	private static final long serialVersionUID = -8058769337567861762L;

	public static final ChannelStringJavaDescriptor INSTANCE = new ChannelStringJavaDescriptor();

	public ChannelStringJavaDescriptor() {
		super(TextChannel.class, new ImmutableMutabilityPlan<>());
	}

	@Override
	public TextChannel fromString(String string) {
		return null;
	}

	@Override
	public <X> X unwrap(TextChannel value, Class<X> type, WrapperOptions options) {
		if (value == null) return null;
		else if (String.class.isAssignableFrom(type)) {
			return type.cast(value.getId());
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> TextChannel wrap(X value, WrapperOptions options) {
		if (value == null) return null;
		else if (value instanceof String id) {
			return Main.getApp().getShiro().getTextChannelById(Utils.getOr(id, "1"));
		}

		throw unknownWrap(value.getClass());
	}
}
