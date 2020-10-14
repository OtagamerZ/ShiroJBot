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

package com.kuuhaku.handlers.games.kawaigotchi.enums;

public enum FoodType {
	RATION("https://i.imgur.com/H2qidsS.png", "\uD83E\uDD6B"),
	MEAT("https://i.imgur.com/3khMjRv.png", "\uD83C\uDF57"),
	SWEET("https://i.imgur.com/02K2786.png", "\uD83C\uDF6C"),
	PLANT("https://i.imgur.com/UYpmwF7.png", "\uD83E\uDD6C"),
	SPECIAL("https://i.imgur.com/WuQF6lM.png", "\uD83E\uDDEA");

	private final String icon;
	private final String button;

	FoodType(String icon, String button) {
		this.icon = icon;
		this.button = button;
	}

	public String getIcon() {
		return icon;
	}

	public String getButton() {
		return button;
	}

	@Override
	public String toString() {
		return switch (this) {
			case RATION -> "Ração";
			case MEAT -> "Carne";
			case SWEET -> "Doce";
			case PLANT -> "Vegetal";
			case SPECIAL -> "Especial";
		};
	}

	public String toStrings() {
		return switch (this) {
			case RATION -> "Rações";
			case MEAT -> "Carnes";
			case SWEET -> "Doces";
			case PLANT -> "Vegetais";
			case SPECIAL -> "Especiais";
		};
	}
}
