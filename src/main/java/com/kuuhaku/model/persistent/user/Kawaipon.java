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
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.shiro.Card;
import jakarta.persistence.*;
import kotlin.Pair;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
		return stashCapacity + account.getHighestLevel() * 3;
	}

	public int getStashUsage() {
		return DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM stashed_card WHERE kawaipon_uid = ?1", uid);
	}

	public int getCapacity() {
		return getMaxCapacity() - getStashUsage();
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

	public Pair<Integer, Integer> countCards(Anime anime) {
		AtomicInteger normal = new AtomicInteger();
		AtomicInteger chrome = new AtomicInteger();

		cards.parallelStream()
				.filter(c -> c.getStashEntry() == null)
				.filter(c -> c.getCard().getAnime().equals(anime))
				.forEach(c -> {
					if (c.isChrome()) {
						chrome.getAndIncrement();
					} else {
						normal.getAndIncrement();
					}
				});

		return new Pair<>(normal.get(), chrome.get());
	}

	public List<StashedCard> getNotInUse() {
		return DAO.queryAll(StashedCard.class, "SELECT s FROM StashedCard s WHERE s.kawaipon.uid = ?1 AND s.deck.id IS NULL AND s.price = 0", uid);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Kawaipon kawaipon = (Kawaipon) o;
		return Objects.equals(uid, kawaipon.uid) && Objects.equals(account, kawaipon.account);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, account);
	}
}
