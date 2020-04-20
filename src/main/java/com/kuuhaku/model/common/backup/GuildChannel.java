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

import java.util.Map;

public class GuildChannel {
	private final String name;
	private final String topic;
	private final Map<Long, long[]> permission;
	private final int userLimit;
	private final int bitrate;
	private final boolean text;
	private final boolean nsfw;

	public GuildChannel(String name, String topic, Map<Long, long[]> permission, boolean nsfw) {
		this.name = name;
		this.topic = topic;
		this.permission = permission;
		this.userLimit = 0;
		this.bitrate = 0;
		this.text = true;
		this.nsfw = nsfw;
	}

	public GuildChannel(String name, Map<Long, long[]> permission, int userLimit, int bitrate) {
		this.name = name;
		this.topic = "";
		this.permission = permission;
		this.userLimit = userLimit;
		this.bitrate = bitrate;
		this.text = false;
		this.nsfw = false;
	}

	public String getName() {
		return name;
	}

	public String getTopic() {
		return topic;
	}

	public Map<Long, long[]> getPermission() {
		return permission;
	}

	public int getUserLimit() {
		return userLimit;
	}

	public int getBitrate() {
		return bitrate;
	}

	public boolean isText() {
		return text;
	}

	public boolean isNsfw() {
		return nsfw;
	}
}
