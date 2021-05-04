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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
			ZonedDateTime last = ZonedDateTime.now(ZoneId.of("GMT-3")).minusMonths(1);

			Query q = em.createQuery("""
					SELECT cm.price * 1.0
					FROM CardMarket cm
					WHERE cm.card = :card
					AND cm.publishDate < :date
					AND cm.buyer <> ''
					AND cm.buyer <> cm.seller
					""");
			q.setParameter("card", c);
			q.setParameter("date", last);

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
						 , EXTRACT(MONTH FROM current_date) AS month
						 , COALESCE(AVG(ms.sold), 10) AS avg_sold
						 , COALESCE(AVG(ms.unique_buyers), 2) AS avg_unique_buyers
					FROM "GetMerchantStats" ms
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
					FROM "GetMerchantStats" ms
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

	@SuppressWarnings("unchecked")
	public static List<CardMarket> getCardsForMarket(String name, int min, int max, KawaiponRarity rarity, String anime, boolean foil, String seller) {
		EntityManager em = Manager.getEntityManager();

		String query = """
				SELECT cm
				FROM CardMarket cm
				JOIN cm.card c
				JOIN c.anime a
				WHERE cm.buyer = ''
				%s
				""";

		String priceCheck = """
				AND cm.price <= CASE c.rarity
					WHEN 'COMMON' THEN 1
					WHEN 'UNCOMMON' THEN 2
					WHEN 'RARE' THEN 3
					WHEN 'ULTRA_RARE' THEN 4
					WHEN 'LEGENDARY' THEN 5
				END * :base * CASE cm.foil WHEN TRUE THEN 100 ELSE 50 END
				""";

		String[] params = {
				name != null ? "AND c.id LIKE UPPER(:name)" : "",
				min > -1 ? "AND cm.price > :min" : "",
				max > -1 ? "AND cm.price < :max" : "",
				rarity != null ? "AND c.rarity LIKE UPPER(:rarity)" : "",
				anime != null ? "AND a.id LIKE UPPER(:anime)" : "",
				foil ? "AND cm.foil = :foil" : "",
				seller != null ? "AND cm.seller = :seller" : "",
				seller == null ? priceCheck : "",
				"ORDER BY cm.price, cm.foil DESC, c.rarity DESC, a.id, c.id"
		};

		Query q = em.createQuery(query.formatted(String.join("\n", params)), CardMarket.class);

		if (!params[0].isBlank()) q.setParameter("name", "%" + name + "%");
		if (!params[1].isBlank()) q.setParameter("min", min);
		if (!params[2].isBlank()) q.setParameter("max", max);
		if (!params[3].isBlank()) q.setParameter("rarity", rarity.name());
		if (!params[4].isBlank()) q.setParameter("anime", "%" + anime + "%");
		if (!params[5].isBlank()) q.setParameter("foil", foil);
		if (!params[6].isBlank()) q.setParameter("seller", seller);
		if (!params[7].isBlank()) q.setParameter("base", Helper.BASE_CARD_PRICE);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}

