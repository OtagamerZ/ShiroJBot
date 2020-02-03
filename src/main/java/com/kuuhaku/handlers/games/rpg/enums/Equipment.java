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

package com.kuuhaku.handlers.games.rpg.enums;

public enum Equipment {
	HEAD("Elmo"), CHEST("Peitoral"), LEG("Calça"), FOOT("Botas"), ARM("Luvas"), NECK("Colar"), BAG("Bolsa"), RING("Anel"), WEAPON("Arma/Escudo"), MISC("Não equipável");

	private final String name;

	Equipment(String name) {
		this.name = name;
	}

	public static Equipment byName(String name) throws IllegalArgumentException {
		switch (name.toLowerCase()) {
			case "elmo":
				return Equipment.HEAD;
			case "peitoral":
				return Equipment.CHEST;
			case "calça":
				return Equipment.LEG;
			case "botas":
				return Equipment.FOOT;
			case "luvas":
				return Equipment.ARM;
			case "colar:":
				return Equipment.NECK;
			case "bolsa":
				return Equipment.BAG;
			case "anel":
				return Equipment.RING;
			case "arma":
			case "escudo":
				return Equipment.WEAPON;
			case "misc":
				return Equipment.MISC;
			default:
				throw new IllegalArgumentException();
		}
	}

	public String getName() {
		return name;
	}
}
