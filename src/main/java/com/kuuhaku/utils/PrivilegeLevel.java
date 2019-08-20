/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.utils;

public enum PrivilegeLevel {

    USER(0), DJ(1), EXCEED(1), PARTNER(2), MOD(3), DEV(4), NIICHAN(5);

	private final Integer authority;

	PrivilegeLevel(int authority) {
		this.authority = authority;
	}

	public boolean hasAuthority(PrivilegeLevel outro) {
		return this.authority >= outro.authority;
	}
}