/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.enums;

public enum DailyTask {
	CREDIT_TASK("Consiga %s créditos"),
	CARD_TASK("Colete %s cartas"),
	DROP_TASK("Abra %s drops"),
	WINS_TASK("Ganhe %s partidas de Shoukan"),
	OFFMETA_TASK("Ganhe %s partidas de Shoukan com um deck com divergência do meta acima de %s%%"),
	XP_TASK("Ganhe %s pontos de XP"),
	ANIME_TASK("Colete %s cartas do anime %s"),
	RACE_TASK("Use %s cartas da raça %s");

	private final String description;

	DailyTask(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return switch (this) {
			case CREDIT_TASK -> "Tio patinhas";
			case CARD_TASK -> "Viciado em cartas";
			case DROP_TASK -> "Baú do pirata";
			case WINS_TASK -> "Pro-player";
			case OFFMETA_TASK -> "Explorador";
			case XP_TASK -> "Alto-falante";
			case ANIME_TASK -> "Otaku";
			case RACE_TASK -> "Invocador";
		};
	}
}
