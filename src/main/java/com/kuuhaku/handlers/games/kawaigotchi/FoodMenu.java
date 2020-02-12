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

import java.util.HashMap;
import java.util.Map;

import static com.kuuhaku.handlers.games.kawaigotchi.enums.FoodType.*;

public class FoodMenu {
	private static Map<String, Food> menu = new HashMap<String, Food>() {{
		//RATION
		put("racao", new Food(RATION, "Ração", "racao", -5, 3, 1, 15, null, null));
		put("poritos", new Food(RATION, "Poritos", "poritos", 5, 5, 5, 50, null, null));

		//MEAT
		put("almondega", new Food(MEAT, "Almondega", "almondega", 7, 10, 2, 35, null, null));
		put("parmigiana", new Food(MEAT, "Parmigiana", "parmigiana", 15, 12, 4, 80, null, null));

		//PLANT
		put("aspargo", new Food(PLANT, "Aspargo", "aspargo", -2, 5, 6, 20, null, null));
		put("tomate", new Food(PLANT, "Tomate", "tomate", 2, 8, 12, 45, null, null));

		//SWEET
		put("marshmallow", new Food(SWEET, "Marshmallow", "marshmallow", 10, 2, -2, 30, null, null));
		put("caramelo", new Food(SWEET, "Caramelo", "caramelo", 15, 3, -5, 60, null, null));

		//SPECIAL
		put("energetico", new Food(SPECIAL, "Energético", "energetico", 0, 0, -25, 125, k -> k.setEnergy(100f), "Recupera toda a energia do Kawaigotchi, mas faz mal à saúde."));
		put("resserum", new Food(SPECIAL, "Serum da Ressureição", "resserum", 0, 0, 0, 3000, Kawaigotchi::resurrect, "Ressuscita o Kawaigotchi, ao custo de metade da experiência atual."));
	}};

	public static Food getFood(String name) {
		return menu.getOrDefault(name, null);
	}

	public static Map<String, Food> getMenu() {
		return menu;
	}
}
