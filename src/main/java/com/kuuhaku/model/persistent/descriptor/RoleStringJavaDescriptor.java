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
import net.dv8tion.jda.api.entities.Role;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

import java.io.Serial;

public class RoleStringJavaDescriptor extends AbstractTypeDescriptor<Role> {
	@Serial
	private static final long serialVersionUID = 7007254124006589665L;

	public static final RoleStringJavaDescriptor INSTANCE = new RoleStringJavaDescriptor();

	public RoleStringJavaDescriptor() {
		super(Role.class, new ImmutableMutabilityPlan<>());
	}

	@Override
	public Role fromString(String string) {
		return null;
	}

	@Override
	public <X> X unwrap(Role value, Class<X> type, WrapperOptions options) {
		if (value == null) return null;
		else if (String.class.isAssignableFrom(type)) {
			return type.cast(value.getId());
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> Role wrap(X value, WrapperOptions options) {
		if (value == null) return null;
		else if (value instanceof String id) {
			return Main.getApp().getShiro().getRoleById(Utils.getOr(id, "1"));
		}

		throw unknownWrap(value.getClass());
	}
}
