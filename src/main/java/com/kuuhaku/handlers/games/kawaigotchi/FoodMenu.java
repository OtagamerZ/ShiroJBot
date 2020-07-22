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

import com.kuuhaku.handlers.games.kawaigotchi.enums.Nature;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Stance;
import com.kuuhaku.utils.Helper;

import java.util.HashMap;
import java.util.Map;

import static com.kuuhaku.handlers.games.kawaigotchi.enums.FoodType.*;
import static com.kuuhaku.handlers.games.kawaigotchi.enums.Status.*;

public class FoodMenu {
	private static Map<String, Food> menu = new HashMap<>() {{
		//RATION
		put("racao", new Food(RATION, "Ração", "racao", -5, 3, 1, 15));
		put("poritos", new Food(RATION, "Poritos", "poritos", 5, 5, 5, 50));
		put("kawaiskas", new Food(RATION, "Kawaiskas Sachê", "kawaiskas", 10, 10, 10, 100));

		//MEAT
		put("almondega", new Food(MEAT, "Almondega", "almondega", 7, 10, 2, 35));
		put("parmigiana", new Food(MEAT, "Parmigiana", "parmigiana", 15, 12, 4, 80));
		put("presunto", new Food(MEAT, "Presunto", "presunto", 10, 8, 4, 70));

		//PLANT
		put("aspargo", new Food(PLANT, "Aspargo", "aspargo", -2, 5, 6, 20));
		put("tomate", new Food(PLANT, "Tomate", "tomate", 2, 8, 12, 45));
		put("baunilha", new Food(PLANT, "Baunilha", "baunilha", 4, 2, 8, 35));

		//SWEET
		put("marshmallow", new Food(SWEET, "Marshmallow", "marshmallow", 10, 2, -2, 30));
		put("caramelo", new Food(SWEET, "Caramelo", "caramelo", 15, 3, -5, 60));
		put("mel", new Food(SWEET, "Mel", "mel", 12, 2, 0, 80));

		//SPECIAL
		put("energetico", new Food(SPECIAL, "Energético", "energetico", 0, 0, -25, 250, k -> k.setEnergy(100f), "Recupera toda a energia do Kawaigotchi, mas faz mal à saúde.", "está a todo vapor!", ENERGY_UP.getIcon().getImage()));
		put("resserum", new Food(SPECIAL, "Serum da Ressureição", "resserum", 0, 0, 0, 5000, Kawaigotchi::resurrect, "Ressuscita o Kawaigotchi, ao custo de metade da experiência atual.", "foi ressuscitado!", RESSURRECTED.getIcon().getImage()));
		put("sonifero", new Food(SPECIAL, "Sonifero", "sonifero", 0, 0, 0, 150, k -> k.setStance(Stance.SLEEPING), "Coloca o Kawaigotchi para dormir, ele irá acordar assim que sua energia chegar a 100% novamente.", "foi sedado!", SLEEPING.getIcon().getImage()));
		put("vacina", new Food(SPECIAL, "Vacina", "vacina", -25, 0, 50, 500, Kawaigotchi::doNothing, "Vacina seu Kawaigotchi, recuperando 50% de saúde, porém ele não ficará muito feliz com uma agulhada.", "foi vacinado!", HEALTH_UP.getIcon().getImage()));
		put("faixademobius", new Food(SPECIAL, "Faixa de Möbius", "faixademobius", Helper.rng(150, false) - 75, Helper.rng(150, false) - 75, Helper.rng(150, false) - 75, 10000, k -> {
			k.setSkin(1 + Helper.rng(5, false));
			k.setNature(Nature.randNature());
		}, "Troca seu Kawaigotchi com uma versão dele de outra dimensão.", "viajou ao multiverso!", MOBIUS.getIcon().getImage()));
	}};

	public static Food getFood(String name) {
		return menu.getOrDefault(name.toLowerCase(), null);
	}

	public static Map<String, Food> getMenu() {
		return menu;
	}
}
