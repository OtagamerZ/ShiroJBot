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

public enum Version {
	V1("Mongoose", 1),
	V2("Butterfly", 2),
	V3("Capybara", 3),
	V4("Dolphin", 4);

	private final String codename;
	private final int version;

	Version(String codename, int version) {
		this.codename = codename;
		this.version = version;
	}

	public String getCodename() {
		return codename;
	}

	public int getVersion() {
		return version;
	}
}
