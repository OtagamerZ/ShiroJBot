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

import java.util.List;
import java.util.Map;

public class GuildCategory {
	private final String name;
	private final List<GuildChannel> channels;
	private final Map<Long, long[]> permission;

	public GuildCategory(String name, List<GuildChannel> channels, Map<Long, long[]> permission) {
		this.name = name;
		this.channels = channels;
		this.permission = permission;
	}

	public String getName() {
		return name;
	}

	public List<GuildChannel> getChannels() {
		return channels;
	}

	public Map<Long, long[]> getPermission() {
		return permission;
	}
}
