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

import com.kuuhaku.handlers.games.kawaigotchi.enums.VanityType;

import java.awt.*;

public class Vanity {
	private final VanityType type;
	private final String name;
	private final String identifier;
	private final int price;
	private final Image image;
	private final float modifier;

	public Vanity(VanityType type, String name, String identifier, int price, Image image, float modifier) {
		this.type = type;
		this.name = name;
		this.identifier = identifier;
		this.price = price;
		this.image = image;
		this.modifier = modifier;
	}

	public VanityType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}

	public int getPrice() {
		return price;
	}

	public Image getImage() {
		return image;
	}

	public float getModifier() {
		return modifier;
	}
}
