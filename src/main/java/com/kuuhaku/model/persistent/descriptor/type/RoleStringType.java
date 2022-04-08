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

package com.kuuhaku.model.persistent.descriptor.type;

import com.kuuhaku.model.persistent.descriptor.RoleStringJavaDescriptor;
import net.dv8tion.jda.api.entities.Role;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.io.Serial;

public class RoleStringType extends AbstractSingleColumnStandardBasicType<Role> {
	@Serial
	private static final long serialVersionUID = -6274205733576989886L;
	public static final RoleStringType INSTANCE = new RoleStringType();

	public RoleStringType() {
		super(VarcharTypeDescriptor.INSTANCE, RoleStringJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "RoleString";
	}
}
