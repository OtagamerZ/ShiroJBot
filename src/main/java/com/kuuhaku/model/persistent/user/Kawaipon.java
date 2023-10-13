/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.shiro.Card;
import jakarta.persistence.*;
import kotlin.Pair;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "kawaipon")
public class Kawaipon extends DAO<Kawaipon> {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@OneToOne(mappedBy = "kawaipon")
	private Account account;

	@ManyToOne
	@JoinColumn(name = "fav_card")
	@Fetch(FetchMode.JOIN)
	private Card favCard;

	@Column(name = "fav_expiration")
	private ZonedDateTime favExpiration;

	@Column(name = "fav_stacks", nullable = false)
	private int favStacks = 0;

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

	public List<KawaiponCard> getCards() {
		return DAO.queryAll(KawaiponCard.class, "SELECT kc FROM KawaiponCard kc WHERE kc.kawaipon.uid = ?1", uid);
	}

	public List<StashedCard> getStash() {
		return DAO.queryAll(StashedCard.class, "SELECT sc FROM StashedCard sc WHERE sc.kawaipon.uid = ?1", uid);
	}

	public List<MarketOrder> getOrders() {
		return DAO.queryAll(MarketOrder.class, "SELECT mo FROM MarketOrder mo WHERE mo.kawaipon.uid = ?1", uid);
	}

	public int getMaxCapacity() {
		int mult = 3 + account.getItemCount("cap_boost");
		int add = account.getItemCount("extra_cap") * 10;

		return 250 + add + account.getHighestLevel() * mult;
	}

	public int getStashUsage() {
		return DAO.queryNative(Integer.class, """
				SELECT count((SELECT 1 FROM stashed_card WHERE kawaipon_uid = ?1))
				     + count((SELECT 1 FROM market_order WHERE kawaipon_uid = ?1))
				""", uid);
	}

	public int getCapacity() {
		return getMaxCapacity() - getStashUsage();
	}

	public Set<KawaiponCard> getCollection() {
		return Set.copyOf(DAO.queryAll(KawaiponCard.class, """
				SELECT kc
				FROM KawaiponCard kc
				LEFT JOIN StashedCard sc ON kc.uuid = sc.uuid
				WHERE sc.id IS NULL
				  AND kc.kawaipon.uid = ?1
				""", account.getUid()));
	}

	public KawaiponCard getCard(Card card, boolean chrome) {
		return DAO.query(KawaiponCard.class, """
				SELECT kc
				FROM KawaiponCard kc
				LEFT JOIN StashedCard sc ON kc.uuid = sc.uuid
				WHERE sc.id IS NULL
				  AND kc.kawaipon.uid = ?1
				  AND kc.card = ?2
				  AND kc.chrome = ?3
				""", account.getUid(), card, chrome);
	}

	public boolean hasCard(KawaiponCard card) {
		return getCard(card.getCard(), card.isChrome()) != null;
	}

	public boolean hasCard(Card card, boolean chrome) {
		return getCard(card, chrome) != null;
	}

	public Pair<Integer, Integer> countCards() {
		Object[] vals = DAO.queryUnmapped("""
				SELECT count(1) FILTER (WHERE NOT x.chrome)
				     , count(1) FILTER (WHERE x.chrome)
				FROM (
				         SELECT kc.chrome
				         FROM kawaipon_card kc
				                  LEFT JOIN stashed_card sc on kc.uuid = sc.uuid
				         WHERE kc.kawaipon_uid = ?1
				           AND sc.id IS NULL
				         GROUP BY kc.card_id, kc.chrome
				     ) x
				""", account.getUid());

		if (vals == null) {
			return new Pair<>(0, 0);
		}

		return new Pair<>(((Number) vals[0]).intValue(), ((Number) vals[1]).intValue());
	}

	public Pair<Integer, Integer> countCards(Anime anime) {
		Object[] vals = DAO.queryUnmapped("""
				SELECT count(1) FILTER (WHERE NOT x.chrome)
				     , count(1) FILTER (WHERE x.chrome)
								FROM (
				         SELECT kc.chrome
				         FROM kawaipon_card kc
				                  INNER JOIN card c on c.id = kc.card_id
				                  LEFT JOIN stashed_card sc on kc.uuid = sc.uuid
				         WHERE kc.kawaipon_uid = ?1
				           AND c.anime_id = ?2
				           AND sc.id IS NULL
				         GROUP BY kc.card_id, kc.chrome
				     ) x
				""", account.getUid(), anime.getId());

		if (vals == null) {
			return new Pair<>(0, 0);
		}

		return new Pair<>(((Number) vals[0]).intValue(), ((Number) vals[1]).intValue());
	}

	public Pair<Integer, Integer> countCards(Rarity rarity) {
		Object[] vals = DAO.queryUnmapped("""
				SELECT count(1) FILTER (WHERE NOT x.chrome)
				     , count(1) FILTER (WHERE x.chrome)
								FROM (
				         SELECT kc.chrome
				         FROM kawaipon_card kc
				                  INNER JOIN card c on c.id = kc.card_id
				                  LEFT JOIN stashed_card sc on kc.uuid = sc.uuid
				         WHERE kc.kawaipon_uid = ?1
				           AND c.rarity = ?2
				           AND sc.id IS NULL
				         GROUP BY kc.card_id, kc.chrome
				     ) x
				""", account.getUid(), rarity.name());

		if (vals == null) {
			return new Pair<>(0, 0);
		}

		return new Pair<>(((Number) vals[0]).intValue(), ((Number) vals[1]).intValue());
	}

	public List<StashedCard> getTrash() {
		return DAO.queryAll(StashedCard.class, """
				SELECT s
				FROM StashedCard s
				WHERE s.kawaipon.uid = ?1
				  AND s.trash = TRUE
				""", uid);
	}

	public List<StashedCard> getNotInUse() {
		return DAO.queryAll(StashedCard.class, """
				SELECT s
				FROM StashedCard s
				WHERE s.kawaipon.uid = ?1
				  AND s.deck.id IS NULL
				  AND s.price = 0
				  AND s.trash = FALSE
				""", uid);
	}

	public List<StashedCard> getTradeable() {
		return DAO.queryAll(StashedCard.class, """
				SELECT s
				FROM StashedCard s
				WHERE s.kawaipon.uid = ?1
				  AND s.deck.id IS NULL
				  AND s.price = 0
				  AND s.trash = FALSE
				  AND s.accountBound = FALSE
				""", uid);
	}

	public Card getFavCard() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		if (favExpiration == null || now.isAfter(favExpiration)) {
			favCard = null;
			favExpiration = null;
		}

		return favCard;
	}

	public String getFavCardId() {
		Card fav = getFavCard();
		if (fav != null) return fav.getId();

		return null;
	}

	public ZonedDateTime getFavExpiration() {
		return favExpiration;
	}

	public void setFavCard(Card card) {
		favCard = card;
		favExpiration = ZonedDateTime.now(ZoneId.of("GMT-3")).plusDays(3);
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
