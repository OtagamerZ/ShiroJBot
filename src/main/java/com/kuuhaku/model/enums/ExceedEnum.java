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

import com.kuuhaku.Main;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public enum ExceedEnum {
	IMANITY("Imanity"),
	SEIREN("Seiren"),
	WEREBEAST("Werebeast"),
	ELF("Elf"),
	EXMACHINA("Ex-Machina"),
	FLUGEL("Flügel");

	private final String name;

	ExceedEnum(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ExceedEnum getByName(String name) {
		return Arrays.stream(ExceedEnum.values())
				.filter(e -> Helper.equalsAny(name, StringUtils.stripAccents(e.name), e.name, e.name()))
				.findFirst()
				.orElse(null);
	}

	public Color getPalette() {
		try {
			return Helper.colorThief(Objects.requireNonNull(Main.getShiroShards().getEmoteById(TagIcons.getExceedId(this))).getImageUrl());
		} catch (IOException | NullPointerException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return Helper.getRandomColor();
		}
	}
}
