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

package com.kuuhaku.handlers.games.kawaigotchi.enums;

import java.util.Random;

public enum Nature {
	BRAVE(1, 1, 2),
	KIND(2, 1, 1),
	ACTIVE(1, 2, 1),
	NORMAL(1, 1, 1),
	LAZY(1, 0.5f, 1),
	SLY(1, 1, 0.5f),
	ROUGH(0.5f, 1, 1);

	private final float kindness;
	private final float energy;
	private final float trainability;

	Nature(float kindness, float energy, float trainability) {
		this.kindness = kindness;
		this.energy = energy;
		this.trainability = trainability;
	}

	public float getKindness() {
		return kindness;
	}

	public float getEnergy() {
		return energy;
	}

	public float getTrainability() {
		return trainability;
	}

	public static Nature randNature() {
		return Nature.values()[new Random().nextInt(Nature.values().length)];
	}

	@Override
	public String toString() {
		return switch (this) {
			case BRAVE -> "Corajoso";
			case KIND -> "Gentil";
			case ACTIVE -> "Ativo";
			case NORMAL -> "Normal";
			case LAZY -> "Preguiçoso";
			case SLY -> "Fresco";
			case ROUGH -> "Áspero";
		};
	}
}
