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

import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum ClanHierarchy {
	LEADER("Líder", "🔱"),
	SUBLEADER("Sub-líder", "⚜️"),
	CAPTAIN("Capitão", "🔰"),
	MEMBER("Membro", Helper.VOID);

	private final String name;
	private final String icon;

	ClanHierarchy(String name, String icon) {
		this.name = name;
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public String getIcon() {
		return icon;
	}

	public static ClanHierarchy getByName(String name) {
		return Arrays.stream(ClanHierarchy.values())
				.filter(e -> Helper.equalsAny(name, StringUtils.stripAccents(e.name), e.name, e.name()))
				.findFirst()
				.orElse(null);
	}
}
