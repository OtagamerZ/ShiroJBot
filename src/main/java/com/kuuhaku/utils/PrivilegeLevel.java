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

package com.kuuhaku.utils;

public enum PrivilegeLevel {

	USER(0, 0), DJ(1, 1), EXCEED(1, 2), PARTNER(2, 3), MOD(3, 4), SUPPORT(3, 5), DEV(4, 6), NIICHAN(5, 7);

	private final int authority;
	private final int id;

	PrivilegeLevel(int authority, int id) {
		this.authority = authority;
		this.id = id;
	}

	public boolean hasAuthority(PrivilegeLevel outro) {
		return this.authority > outro.authority || this.id == outro.id;
	}
}