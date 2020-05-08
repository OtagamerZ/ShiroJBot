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

public enum VanityType {
	HOUSE("https://i.imgur.com/8EH9Pyu.png", "\uD83C\uDFE1"),
	FENCE("https://i.imgur.com/G1Fj9Zo.png", "\uD83E\uDDF1"),
	BOWL("https://i.imgur.com/VsrI9rW.png", "\uD83E\uDD63");

	private final String icon;
	private final String button;

	VanityType(String icon, String button) {
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
		switch (this) {
			case HOUSE:
				return "Casa";
			case FENCE:
				return "Cerca";
			case BOWL:
				return "Tigela";
			default:
				throw new RuntimeException();
		}
	}

	public String toStrings() {
		switch (this) {
			case HOUSE:
				return "Casas";
			case FENCE:
				return "Cercas";
			case BOWL:
				return "Tigelas";
			default:
				throw new RuntimeException();
		}
	}

	public String getBonus() {
		switch (this) {
			case HOUSE:
				return "Conforto";
			case FENCE:
				return "Segurança";
			case BOWL:
				return "Satisfação";
			default:
				throw new RuntimeException();
		}
	}
}
