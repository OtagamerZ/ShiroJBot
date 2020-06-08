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

package com.kuuhaku.utils;

import com.kuuhaku.Main;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public enum ExceedEnums {
	IMANITY("Imanity", Color.MAGENTA),
	SEIREN("Seiren", Color.CYAN),
	WEREBEAST("Werebeast", Color.YELLOW),
	ELF("Elf", Color.PINK),
	EXMACHINA("Ex-Machina", Color.GREEN),
	FLUGEL("Flügel", Color.ORANGE);

	private final String name;
	private final Color palette;

	ExceedEnums(String name, Color palette) {
		this.name = name;
		this.palette = palette;
	}

	public String getName() {
		return this.name;
	}

	public static ExceedEnums getByName(String name) {
		return Arrays.stream(ExceedEnums.values()).filter(e -> e.getName().equalsIgnoreCase(name)).collect(Collectors.toList()).get(0);
	}

	public Color getPalette() {
		try {
			return Helper.colorThief(Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(TagIcons.getExceedId(this))).getImageUrl());
		} catch (IOException | NullPointerException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return Helper.getRandomColor();
		}
	}
}
