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

package com.kuuhaku.model.enums;

import java.util.EnumSet;

public enum ClanPermission {
	ALTER_HIERARCHY("Promover/rebaixar membros", 0x1),
	KICK("Expulsar membros", 0x2),
	WITHDRAW("Sacar créditos do cofre", 0x4),
	INVITE("Convidar membros", 0x8);

	private final String name;
	private final int flag;

	ClanPermission(String name, int flag) {
		this.name = name;
		this.flag = flag;
	}

	public String getName() {
		return name;
	}

	public static int getFlags(EnumSet<ClanPermission> perms) {
		int flags = 0;
		if (perms.contains(ALTER_HIERARCHY)) {
			flags |= ALTER_HIERARCHY.flag;
		}
		if (perms.contains(KICK)) {
			flags |= KICK.flag;
		}
		if (perms.contains(WITHDRAW)) {
			flags |= WITHDRAW.flag;
		}
		if (perms.contains(INVITE)) {
			flags |= INVITE.flag;
		}

		return flags;
	}

	public static EnumSet<ClanPermission> getPermissions(int flags) {
		EnumSet<ClanPermission> perms = EnumSet.noneOf(ClanPermission.class);
		if ((flags & ALTER_HIERARCHY.flag) == ALTER_HIERARCHY.flag) {
			perms.add(ALTER_HIERARCHY);
		}
		if ((flags & KICK.flag) == KICK.flag) {
			perms.add(KICK);
		}
		if ((flags & WITHDRAW.flag) == WITHDRAW.flag) {
			perms.add(WITHDRAW);
		}
		if ((flags & INVITE.flag) == INVITE.flag) {
			perms.add(INVITE);
		}

		return perms;
	}
}
