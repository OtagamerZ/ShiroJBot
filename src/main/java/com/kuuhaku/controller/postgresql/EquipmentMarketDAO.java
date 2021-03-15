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

import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.EquipmentMarket;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Calendar;
import java.util.List;

public class EquipmentMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<EquipmentMarket> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT eqm FROM EquipmentMarket eqm WHERE buyer = ''", EquipmentMarket.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<EquipmentMarket> getCardsByCard(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT eqm FROM EquipmentMarket eqm WHERE eqm.card.card.id = UPPER(:id) AND eqm.publishDate IS NOT NULL", EquipmentMarket.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static EquipmentMarket getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			EquipmentMarket eqm = em.find(EquipmentMarket.class, id);
			if (eqm == null || !eqm.getBuyer().isBlank()) return null;
			else return eqm;
		} finally {
			em.close();
		}
	}

	public static void saveCard(EquipmentMarket card) {
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
				SELECT em.price * 1.0
				FROM EquipmentMarket em
				WHERE em.card.card = :card
				AND em.buyer <> ''
				AND em.buyer <> em.seller
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
					SELECT em.price * 1.0
					FROM EquipmentMarket em
					WHERE em.card.card = :card
					AND em.publishDate < :date
					AND em.buyer <> ''
					AND em.buyer <> em.seller
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());

			double[] before = ArrayUtils.toPrimitive(((List<Double>) q.getResultList()).toArray(Double[]::new));


			q = em.createQuery("""
					SELECT em.price * 1.0
					FROM EquipmentMarket em
					WHERE em.card.card = :card
					AND em.buyer <> ''
					AND em.buyer <> em.seller
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

	@SuppressWarnings("unchecked")
	public static List<EquipmentMarket> getCardsForMarket(String name, int min, int max, String seller) {
		EntityManager em = Manager.getEntityManager();

		String query = """
				SELECT em 
				FROM EquipmentMarket em
				JOIN em.card e
				JOIN e.card c 
				WHERE em.buyer = ''
				%s
				""";

		String[] params = {
				name != null ? "AND c.id LIKE :name" : "",
				min > -1 ? "AND em.price > :min" : "",
				max > -1 ? "AND em.price < :max" : "",
				seller != null ? "AND em.seller = :seller" : "",
				"ORDER BY em.price, c.id"
		};

		Query q = em.createQuery(query.formatted(String.join("\n", params)), EquipmentMarket.class);

		if (!params[0].isBlank()) q.setParameter("name", "%" + name + "%");
		if (!params[1].isBlank()) q.setParameter("min", min);
		if (!params[2].isBlank()) q.setParameter("max", max);
		if (!params[3].isBlank()) q.setParameter("seller", seller);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}
