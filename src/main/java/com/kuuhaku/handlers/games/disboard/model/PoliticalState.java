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

package com.kuuhaku.handlers.games.disboard.model;

import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.enums.Country;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import org.json.JSONArray;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "politicalstate")
public class PoliticalState {
	@Id
	@Enumerated(EnumType.STRING)
	private ExceedEnums exceed;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT '[]'")
	private String countries = "[]";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int influence = 0;

	public PoliticalState(ExceedEnums exceed) {
		this.exceed = exceed;
		List<Set<Country>> countries = PStateDAO.getAllPoliticalState().stream().map(PoliticalState::getCountries).collect(Collectors.toList());
		Set<Country> available = EnumSet.allOf(Country.class);
		countries.forEach(available::removeAll);

		List<String> cs = new ArrayList<>();
		cs.add(new ArrayList<>(available).get(Helper.rng(available.size())).name());
		this.countries = cs.toString();
	}

	public PoliticalState() {
	}

	public ExceedEnums getExceed() {
		return exceed;
	}

	public Set<Country> getCountries() {
		Set<Country> cs = EnumSet.noneOf(Country.class);
		new JSONArray(countries).forEach(c -> Country.valueOf(String.valueOf(c)));
		return cs;
	}

	public void addCountry(Country country) {
		List<String> cs = new ArrayList<>();
		for (Country c : getCountries()) {
			cs.add(c.name());
		}

		cs.add(country.name());
		this.countries = cs.toString();
	}

	public void removeCountry(Country country) {
		List<String> cs = new ArrayList<>();
		for (Country c : getCountries()) {
			cs.add(c.name());
		}

		cs.remove(country.name());
		this.countries = cs.toString();
	}

	public int getInfluence() {
		return influence;
	}

	public int getLandValue() {
		return getCountries().parallelStream().mapToInt(Country::getSize).sum();
	}

	public void modifyInfluence(boolean won) {
		if (won)
			this.influence++;
		else
			this.influence--;
	}

	public void modifyInfluence(int inf) {
		this.influence += inf;
	}

	public boolean aschente(Country targetC, Country selfC, PoliticalState enemy) {
		boolean won = (int) Math.round(Math.random() * influence + enemy.getInfluence() * (int) Math.ceil(getCountries().size() / 2f)) > influence;

		if (won) {
			enemy.removeCountry(targetC);
			addCountry(targetC);
		} else {
			enemy.addCountry(selfC);
			removeCountry(selfC);
		}

		this.influence /= Math.max(influence - enemy.getInfluence(), 1);
		enemy.modifyInfluence(enemy.getInfluence() / Math.max(enemy.getInfluence() - influence / 2, 1));

		return won;
	}
}
