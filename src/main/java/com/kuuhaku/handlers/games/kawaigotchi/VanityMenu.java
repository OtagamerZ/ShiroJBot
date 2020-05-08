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

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.kuuhaku.handlers.games.kawaigotchi.enums.VanityType.FENCE;

public class VanityMenu {
	private static Map<String, Vanity> menu = new HashMap<>() {{
		put("cercademadeira", new Vanity(FENCE, "Cerca de Madeira", "cercademadeira", 500, getAsset("kawaigotchi/decoration/fence_wood.png").getImage()));
		put("cercafloral", new Vanity(FENCE, "Cerca Floral", "cercafloral", 500, getAsset("kawaigotchi/decoration/fence_floral.png").getImage()));
		put("parededepedra", new Vanity(FENCE, "Parede de Pedra", "parededepedra", 500, getAsset("kawaigotchi/decoration/fence_stone.png").getImage()));
		put("cercadeaco", new Vanity(FENCE, "Cerca de Aço", "cercadeaco", 500, getAsset("kawaigotchi/decoration/fence_iron.png").getImage()));

	}};

	public static Vanity getVanity(String name) {
		return menu.getOrDefault(name, null);
	}

	public static Map<String, Vanity> getMenu() {
		return menu;
	}

	private static ImageIcon getAsset(String path) {
		return new ImageIcon(Objects.requireNonNull(Vanity.class.getClassLoader().getResource(path)));
	}
}
