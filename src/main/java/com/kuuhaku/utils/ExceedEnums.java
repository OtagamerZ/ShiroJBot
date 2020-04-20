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

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ExceedEnums {
	IMANITY("Imanity"), SEIREN("Seiren"), WEREBEAST("Werebeast"), ELF("Elf"), EXMACHINA("Ex-Machina"), FLUGEL("Flügel");

	private final String name;

	ExceedEnums(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public static ExceedEnums getByName(String name) {
		return Arrays.stream(ExceedEnums.values()).filter(e -> e.getName().equalsIgnoreCase(name)).collect(Collectors.toList()).get(0);
	}
}
