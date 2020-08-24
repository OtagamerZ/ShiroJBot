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

public enum AnimeName {
	AKAME_GA_KILL("Akame Ga Kill"),
	ANGEL_BEATS("Angel Beats"),
	BLEACH("Bleach"),
	BOKU_NO_PICO("Boku No Pico"),
	DANGANRONPA("Danganronpa"),
	DARLING_IN_THE_FRANXX("Darling In The Franxx"),
	DEATH_NOTE("Death Note"),
	DR_STONE("Dr. Stone"),
	ELFEN_LIED("Elfen Lied"),
	GATE("GATE"),
	HELLSING("Hellsing"),
	HIGHSCHOOL_DXD("Highschool DxD"),
	ISHUZOKU_REVIEWERS("Ishuzoku Reviewers"),
	ITADAKI_SEIEKI("Itadaki! Seieki"),
	JOJO_BIZARRE_ADVENTURES("JoJo Bizarre Adventures"),
	KILL_LA_KILL("Kill La Kill"),
	KIMETSU_NO_YAIBA("Kimetsu No Yaiba"),
	MIRAI_NIKKI("Mirai Nikki"),
	MOB_PSYCHO_100("Mob Psycho 100"),
	MONSTER_MUSUME("Monster Musume"),
	NANATSU_NO_TAIZAI("Nanatsu No Taizai"),
	NARUTO("Naruto"),
	NICHIJOU("Nichijou"),
	NO_GAME_NO_LIFE("No Game No Life"),
	OVERLORD("Overlord"),
	PANTY_AND_STOCKING("Panty & Stocking"),
	PROJECT_VOCALOID("Project Vocaloid"),
	SAINT_SEIYA("Saint Seiya"),
	SHINRYAKU_IKA_MUSUME("Shinryaku! Ika Musume"),
	STEINS_GATE("Steins;Gate"),
	SWORD_ART_ONLINE("Sword Art Online");

	private final String name;

	AnimeName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
