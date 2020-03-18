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

package com.kuuhaku.model.common.backup;

import java.util.List;

public class GuildData {
	private final List<GuildCategory> categories;
	private final List<GuildRole> roles;
	private final List<String> members;

	public GuildData(List<GuildCategory> categories, List<GuildRole> roles, List<String> members) {
		this.categories = categories;
		this.roles = roles;
		this.members = members;
	}

	public List<GuildCategory> getCategories() {
		return categories;
	}

	public List<GuildRole> getRoles() {
		return roles;
	}

	public List<String> getMembers() {
		return members;
	}
}
