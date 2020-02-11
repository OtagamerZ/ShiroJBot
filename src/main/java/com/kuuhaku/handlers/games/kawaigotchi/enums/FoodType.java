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

package com.kuuhaku.handlers.games.kawaigotchi.enums;

public enum FoodType {
	RATION("https://discordapp.com/assets/2396da35337ea36298e29e868ea4407b.svg", "\uD83E\uDD6B"),
	MEAT("https://discordapp.com/assets/cb026612789e2a304448f7d84c6d1f61.svg", "\uD83C\uDF57"),
	SWEET("https://discordapp.com/assets/45c8f940aac4dab66e6e73181663a59e.svg", "\uD83C\uDF6C"),
	PLANT("https://discordapp.com/assets/816f010a6094fe98836610d2f080a5fb.svg", "\uD83E\uDD6C");

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
		switch (this) {
			case RATION:
				return "Ração";
			case MEAT:
				return "Carne";
			case SWEET:
				return "Doce";
			case PLANT:
				return "Vegetal";
			default:
				throw new RuntimeException();
		}
	}
}
