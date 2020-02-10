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

package com.kuuhaku.handlers.games.kawaigotchi;

import com.kuuhaku.handlers.games.kawaigotchi.enums.FoodType;

public class Food {
	private final FoodType type;
	private final String name;
	private final int moodBoost;
	private final int nutrition;
	private final int tastiness;

	public Food(FoodType type, String name, int moodBoost, int nutrition, int tastiness) {
		this.type = type;
		this.name = name;
		this.moodBoost = moodBoost;
		this.nutrition = nutrition;
		this.tastiness = tastiness;
	}

	public FoodType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public int getMoodBoost() {
		return moodBoost;
	}

	public int getNutrition() {
		return nutrition;
	}

	public int getHealthiness() {
		return tastiness;
	}
}
