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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum Category {
	DUELIST("Duelista", "Cartas-duelista são especializadas em causar dano mas sem sacrificar a defesa. São melhor aproveitadas no modo ofensivo e possuem alta sinergia com equipamentos de ataque."),
	TANK("Tanque", "Cartas-tanque são muito difíceis de abater em ataques diretos, e oferecem uma alta sobrevivência quando viradas para baixo. Possuem alta sinergia com equipamentos defensivos."),
	SUPPORT("Suporte", "Cartas-suporte são usadas principalmente para oferecer sustentação ao jogador ou outras cartas. Por possuirem atributos baixos, é essencial que hajam equipamentos ou cartas defendendo-as."),
	NUKE("Nuker", "Cartas-nuker sacrificam a defesa e sobrevivência em troca de um ataque extremamente alto. Geralmente são cartas que duram no máximo 2 turnos, mas são muito valiosas quando usadas corretamente."),
	TRAP("Armadilha", "Cartas-armadilha são feitas para serem jogadas viradas para baixo. Seus efeitos tornam-as muito perigosas, e causam relutância no oponente de atacar quando houver uma em campo."),
	LEVELER("Nivelador", "Cartas-niveladora são cartas especializadas em mudar o fluxo da partida. São equivalentes às cartas-armadilha, mas devem ser usadas apenas quando necessário para causar maior efeito colateral."),
	SPECIALIST("Especialista", "Cartas-especialista possuem efeitos complexos ou situacionais, o que tornam-as incrivelmente difíceis de serem usadas. Mas não se engane, nas mãos certas uma carta especialista pode virar completamente a situação de uma partida.");

	private final String name;
	private final String description;

	Category(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public static Category getByName(String name) {
		return Arrays.stream(values()).filter(c -> Helper.equalsAny(name, StringUtils.stripAccents(c.name), c.name, c.name())).findFirst().orElse(null);
	}
}
