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

import com.kuuhaku.controller.postgresql.CardDAO;

public enum AnimeName {
	AKAME_GA_KILL,
	ANGEL_BEATS,
	BLEACH,
	BOKU_NO_PICO,
	DANGANRONPA,
	DARLING_IN_THE_FRANXX,
	DEATH_NOTE,
	DR_STONE,
	ELFEN_LIED,
	GATE,
	HELLSING,
	HIGHSCHOOL_DXD,
	ISHUZOKU_REVIEWERS,
	ITADAKI_SEIEKI,
	JOJO_BIZARRE_ADVENTURES,
	KILL_LA_KILL,
	KIMETSU_NO_YAIBA,
	MIRAI_NIKKI,
	MOB_PSYCHO_100,
	MONSTER_MUSUME,
	NANATSU_NO_TAIZAI,
	NARUTO,
	NICHIJOU,
	NO_GAME_NO_LIFE,
	OVERLORD,
	PANTY_AND_STOCKING,
	PROJECT_VOCALOID,
	SAINT_SEIYA,
	SHINRYAKU_IKA_MUSUME,
	STEINS_GATE,
	SWORD_ART_ONLINE,
	TATE_NO_YUUSHA,
	BOKU_NO_HERO_ACADEMIA,
	BRAND_NEW_ANIMAL,
	FATE_APOCRYPHA;

	@Override
	public String toString() {
		return CardDAO.getUltimate(this).getName();
	}
}
