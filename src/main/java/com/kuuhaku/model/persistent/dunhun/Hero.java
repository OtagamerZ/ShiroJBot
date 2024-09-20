/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.dunhun.HeroModifiers;
import com.kuuhaku.model.persistent.user.Account;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "hero", schema = "dunhun")
public class Hero extends DAO<Hero> {
	@Id
	@Column(name = "name", nullable = false)
	private String name;

	@Embedded
	private HeroStats stats;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@Transient
	private final HeroModifiers modifiers = new HeroModifiers();

	public Hero() {
	}

	public Hero(Account account, String name) {
		this.name = name.toUpperCase();
		this.account = account;
	}

	public String getName() {
		return name;
	}

	public HeroStats getStats() {
		return stats;
	}

	public HeroModifiers getModifiers() {
		return modifiers;
	}

	public Account getAccount() {
		return account;
	}

//	public Senshi asSenshi() {
//		Senshi out = new Senshi(
//				name.toUpperCase(),
//				new Card(name),
//		)
//	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hero hero = (Hero) o;
		return Objects.equals(name, hero.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
}
