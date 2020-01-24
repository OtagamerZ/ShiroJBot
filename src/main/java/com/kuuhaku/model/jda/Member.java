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

package com.kuuhaku.model.jda;

public class Member {
	private final String id;
	private final String guild;
	private final String name;
	private final String nickname;
	private final String avatar;

	public Member(String id, String guild, String name, String nickname, String avatar) {
		this.id = id;
		this.guild = guild;
		this.name = name;
		this.nickname = nickname;
		this.avatar = avatar;
	}

	public String getId() {
		return id;
	}

	public String getGuild() {
		return guild;
	}

	public String getName() {
		return name;
	}

	public String getNickname() {
		return nickname;
	}

	public String getAvatar() {
		return avatar;
	}
}
