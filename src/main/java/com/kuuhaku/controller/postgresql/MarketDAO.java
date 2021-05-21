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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.model.persistent.Market;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class MarketDAO {
	@SuppressWarnings("unchecked")
	public static List<Market> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Market m WHERE buyer = ''", Market.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Market> getCardsByCard(String id, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT m
				FROM Market m
				WHERE m.card.id = UPPER(:id)
				AND m.foil = :foil
				AND m.publishDate IS NOT NULL
				AND m.buyer <> ''
				AND m.buyer <> m.seller
				""", Market.class);
		q.setParameter("id", id);
		q.setParameter("foil", foil);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Market> getCardsByRarity(KawaiponRarity r, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT m
				FROM Market m
				WHERE m.card.rarity = :rarity
				AND m.foil = :foil
				AND m.publishDate IS NOT NULL
				AND m.buyer <> ''
				AND m.buyer <> m.seller
				""", CardMarket.class);
		q.setParameter("rarity", r);
		q.setParameter("foil", foil);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static Market getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Market m = em.find(Market.class, id);
			if (m == null || !m.getBuyer().isBlank()) return null;
			else return m;
		} finally {
			em.close();
		}
	}

	public static void saveCard(Market offer) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(offer);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Market> getOffers(String name, int min, int max, KawaiponRarity rarity, String anime, boolean foil, boolean onlyKp, boolean onlyEq, boolean onlyFd, String seller) {
		EntityManager em = Manager.getEntityManager();

		String query = """
				SELECT m
				FROM Market m
				JOIN m.card c
				JOIN c.anime a
				WHERE m.buyer = ''
				%s
				""";

		String priceCheck = """
				AND m.price <= CASE c.rarity
					WHEN 'COMMON'     THEN 1
					WHEN 'UNCOMMON'   THEN 2
					WHEN 'RARE'       THEN 3
					WHEN 'ULTRA_RARE' THEN 4
					WHEN 'LEGENDARY'  THEN 5
					WHEN 'EQUIPMENT'  THEN 1
					WHEN 'FIELD'      THEN 1
				END *
				CASE c.rarity
					WHEN 'COMMON'     THEN (:cbase * CASE m.foil WHEN TRUE THEN 50 ELSE 25 END)
					WHEN 'UNCOMMON'   THEN (:cbase * CASE m.foil WHEN TRUE THEN 50 ELSE 25 END)
					WHEN 'RARE'       THEN (:cbase * CASE m.foil WHEN TRUE THEN 50 ELSE 25 END)
					WHEN 'ULTRA_RARE' THEN (:cbase * CASE m.foil WHEN TRUE THEN 50 ELSE 25 END)
					WHEN 'LEGENDARY'  THEN (:cbase * CASE m.foil WHEN TRUE THEN 50 ELSE 25 END)
					WHEN 'EQUIPMENT'  THEN (:ebase * 50)
					WHEN 'FIELD'      THEN :fbase
				END
				""";

		String[] params = {
				name != null ? "AND c.id LIKE UPPER(:name)" : "",
				min > -1 ? "AND m.price > :min" : "",
				max > -1 ? "AND m.price < :max" : "",
				rarity != null ? "AND c.rarity LIKE UPPER(:rarity)" : "",
				anime != null ? "AND a.id LIKE UPPER(:anime)" : "",
				foil ? "AND m.foil = :foil" : "",
				onlyKp ? "AND c.rarity <> 'EQUIPMENT' AND c.rarity <> 'FIELD'" : "",
				onlyEq ? "AND c.rarity = 'EQUIPMENT'" : "",
				onlyFd ? "AND c.rarity = 'FIELD'" : "",
				seller != null ? "AND m.seller = :seller" : "",
				seller == null ? priceCheck : "",
				"ORDER BY m.price, m.foil DESC, c.rarity DESC, a.id, c.id"
		};

		Query q = em.createQuery(query.formatted(String.join("\n", params)), Market.class);

		if (!params[0].isBlank()) q.setParameter("name", "%" + name + "%");
		if (!params[1].isBlank()) q.setParameter("min", min);
		if (!params[2].isBlank()) q.setParameter("max", max);
		if (!params[3].isBlank()) q.setParameter("rarity", rarity.name());
		if (!params[4].isBlank()) q.setParameter("anime", "%" + anime + "%");
		if (!params[5].isBlank()) q.setParameter("foil", foil);
		if (!params[6].isBlank()) q.setParameter("seller", seller);
		if (!params[7].isBlank()) {
			q.setParameter("cbase", Helper.BASE_CARD_PRICE);
			q.setParameter("ebase", Helper.BASE_EQUIPMENT_PRICE);
			q.setParameter("fbase", Helper.BASE_FIELD_PRICE);
		}

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}

