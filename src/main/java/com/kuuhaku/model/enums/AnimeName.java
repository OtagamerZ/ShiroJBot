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

package com.kuuhaku.model.enums;

import com.kuuhaku.controller.postgresql.CardDAO;

import java.util.Arrays;

public enum AnimeName {
	HIDDEN,
	AKAME_GA_KILL,
	ANGEL_BEATS,
	BAYONETTA_BLOODY_FATE,
	BLEACH,
	BOKU_NO_HERO_ACADEMIA,
	BOKU_NO_PICO,
	BRAND_NEW_ANIMAL,
	DANGANRONPA,
	DANMACHI,
	DARLING_IN_THE_FRANXX,
	DEATH_NOTE,
	DR_STONE,
	ELFEN_LIED,
	FATE_APOCRYPHA,
	GATE,
	HELLSING,
	HIGHSCHOOL_DXD,
	ISHUZOKU_REVIEWERS,
	ITADAKI_SEIEKI,
	JOJO_NO_KIMYOU_NA_BOUKEN,
	KILL_LA_KILL,
	KIMETSU_NO_YAIBA,
	KOBAYASHI_SAN_CHI_NO_MAID_DRAGON,
	KONOSUBA,
	MIRAI_NIKKI,
	MOB_PSYCHO_100,
	MONSTER_MUSUME_NO_IRU_NICHIJOU,
	NANATSU_NO_TAIZAI,
	NARUTO,
	NICHIJOU,
	NO_GAME_NO_LIFE,
	OVERLORD,
	PANTY_AND_STOCKING,
	PROJECT_VOCALOID,
	SAINT_SEIYA,
	SHINCHOU_YUUSHA,
	SHINRYAKU_IKA_MUSUME,
	STEINS_GATE,
	SWORD_ART_ONLINE,
	TATE_NO_YUUSHA_NO_NARIAGARI,
	LUCKY_STAR,
	SHINGEKI_NO_KYOJIN,
	HUNTER_X_HUNTER,
	BLACK_ROCK_SHOOTER,
	FATE_STAY_NIGHT,
	OVERFLOW,
	GENSHIN_IMPACT,
	MAHOU_SHOUJO_LYRICAL_NANOHA,
	KYONYUU_FANTASY;

	public static AnimeName[] validValues() {
		return Arrays.stream(values()).filter(an -> an != HIDDEN).toArray(AnimeName[]::new);
	}

	@Override
	public String toString() {
		return CardDAO.getUltimate(this).getName();
	}
}
