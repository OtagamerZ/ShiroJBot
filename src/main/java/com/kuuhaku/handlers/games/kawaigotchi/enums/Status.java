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

import javax.swing.*;
import java.util.Objects;

public enum Status {
	ENERGY_UP(getAsset("kawaigotchi/status/energyUp.png")),
	FOOD_UP(getAsset("kawaigotchi/status/foodUp.png")),
	HEALTH_UP(getAsset("kawaigotchi/status/healthUp.png")),
	MOOD_UP(getAsset("kawaigotchi/status/moodUp.png")),
	XP_UP(getAsset("kawaigotchi/status/xpUp.png")),
	MOBIUS(getAsset("kawaigotchi/status/mobius.png")),
	RESSURRECTED(getAsset("kawaigotchi/status/ressurrected.png")),
	SLEEPING(getAsset("kawaigotchi/status/sleeping.png")),
	TROUBLED(getAsset("kawaigotchi/status/troubled.png"));

	private final ImageIcon icon;

	Status(ImageIcon icon) {
		this.icon = icon;
	}

	public ImageIcon getIcon() {
		return icon;
	}

	private static ImageIcon getAsset(String path) {
		return new ImageIcon(Objects.requireNonNull(Status.class.getClassLoader().getResource(path)));
	}
}
