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

import java.util.Arrays;

public enum ExceedRole {
	IMANITY("719603595200430101", ExceedEnum.IMANITY),
	SEIREN("719609078543941643", ExceedEnum.SEIREN),
	ELF("719609253144428554", ExceedEnum.ELF),
	WEREBEAST("719609156364927057", ExceedEnum.WEREBEAST),
	EXMACHINA("719609343300861972", ExceedEnum.EXMACHINA),
	FLUGEL("719609423311536145", ExceedEnum.FLUGEL);

	private final String id;
	private final ExceedEnum exceed;

	ExceedRole(String id, ExceedEnum exceed) {
		this.id = id;
		this.exceed = exceed;
	}

	public String getId() {
		return id;
	}

	public ExceedEnum getExceed() {
		return exceed;
	}

	public static ExceedRole getById(String id) {
		return Arrays.stream(ExceedRole.values()).filter(e -> e.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}

	public static ExceedRole getByExceed(ExceedEnum ex) {
		return Arrays.stream(ExceedRole.values()).filter(e -> e.getExceed().equals(ex)).findFirst().orElse(null);
	}
}
