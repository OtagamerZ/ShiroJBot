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
	RATION(-1, 2, 0),
	MEAT(0, 1.5f, 1),
	SWEET(1.5f, 1, -1.5f),
	PLANT(0, 1.5f, 1.5f);

	private final float moodModifier;
	private final float nutritionModifier;
	private final float healthModifier;

	FoodType(float moodModifier, float nutritionModifier, float healthModifier) {
		this.moodModifier = moodModifier;
		this.nutritionModifier = nutritionModifier;
		this.healthModifier = healthModifier;
	}

	public float getMoodModifier() {
		return moodModifier;
	}

	public float getNutritionModifier() {
		return nutritionModifier;
	}

	public float getHealthModifier() {
		return healthModifier;
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
				return null;
		}
	}
}
