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

package com.kuuhaku.utils;

import net.dv8tion.jda.api.entities.MessageEmbed;

public enum VipItem {
	CARD_ROLL(1, 1, new MessageEmbed.Field("1 - Rodar carta (1 gema)", "Troca a carta por outra aleatória que você não tenha", false)),
	CARD_FOIL(2, 5, new MessageEmbed.Field("2 - Melhoria de carta (5 gemas)", "Transforma uma carta em cromada", false)),
	ANIMATED_BACKGROUND(3, 10, new MessageEmbed.Field("3 - Fundo de perfil animado (10 gemas)", "Permite usar GIFs como fundo de perfil", false)),
	;

	private final int id;
	private final int gems;
	private final MessageEmbed.Field field;

	VipItem(int id, int gems, MessageEmbed.Field field) {
		this.id = id;
		this.gems = gems;
		this.field = field;
	}

	public int getId() {
		return id;
	}

	public int getGems() {
		return gems;
	}

	public MessageEmbed.Field getField() {
		return field;
	}

	public static VipItem getById(int id) {
		switch (id) {
			case 1:
				return CARD_ROLL;
			case 2:
				return CARD_FOIL;
			case 3:
				return ANIMATED_BACKGROUND;
			default:
				return null;
		}
	}
}
