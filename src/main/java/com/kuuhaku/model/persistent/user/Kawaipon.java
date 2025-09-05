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
import com.kuuhaku.interfaces.AutoMake;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.shiro.Card;
import com.ygimenez.json.JSONObject;
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
@Table(name = "kawaipon", schema = "kawaipon")
public class Kawaipon extends DAO<Kawaipon> implements AutoMake<Kawaipon> {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

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

	public Kawaipon(Account acc) {
		this.uid = acc.getUid();
	}

	@Override
	public Kawaipon make(JSONObject args) {
		this.uid = args.getString("uid");
		return this;
	}

	public String getUid() {
		return uid;
	}

	public Account getAccount() {
		return DAO.find(Account.class, uid);
	}

	public List<StashedCard> getStash() {
		return DAO.queryAll(StashedCard.class, "SELECT sc FROM StashedCard sc WHERE sc.kawaipon.uid = ?1", uid);
	}

	public List<MarketOrder> getOrders() {
		return DAO.queryAll(MarketOrder.class, "SELECT mo FROM MarketOrder mo WHERE mo.kawaipon.uid = ?1", uid);
	}

	public int getMaxCapacity() {
		Account acc = getAccount();
		int mult = 3 + acc.getItemCount("cap_boost") + acc.getItemCount("pumpkin_gummy");
		int add = acc.getItemCount("extra_cap") * 10;
		int rem = acc.getItemCount("leaver_penalty") * 10;

		return Math.max(0, 250 + add - rem + acc.getHighestLevel() * mult);
	}

	public int getStashUsage() {
		return DAO.queryNative(Integer.class, """
				SELECT (SELECT count(1) FROM stashed_card WHERE kawaipon_uid = ?1)
				     + (SELECT count(1) FROM market_order WHERE kawaipon_uid = ?1)
				""", uid);
	}

	public int getCapacity() {
		return getMaxCapacity() - getStashUsage();
	}

	public Set<StashedCard> getCollection() {
		return Set.copyOf(DAO.queryAll(StashedCard.class, """
				SELECT sc
				FROM StashedCard sc
				WHERE sc.inCollection
				  AND sc.kawaipon.uid = ?1
				""", uid));
	}

	public Set<StashedCard> getCollection(Anime a, boolean chrome) {
		return Set.copyOf(DAO.queryAll(StashedCard.class, """
				SELECT sc
				FROM StashedCard sc
				WHERE sc.inCollection
				  AND sc.kawaipon.uid = ?1
				  AND sc.card.anime.id = ?2
				  AND sc.chrome = ?3
				""", uid, a.getId(), chrome));
	}

	public StashedCard getCard(Card card, boolean chrome) {
		return DAO.query(StashedCard.class, """
				SELECT sc
				FROM StashedCard sc
				WHERE sc.inCollection
				  AND sc.kawaipon.uid = ?1
				  AND sc.card = ?2
				  AND sc.chrome = ?3
				""", uid, card, chrome);
	}

	public boolean hasCard(StashedCard card) {
		return getCard(card.getCard(), card.isChrome()) != null;
	}

	public boolean hasCard(Card card, boolean chrome) {
		return getCard(card, chrome) != null;
	}

	public Pair<Integer, Integer> countCards() {
		Object[] vals = DAO.queryUnmapped("""
				SELECT coalesce(sum(normal), 0)
				     , coalesce(sum(chrome), 0)
				FROM v_collection_counter
				WHERE uid = ?1
				""", uid);

		if (vals == null) {
			return new Pair<>(0, 0);
		}

		return new Pair<>(((Number) vals[0]).intValue(), ((Number) vals[1]).intValue());
	}

	public Pair<Integer, Integer> countCards(Anime anime) {
		Object[] vals = DAO.queryUnmapped("""
				SELECT coalesce(normal, 0)
				     , coalesce(chrome, 0)
				FROM v_collection_counter
				WHERE uid = ?1
				  AND anime_id = ?2
				""", uid, anime.getId());

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
				         SELECT sc.chrome
				         FROM stashed_card sc
				                  INNER JOIN card c ON c.id = sc.card_id
				         WHERE sc.kawaipon_uid = ?1
				           AND c.rarity = ?2
				           AND sc.in_collection
				         GROUP BY sc.card_id, sc.chrome
				     ) x
				""", uid, rarity.name());

		if (vals == null) {
			return new Pair<>(0, 0);
		}

		return new Pair<>(((Number) vals[0]).intValue(), ((Number) vals[1]).intValue());
	}

	public boolean isCollectionComplete(Anime anime) {
		Pair<Integer, Integer> count = countCards(anime);
		return Math.max(count.getFirst(), count.getSecond()) >= anime.getCount();
	}

	public List<StashedCard> getLocked() {
		return DAO.queryAll(StashedCard.class, """
				SELECT s
				FROM StashedCard s
				WHERE s.kawaipon.uid = ?1
				  AND s.locked = TRUE
				""", uid);
	}

	public List<StashedCard> getExtras() {
		List<Integer> ids = DAO.queryAllNative(Integer.class, """
				SELECT x.id
				FROM (
				         SELECT sc.id
				              , row_number() OVER (PARTITION BY sc.card_id ORDER BY sc.quality DESC) AS copy
				         FROM stashed_card sc
				         WHERE sc.kawaipon_uid = ?1
				           AND sc.deck_id IS NULL
				           AND sc.price = 0
				           AND NOT sc.locked
				           AND NOT sc.in_collection
				     ) x
				WHERE x.copy > 3
				""", uid);

		return DAO.queryAll(StashedCard.class, "SELECT sc FROM StashedCard sc WHERE sc.id IN ?1", ids);
	}

	public List<StashedCard> getNotInUse() {
		return DAO.queryAll(StashedCard.class, """
				SELECT s
				FROM StashedCard s
				WHERE s.kawaipon.uid = ?1
				  AND s.deck.id IS NULL
				  AND s.price = 0
				  AND s.locked = FALSE
				""", uid);
	}

	public List<StashedCard> getRemovable() {
		List<String> cards = DAO.queryAllNative(String.class, """
				SELECT x.uuid
				FROM (
					 SELECT sc.uuid
						  , row_number() OVER (PARTITION BY sc.card_id, sc.chrome ORDER BY sc.id) AS copy
					 FROM stashed_card sc
					 WHERE sc.kawaipon_uid = ?1
					 ) x
				WHERE x.copy = 1
				""", uid);

		return DAO.queryAll(StashedCard.class, "SELECT s FROM StashedCard s WHERE s.uuid IN ?1", cards);
	}

	public List<StashedCard> getTradeable() {
		return DAO.queryAll(StashedCard.class, """
				SELECT s
				FROM StashedCard s
				WHERE s.kawaipon.uid = ?1
				  AND s.deck.id IS NULL
				  AND s.price = 0
				  AND NOT s.locked
				  AND NOT s.accountBound
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
		return Objects.equals(uid, kawaipon.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(uid);
	}
}
