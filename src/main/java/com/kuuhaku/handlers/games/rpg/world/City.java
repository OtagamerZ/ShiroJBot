/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.games.rpg.world;

import java.util.ArrayList;
import java.util.List;

public class City {
	private final String name;
	private final char[] pos;

	public City(String name, char[] pos) {
		this.name = name;
		this.pos = pos;
	}

	public String getName() {
		return name;
	}

	public char[] getPos() {
		return pos;
	}

	public static List<City> schwiaCities() {
		return new ArrayList<City>(){{
			add(new City("Okhon", new char[]{'X', 'f'}));
			add(new City("Ikhansaik", new char[]{'X', 'g'}));
			add(new City("Duukhovd", new char[]{'X', 'j'}));
			add(new City("Ergonuurga", new char[]{'V', 'j'}));
			add(new City("Khahbal", new char[]{'V', 'i'}));
			add(new City("Khakh", new char[]{'V', 'i'}));
			add(new City("Grafranosi", new char[]{'V', 'e'}));
			add(new City("Vente", new char[]{'V', 'c'}));
			add(new City("Morootsalant", new char[]{'T', 'g'}));
			add(new City("Seza Fano", new char[]{'S', 'c'}));
			add(new City("Onmochu", new char[]{'R', 'j'}));
			add(new City("Petro", new char[]{'R', 'g'}));
			add(new City("Santegione", new char[]{'R', 'f'}));
			add(new City("Aro Toroot", new char[]{'Q', 'i'}));
			add(new City("Lauvograv", new char[]{'O', 'h'}));
			add(new City("Stogadur", new char[]{'N', 'c'}));
			add(new City("Laustavik", new char[]{'L', 'd'}));
			add(new City("Stoknes", new char[]{'L', 'f'}));
			add(new City("Bifjorvi", new char[]{'L', 'h'}));
			add(new City("Neloyri", new char[]{'K', 'h'}));
			add(new City("Holheim", new char[]{'K', 'f'}));
			add(new City("Salfostad", new char[]{'J', 'a'}));
			add(new City("Veroykir", new char[]{'J', 'e'}));
			add(new City("Blistad", new char[]{'J', 'f'}));
			add(new City("Bjorfug", new char[]{'I', 'e'}));
			add(new City("Olfos", new char[]{'H', 'd'}));
			add(new City("Koyavik", new char[]{'H', 'g'}));
			add(new City("Sadmyra", new char[]{'G', 'f'}));
			add(new City("Lenduosta", new char[]{'E', 'i'}));
			add(new City("Vastad", new char[]{'C', 'f'}));
			add(new City("Rifjor", new char[]{'A', 'i'}));
		}};
	}
}
