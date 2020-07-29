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

import org.apache.commons.text.WordUtils;

public enum AnimeName {
	NO_GAME_NO_LIFE,
	AKAME_GA_KILL,
	BLEACH,
	BOKU_NO_PICO,
	HIGHSCHOOL_DXD,
	DEATH_NOTE,
	ELFEN_LIED,
	JOJO_BIZARRE_ADVENTURES,
	PROJECT_VOCALOID,
	ANGEL_BEATS,
	PANTY_AND_STOCKING,
	SAINT_SEIYA,
	SWORD_ART_ONLINE,
	KILL_LA_KILL,
	ITADAKI_SEIEKI,
	OVERLORD,
	NICHIJOU,
	DARLING_IN_THE_FRANXX,
	ISHUZOKU_REVIEWERS,
	HELLSING,
	NARUTO,
	MIRAI_NIKKI,
	DR_STONE,
	KIMETSU_NO_YAIBA,
	MONSTER_MUSUME,
	GATE,
	NANATSU_NO_TAIZAI,
	STEINS_GATE,
	MOB_PSYCHO_100;

	@Override
	public String toString() {
		return WordUtils.capitalize(name().toLowerCase().replace("_", " "));
	}
}
