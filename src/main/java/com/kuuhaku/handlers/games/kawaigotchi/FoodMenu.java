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

import com.kuuhaku.handlers.games.kawaigotchi.enums.Stance;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.kuuhaku.handlers.games.kawaigotchi.enums.FoodType.*;

public class FoodMenu {
	private static Map<String, Food> menu = new HashMap<String, Food>() {{
		//RATION
		put("racao", new Food(RATION, "Ração", "racao", -5, 3, 1, 15));
		put("poritos", new Food(RATION, "Poritos", "poritos", 5, 5, 5, 50));

		//MEAT
		put("almondega", new Food(MEAT, "Almondega", "almondega", 7, 10, 2, 35));
		put("parmigiana", new Food(MEAT, "Parmigiana", "parmigiana", 15, 12, 4, 80));

		//PLANT
		put("aspargo", new Food(PLANT, "Aspargo", "aspargo", -2, 5, 6, 20));
		put("tomate", new Food(PLANT, "Tomate", "tomate", 2, 8, 12, 45));

		//SWEET
		put("marshmallow", new Food(SWEET, "Marshmallow", "marshmallow", 10, 2, -2, 30));
		put("caramelo", new Food(SWEET, "Caramelo", "caramelo", 15, 3, -5, 60));

		//SPECIAL
		put("energetico", new Food(SPECIAL, "Energético", "energetico", 0, 0, -25, 125, k -> k.setEnergy(100f), "Recupera toda a energia do Kawaigotchi, mas faz mal à saúde.", "está a todo vapor!", new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("energyUp.png"))).getImage()));
		put("resserum", new Food(SPECIAL, "Serum da Ressureição", "resserum", 0, 0, 0, 3000, Kawaigotchi::resurrect, "Ressuscita o Kawaigotchi, ao custo de metade da experiência atual.", "foi ressuscitado!", new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("ressurrected.png"))).getImage()));
		put("sonifero", new Food(SPECIAL, "Sonifero", "sonifero", 0, 0, 0, 50, k -> k.setStance(Stance.SLEEPING), "Coloca o Kawaigotchi para dormir, ele irá acordar assim que sua energia chegar a 100% novamente.", "foi sedado!", new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sleeping.png"))).getImage()));
	}};

	public static Food getFood(String name) {
		return menu.getOrDefault(name, null);
	}

	public static Map<String, Food> getMenu() {
		return menu;
	}
}
