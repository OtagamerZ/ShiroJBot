/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.annotations.WhenNull;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "stash")
public class Stash extends DAO {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@OneToOne(mappedBy = "stash", orphanRemoval = true)
	private Account account;

	@OneToMany(mappedBy = "stash", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<StashedCard> cards = new LinkedHashSet<>();

	public Stash() {
	}

	public Stash(Account account) {
		this.uid = account.getUid();
		this.account = account;
	}

	@WhenNull
	public Stash(String uid) {
		this.uid = uid;
		this.account = DAO.find(Account.class, uid);
	}

	public String getUid() {
		return uid;
	}

	public Account getAccount() {
		return account;
	}

	public Set<StashedCard> getCards() {
		return cards;
	}

	public int getMaxCapacity() {
		return 250;
	}

	public int getCapacity() {
		return getMaxCapacity() - cards.size();
	}
}
