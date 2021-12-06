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

import com.kuuhaku.utils.Helper;

import java.awt.image.BufferedImage;
import java.util.Locale;

public enum Charm {
	SHIELD("Escudo", "Bloqueia %s efeito%s de destruição ou conversão"),
	MIRROR("Reflexo", "Reflete efeitos de destruição ou conversão"),
	TIMEWARP("Salto temporal", "Ativa %s efeito%s por turno instantaneamente"),
	DOUBLETAP("Toque duplo", "Ativa novamente %s efeito%s de invocação"),
	CLONE("Clone", "Cria %s clone%s com 75% dos atributos"),
	LINK("Vínculo", "Bloqueia modificadores de campo"),
	SPELL("Magia", "Executa um efeito ao ativar"),
	ENCHANTMENT("Encantamento", "Prende-se à uma carta, adicionando um efeito extra à ela"),
	TRAP("Armadilha", "Prende-se à uma carta mas virada para baixo, adicionando um efeito de uso único à ela"),
	PIERCING("Penetração", "Causa dano direto ao atacar"),
	AGILITY("Agilidade", "Aumenta a chance de esquiva em %s%%"),
	DRAIN("Dreno", "Rouba %s de mana ao atacar"),
	BLEEDING("Sangramento", "Reduz curas em 50% e causa dano direto ao longo de 10 turnos ao atacar");

	private final String name;
	private final String description;

	Charm(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription(int tier) {
		return switch (this) {
			case SHIELD, TIMEWARP, DOUBLETAP -> description.formatted(Helper.getFibonacci(tier), tier == 1 ? "" : "s");
			case CLONE, DRAIN -> description.formatted(Helper.getFibonacci(tier));
			case AGILITY -> description.formatted(10 * Helper.getFibonacci(tier));
			default -> description;
		};
	}

	public BufferedImage getIcon() {
		if (this == SPELL) return null;
		return Helper.getResourceAsImage(this.getClass(), "shoukan/charm/" + name().toLowerCase(Locale.ROOT) + ".png");
	}
}
