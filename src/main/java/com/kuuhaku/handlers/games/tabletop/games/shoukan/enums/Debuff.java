/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import org.intellij.lang.annotations.Language;

public enum Debuff {
	NAUSEA("Nausea", "Será atordoado por 1 turno ao ser invocado",
	""
	),
	WEAK_BONES("Ossos frágeis", "Perca metade da sua defesa base",
	""
	),
	WEAK_GRIP("Empunhadura fraca", "Perca metade de seu ataque base",
	""
	),
	TIREDNESS("Cansaço", "Sua chance de esquiva é zero",
	""
	),
	POISONED_I("Envenenado I", "Sofra 1% do HP base como dano direto a cada turno",
	""
	),
	POISONED_II("Envenenado II", "Sofra 5% do HP base como dano direto a cada turno",
	""
	),
	POISONED_III("Envenenado III", "Sofra 10% do HP base como dano direto a cada turno",
	""
	),

	;

	private final String name;
	private final String description;
	private final String effect;

	Debuff(String name, String description, @Language("groovy") String effect) {
		this.name = name;
		this.description = description;
		this.effect = effect;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getEffect() {
		return effect;
	}

	@Override
	public String toString() {
		return name;
	}
}
