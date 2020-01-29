/*
 * This file is part of Shiro J Bot.
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

import org.apache.commons.lang3.StringUtils;

public class Extensions {
	private static final String[] ext = new String[]{
			".com", ".br", ".net", ".org", ".gov",
			".gg", ".xyz", ".site", ".blog", ".tv",
			".biz", ".fly", ".gl", ".ru", ".es",
			".tech"
	};

	public static boolean checkExtension(String str) {
		return StringUtils.containsAny(str, ext);
	}
}
