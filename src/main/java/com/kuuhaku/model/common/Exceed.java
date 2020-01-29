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

import com.kuuhaku.utils.ExceedEnums;

public class Exceed {
	private final ExceedEnums exceed;
	private final int members;
	private final long exp;

	public Exceed(ExceedEnums exceed, int members, long exp) {
		this.exceed = exceed;
		this.members = members;
		this.exp = exp;
	}

	public ExceedEnums getExceed() {
		return exceed;
	}

	public int getMembers() {
		return members;
	}

	public long getExp() {
		return exp;
	}
}
