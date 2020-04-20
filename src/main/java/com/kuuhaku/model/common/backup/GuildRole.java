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

package com.kuuhaku.model.common.backup;

public class GuildRole {
	private final String name;
	private final int color;
	private final long permission;
	private final long oldId;
	private final boolean publicRole;

	public GuildRole(String name, int color, long permission, long oldId, boolean publicRole) {
		this.name = name;
		this.color = color;
		this.permission = permission;
		this.oldId = oldId;
		this.publicRole = publicRole;
	}

	public String getName() {
		return name;
	}

	public int getColor() {
		return color;
	}

	public long getPermission() {
		return permission;
	}

	public long getOldId() {
		return oldId;
	}

	public boolean isPublicRole() {
		return publicRole;
	}
}
