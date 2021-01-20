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
import java.util.Calendar;
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

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);

		double average;
		try {
			average = (double) em.createQuery("""
					SELECT AVG(cm.price)
					FROM CardMarket cm
					WHERE cm.card.card = :card
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					""").setParameter("card", c)
					.getSingleResult();
		} catch (NullPointerException e) {
			average = 0;
		}

		Query q = em.createQuery("""
				SELECT AVG(cm.price)
				FROM CardMarket cm
				WHERE cm.card.card = :card
				AND cm.publishDate >= :date
				AND cm.buyer <> ''
				AND cm.buyer <> cm.seller
				AND cm.price / :average BETWEEN -0.5 AND 0.5 
				""");
		q.setParameter("card", c);
		q.setParameter("date", cal.getTime());
		q.setParameter("average", average);

		try {
			return (Double) q.getSingleResult();
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	public static double getStockValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		double average;
		try {
			average = (double) em.createQuery("""
					SELECT AVG(cm.price)
					FROM CardMarket cm
					WHERE cm.card.card = :card
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					""").setParameter("card", c)
					.getSingleResult();
		} catch (NullPointerException e) {
			average = 0;
		}

		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -1);

			Query q = em.createQuery("""
					SELECT AVG(cm.price)
					FROM CardMarket cm
					WHERE cm.card.card = :card
					AND cm.publishDate >= :date
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					AND cm.price / :average BETWEEN -0.5 AND 0.5 
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());
			q.setParameter("average", average);

			double before = Helper.round((Double) q.getSingleResult(), 3);

			q = em.createQuery("""
					SELECT AVG(cm.price)
					FROM CardMarket cm
					WHERE cm.card.card = :card
					AND cm.publishDate < :date
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					AND cm.price / :average BETWEEN -0.5 AND 0.5 
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());
			q.setParameter("average", average);

			double now = Helper.round((Double) q.getSingleResult(), 3);

			return Helper.prcnt(now, before) - 1;
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}
}
