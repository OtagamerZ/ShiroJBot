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

import com.kuuhaku.model.common.StockValue;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.StockMarket;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<StockMarket> getInvestments(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT sm FROM StockMarket sm WHERE sm.uid = :id", StockMarket.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static StockMarket getCardInvestment(String id, Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT sm FROM StockMarket sm WHERE sm.uid = :id AND sm.card = :card", StockMarket.class);
		q.setParameter("id", id);
		q.setParameter("card", c);

		try {
			return (StockMarket) q.getSingleResult();
		} catch (NoResultException e) {
			return new StockMarket(id, c, 0);
		} finally {
			em.close();
		}
	}

	public static void saveInvestment(StockMarket sm) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(sm);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeZeroInvestments() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("DELETE FROM StockMarket sm WHERE sm.investment = 0")
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, StockValue> getValues() {
		EntityManager em = Manager.getEntityManager();

		Query prev = em.createNativeQuery("""
				SELECT c.id
					 , c.name
				     , COALESCE(x.values, '')
				FROM Card c
				LEFT JOIN (
						SELECT x.card_id
				        	 , STRING_AGG(CAST(x.price AS VARCHAR(32)), ',') 				 AS values
				    	FROM (
				             SELECT c.id                                                     AS card_id
				                  , COALESCE(cm.price, em.price, fm.price)                   AS price
				                  , COALESCE(cm.publishdate, em.publishdate, fm.publishdate) AS publishdate
				                  , COALESCE(cm.buyer, em.buyer, fm.buyer)                   AS buyer
				                  , COALESCE(cm.seller, em.seller, fm.seller)                AS seller
				             FROM Card c
				        	 LEFT JOIN Equipment e ON e.card_id = c.id
				             LEFT JOIN Field f ON f.card_id = c.id
				        	 LEFT JOIN CardMarket cm ON cm.card_id = c.id
				             LEFT JOIN EquipmentMarket em ON em.card_id = e.id
				             LEFT JOIN FieldMarket fm ON fm.card_id = f.id
				        ) x
						WHERE x.publishDate BETWEEN :from AND :to
				        AND x.buyer <> ''
				        AND x.buyer <> x.seller
				    	GROUP BY x.card_id
				) x ON x.card_id = c.id
				""")
				.setParameter("from", ZonedDateTime.now(ZoneId.of("GMT-3")).minusMonths(2))
				.setParameter("to", ZonedDateTime.now(ZoneId.of("GMT-3")).minusMonths(1));

		Query curr = em.createNativeQuery("""
				SELECT c.id
					 , c.name
				     , COALESCE(x.values, '')
				FROM Card c
				LEFT JOIN (
						SELECT x.card_id
				        	 , STRING_AGG(CAST(x.price AS VARCHAR(32)), ',') 				 AS values
				    	FROM (
				             SELECT c.id                                                     AS card_id
				                  , COALESCE(cm.price, em.price, fm.price)                   AS price
				                  , COALESCE(cm.publishdate, em.publishdate, fm.publishdate) AS publishdate
				                  , COALESCE(cm.buyer, em.buyer, fm.buyer)                   AS buyer
				                  , COALESCE(cm.seller, em.seller, fm.seller)                AS seller
				             FROM Card c
				        	 LEFT JOIN Equipment e ON e.card_id = c.id
				             LEFT JOIN Field f ON f.card_id = c.id
				        	 LEFT JOIN CardMarket cm ON cm.card_id = c.id
				             LEFT JOIN EquipmentMarket em ON em.card_id = e.id
				             LEFT JOIN FieldMarket fm ON fm.card_id = f.id
				        ) x
						WHERE x.publishDate > :date
				        AND x.buyer <> ''
				        AND x.buyer <> x.seller
				    	GROUP BY x.card_id
				) x ON x.card_id = c.id
				""")
				.setParameter("date", ZonedDateTime.now(ZoneId.of("GMT-3")).minusMonths(1));

		Map<String, StockValue> out = new HashMap<>();
		List<Object[]> prevResults = (List<Object[]>) prev.getResultList();
		List<Object[]> currResults = (List<Object[]>) curr.getResultList();

		for (int i = 0; i < prevResults.size(); i++) {
			Object[] prevRes = prevResults.get(i);
			Object[] currRes = currResults.get(i);

			double[] prevValues = Arrays.stream(String.valueOf(prevRes[2]).split(",")).mapToDouble(Double::valueOf).toArray();
			double[] currValues = Arrays.stream(String.valueOf(currRes[2]).split(",")).mapToDouble(Double::valueOf).toArray();

			out.put(String.valueOf(prevRes[0]), new StockValue(
					String.valueOf(prevRes[0]),
					String.valueOf(prevRes[1]),
					prevValues, currValues
			));
		}

		return out;
	}
}
