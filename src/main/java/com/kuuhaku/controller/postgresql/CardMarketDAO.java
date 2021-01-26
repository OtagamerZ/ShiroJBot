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

import com.kuuhaku.model.common.MerchantStats;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
				WHERE cm.card.id = UPPER(:id) 
				AND cm.foil = :foil 
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
				WHERE cm.card.rarity = :rarity 
				AND cm.foil = :foil 
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

	@SuppressWarnings("unchecked")
	public static double getAverageValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT cm.price * 1.0
				FROM CardMarket cm
				WHERE cm.card = :card
				AND cm.buyer <> ''
				AND cm.buyer <> cm.seller
				""");
		q.setParameter("card", c);

		double[] values = ArrayUtils.toPrimitive(((List<Double>) q.getResultList()).toArray(Double[]::new));

		try {
			return new GeometricMean().evaluate(values);
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static double getStockValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -1);

			Query q = em.createQuery("""
					SELECT cm.price * 1.0
					FROM CardMarket cm
					WHERE cm.card = :card
					AND cm.publishDate < :date
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());

			double[] before = ArrayUtils.toPrimitive(((List<Double>) q.getResultList()).toArray(Double[]::new));


			q = em.createQuery("""
					SELECT cm.price * 1.0
					FROM CardMarket cm
					WHERE cm.card = :card
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					""");
			q.setParameter("card", c);

			double[] now = ArrayUtils.toPrimitive(((List<Double>) q.getResultList()).toArray(Double[]::new));

			GeometricMean gm = new GeometricMean();
			return Helper.prcnt(gm.evaluate(now), gm.evaluate(before)) - 1;
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	public static MerchantStats getAverageMerchantStats() {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery("""
					SELECT ''
						 , AVG(ms.month) AS avg_month
						 , AVG(ms.sold) AS avg_sold
						 , AVG(ms.unique_buyers) AS avg_unique_buyers 
					FROM \"GetMerchantStats\" ms 
					WHERE ms.month = EXTRACT(MONTH FROM current_date)
					""");

			return MerchantStats.of((Object[]) q.getSingleResult());
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static MerchantStats getMerchantStats(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery("""
					SELECT ms.seller
						 , ms.month
						 , ms.sold
						 , ms.unique_buyers 
					FROM \"GetMerchantStats\" ms 
					WHERE ms.seller = :id
					AND ms.month = EXTRACT(MONTH FROM current_date)
					""");
			q.setParameter("id", id);

			return MerchantStats.of((Object[]) q.getSingleResult());
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}
}

