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

import static com.kuuhaku.handlers.games.kawaigotchi.enums.VanityType.*;

public class VanityMenu {
	private static Map<String, Vanity> menu = new HashMap<>() {{
		//CERCA
		put("cercademadeira", new Vanity(FENCE, "Cerca de Madeira", "cercademadeira", 750, getAsset("kawaigotchi/decoration/fence_wood.png").getImage(), 1.05f));
		put("cercafloral", new Vanity(FENCE, "Cerca Floral", "cercafloral", 1500, getAsset("kawaigotchi/decoration/fence_floral.png").getImage(), 1.15f));
		put("parededepedra", new Vanity(FENCE, "Parede de Pedra", "parededepedra", 2250, getAsset("kawaigotchi/decoration/fence_stone.png").getImage(), 1.25f));
		put("cercademetal", new Vanity(FENCE, "Cerca de Metal", "cercademetal", 3500, getAsset("kawaigotchi/decoration/fence_metal.png").getImage(), 1.5f));

		//DECORAÇÃO
		put("arvore", new Vanity(HOUSE, "Árvore", "arvore", 750, getAsset("kawaigotchi/decoration/deco_tree.png").getImage(), 1.1f));
		put("tenda", new Vanity(HOUSE, "Tenda", "tenda", 2750, getAsset("kawaigotchi/decoration/deco_tent.png").getImage(), 1.25f));
		put("casinha", new Vanity(HOUSE, "Casinha", "casinha", 6000, getAsset("kawaigotchi/decoration/deco_house.png").getImage(), 1.50f));

		//MISC
		put("tigela", new Vanity(BOWL, "Tigela", "tigela", 1000, getAsset("kawaigotchi/decoration/misc_bowl.png").getImage(), 1.25f));
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
