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

import java.util.Set;
import java.util.stream.Collectors;

public enum Perk {
	VANGUARD("Vanguarda", "Aumenta o HP em 33% e a defesa em 15%, mas reduz ataque em 25%."),
	BLOODLUST("Sede de sangue", "Converte metade do custo de MP em custo de HP (1 MP = 100 HP)."),
	CARELESS("Descuidado", "Aumenta ataque em 25% mas reduz defesa em 33%."),
	NIMBLE("Ágil", "Aumenta esquiva em 25% e reduz o tempo de expedição em 10%, mas reduz o HP máximo em 25%."),
	MANALESS("Sem mana", "Remove custo de MP, mas reduz todos os atributos em 50%."),
	MASOCHIST("Masoquista", "Aumenta o ataque e reduz a defesa com base no dano acumulado (2% HP perdido = +1% ATK/-1% DEF)."),
	NIGHTCAT("Gato Noturno", "Dobra a chance de esquiva em campos noturnos, mas divide pela metade em campos diurnos."),
	VAMPIRE("Vampiro", "Derrotar cartas restaura 10% do HP perdido, mas reduz o HP máximo em 25%."),
	ARMORED("Encouraçado", "O atributo AGI passa a conceder chance de bloqueio ao invés de esquiva."),
	MINDSHIELD("Escudo Psíquico", "Ganha permanentemente um escudo de feitiços, mas dobra o custo de mana e reduz a eficiência de WIS pela metade."),
	OPTIMISTIC("Otimista", "Recebe 25% mais XP ao falhar a missão."),
	PESSIMISTIC("Pessimista", "Reduz aPossui 25% menos chance de ser penalizado ao falhar a missão."),
	REAPER("Ceifador", "Ceifa cartas destruídas, selando-as, mas efeitos positivos de campo não te afetam mais."),
	ADAPTIVE("Adaptivo", "Enquanto em modo de ataque toda chance de bloqueio torna-se esquiva (2% esquiva = 1% bloqueio), ocorrendo o inverso em modo de defesa."),
	SCHOLAR("Erudito", "Não pode ter equipamentos, mas recebe +2 slots de perk extras.")
	;

	private final String name;
	private final String description;

	Perk(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		Set<Perk> incom = getIncompatibility();

		if (!incom.isEmpty())
			return description + "\n**Incompatível com: " + incom.stream().map(Perk::toString).collect(Collectors.joining(", ")) + "**";

		if (this == SCHOLAR)
			return description + "\n**Irá destruir todos os equipamentos atualmente equipados**";

		return description;
	}

	public Set<Perk> getIncompatibility() {
		return switch (this) {
			case BLOODLUST, MINDSHIELD -> Set.of(MANALESS);
			case MANALESS -> Set.of(BLOODLUST, MINDSHIELD);
			case NIGHTCAT -> Set.of(ARMORED);
			case ARMORED -> Set.of(NIGHTCAT);
			case OPTIMISTIC -> Set.of(PESSIMISTIC);
			case PESSIMISTIC -> Set.of(OPTIMISTIC);
			default -> Set.of();
		};
	}

	@Override
	public String toString() {
		return name;
	}
}
