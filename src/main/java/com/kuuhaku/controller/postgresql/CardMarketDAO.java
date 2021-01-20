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
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

public class CardMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<CardMarket> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT cm FROM CardMarket cm WHERE buyer = ''", CardMarket.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CardMarket> getCardsByCard(String id, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT cm 
				FROM CardMarket cm 
				WHERE cm.card.card.id = UPPER(:id) 
				AND cm.card.foil = :foil 
				AND cm.publishDate IS NOT NULL
				AND cm.buyer <> ''
				AND cm.buyer <> cm.seller
				""", CardMarket.class);
		q.setParameter("id", id);
		q.setParameter("foil", foil);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CardMarket> getCardsByRarity(KawaiponRarity r, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT cm 
				FROM CardMarket cm 
				WHERE cm.card.card.rarity = :rarity 
				AND cm.card.foil = :foil 
				AND cm.publishDate IS NOT NULL
				AND cm.buyer <> ''
				AND cm.buyer <> cm.seller
				""", CardMarket.class);
		q.setParameter("rarity", r);
		q.setParameter("foil", foil);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static CardMarket getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			CardMarket cm = em.find(CardMarket.class, id);
			if (cm == null || !cm.getBuyer().isBlank()) return null;
			else return cm;
		} finally {
			em.close();
		}
	}

	public static void saveCard(CardMarket card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(card);
		em.getTransaction().commit();

		em.close();
	}

	public static double getAverageValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT AVG(cm.price)
				FROM CardMarket cm
				WHERE cm.card = :card
				AND cm.publishDate >= :date
				""");
		q.setParameter("card", c);
		q.setParameter("date", OffsetDateTime.now().minusMonths(1));

		try {
			return ((BigDecimal) q.getSingleResult()).doubleValue();
		} finally {
			em.close();
		}
	}

	public static double getStockValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT AVG(cm.price)
				FROM CardMarket cm
				WHERE cm.card = :card
				AND cm.publishDate >= :date
				""");
		q.setParameter("card", c);
		q.setParameter("date", OffsetDateTime.now().minusMonths(1));

		double before = ((BigDecimal) q.getSingleResult()).setScale(3, RoundingMode.HALF_EVEN).doubleValue();

		q = em.createQuery("""
				SELECT AVG(cm.price)
				FROM CardMarket cm
				WHERE cm.card = :card
				AND cm.publishDate < :date
				""");
		q.setParameter("card", c);
		q.setParameter("date", OffsetDateTime.now().minusMonths(1));

		double now = ((BigDecimal) q.getSingleResult()).setScale(3, RoundingMode.HALF_EVEN).doubleValue();

		try {
			return Helper.prcnt(now, before) - 1;
		} finally {
			em.close();
		}
	}
}
