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

package com.kuuhaku.handlers.games.kawaigotchi;

import com.kuuhaku.handlers.games.kawaigotchi.enums.FoodType;

import java.awt.*;
import java.util.function.Consumer;

public class Food {
	private final FoodType type;
	private final String name;
	private final String identifier;
	private final int moodBoost;
	private final int nutrition;
	private final int healthiness;
	private final int price;
	private final Consumer<Kawaigotchi> special;
	private final String specialDesc;
	private final String specialQuote;
	private final Image specialIcon;

	public Food(FoodType type, String name, String identifier, int moodBoost, int nutrition, int healthiness, int price, Consumer<Kawaigotchi> special, String specialDesc, String specialQuote, Image specialIcon) {
		this.type = type;
		this.name = name;
		this.identifier = identifier;
		this.moodBoost = moodBoost;
		this.nutrition = nutrition;
		this.healthiness = healthiness;
		this.price = price;
		this.special = special;
		this.specialDesc = specialDesc;
		this.specialQuote = specialQuote;
		this.specialIcon = specialIcon;
	}

	public Food(FoodType type, String name, String identifier, int moodBoost, int nutrition, int healthiness, int price) {
		this.type = type;
		this.name = name;
		this.identifier = identifier;
		this.moodBoost = moodBoost;
		this.nutrition = nutrition;
		this.healthiness = healthiness;
		this.price = price;
		this.special = null;
		this.specialDesc = null;
		this.specialQuote = null;
		this.specialIcon = null;
	}

	public FoodType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}

	public int getMoodBoost() {
		return moodBoost;
	}

	public int getNutrition() {
		return nutrition;
	}

	public int getHealthiness() {
		return healthiness;
	}

	public int getPrice() {
		return price;
	}

	public Consumer<Kawaigotchi> getSpecial() {
		return special;
	}

	public String getSpecialDesc() {
		return specialDesc;
	}

	public String getSpecialQuote() {
		return specialQuote;
	}

	public Image getSpecialIcon() {
		return specialIcon;
	}
}
