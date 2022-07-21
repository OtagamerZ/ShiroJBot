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
import com.kuuhaku.model.persistent.shiro.Card;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "kawaipon")
public class Kawaipon extends DAO<Kawaipon> {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@OneToOne(mappedBy = "kawaipon", orphanRemoval = true)
	private Account account;

	@OneToMany(mappedBy = "kawaipon", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<KawaiponCard> cards = new LinkedHashSet<>();

	@OneToMany(mappedBy = "kawaipon", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<StashedCard> stash = new LinkedHashSet<>();

	@Column(name = "stash_capacity", nullable = false)
	private int stashCapacity = 250;

	public Kawaipon() {
	}

	public Kawaipon(Account account) {
		this.uid = account.getUid();
		this.account = account;
	}

	@WhenNull
	public Kawaipon(String uid) {
		this.uid = uid;
		this.account = DAO.find(Account.class, uid);
	}

	public String getUid() {
		return uid;
	}

	public Account getAccount() {
		return account;
	}

	public Set<KawaiponCard> getCards() {
		return cards;
	}

	public Set<StashedCard> getStash() {
		return stash;
	}

	public int getMaxCapacity() {
		return stashCapacity;
	}

	public int getCapacity() {
		return getMaxCapacity() - stash.size();
	}

	public Set<KawaiponCard> getCollection() {
		return cards.parallelStream()
				.filter(c -> c.getStashEntry() == null)
				.collect(Collectors.toSet());
	}

	public KawaiponCard getCard(Card card, boolean chrome) {
		return cards.parallelStream()
				.filter(c -> c.getStashEntry() == null)
				.filter(c -> c.getCard().equals(card) && c.isChrome() == chrome)
				.findFirst().orElse(null);
	}

	public boolean hasCard(Card card, boolean chrome) {
		return cards.parallelStream()
				.filter(c -> c.getStashEntry() == null)
				.anyMatch(c -> c.getCard().equals(card) && c.isChrome() == chrome);
	}
}
