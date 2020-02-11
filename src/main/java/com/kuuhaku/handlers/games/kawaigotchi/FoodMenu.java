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
		put("racao", new Food(RATION, "Ração", -5, 3, 1, 15));
		put("poritos", new Food(RATION, "Poritos", 5, 5, 5, 50));

		//MEAT
		put("almondega", new Food(MEAT, "Almondega", 7, 10, 2, 35));
		put("parmigiana", new Food(MEAT, "Parmigiana", 15, 12, 4, 80));

		//PLANT
		put("aspargo", new Food(PLANT, "Aspargo", -2, 5, 6, 20));
		put("tomate", new Food(PLANT, "Tomate", 2, 8, 12, 45));

		//SWEET
		put("marshmallow", new Food(SWEET, "Marshmallow", 10, 2, -2, 30));
		put("caramelo", new Food(SWEET, "Caramelo", 15, 3, -5, 60));
	}};

	public static Food getFood(String name) {
		return menu.getOrDefault(name, null);
	}
}
